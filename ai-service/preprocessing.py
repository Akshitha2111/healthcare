"""Data loading, validation, cleaning and symptom encoding.

This module is shared by training (train_model.py) and inference (predict.py)
so that both use the *same* symptom vocabulary and encoding.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
import pandas as pd

LABEL_COLUMN = "prognosis"

# Spelling variants found in symptom_Description.csv vs Training.csv
LABEL_ALIASES = {
    "Dimorphic hemmorhoids(piles)": "Dimorphic hemorrhoids(piles)",
}

_NON_WORD = re.compile(r"[^a-z0-9]+")


def normalize_symptom(raw: str) -> str:
    """'High Fever ' / 'high fever' / 'high-fever' -> 'high_fever'."""
    if not isinstance(raw, str):
        raise TypeError("symptom must be a string")
    return _NON_WORD.sub("_", raw.strip().lower()).strip("_")


def display_name(symptom: str) -> str:
    """'high_fever' -> 'High fever' (UI label only)."""
    return symptom.replace("_", " ").capitalize()


@dataclass
class DataQualityReport:
    raw_rows: int = 0
    raw_columns: int = 0
    missing_values: int = 0
    duplicate_columns_dropped: list[str] = field(default_factory=list)
    constant_zero_columns_dropped: list[str] = field(default_factory=list)
    renamed_columns: dict[str, str] = field(default_factory=dict)
    exact_duplicate_rows: int = 0
    unique_rows: int = 0
    conflicting_rows: int = 0  # identical symptoms, different label
    n_symptoms: int = 0
    n_classes: int = 0
    class_count_min: int = 0
    class_count_max: int = 0
    class_count_min_after_dedup: int = 0
    class_count_max_after_dedup: int = 0

    def to_dict(self) -> dict:
        return self.__dict__.copy()


def clean_column_name(name: str) -> str:
    return normalize_symptom(name)


def load_dataset(csv_path: str | Path) -> tuple[pd.DataFrame, DataQualityReport]:
    """Load the raw CSV, validate and clean it. Returns the *raw-row* frame
    (duplicates still present, so they can be analysed) and a quality report."""
    df = pd.read_csv(csv_path)
    rep = DataQualityReport(raw_rows=len(df), raw_columns=df.shape[1])

    if LABEL_COLUMN not in df.columns:
        raise ValueError(f"Label column '{LABEL_COLUMN}' not found")

    rep.missing_values = int(df.isna().sum().sum())
    if rep.missing_values:
        # Symptom indicators: a missing value means "not reported" -> 0.
        # A missing label cannot be repaired: drop those rows.
        df = df.dropna(subset=[LABEL_COLUMN]).copy()
        df = df.fillna(0)

    # Repeated header names: pandas renames a second 'fluid_overload' column to
    # 'fluid_overload.1'. Keep the copy that actually carries data, under the base
    # name. (Columns that merely HAVE identical values are different symptoms and are kept.)
    dup_cols: list[str] = []
    for col in list(df.columns):
        m = re.fullmatch(r"(.+)\.(\d+)", col)
        if m and m.group(1) in df.columns:
            base = m.group(1)
            keep, drop = (col, base) if df[col].sum() > df[base].sum() else (base, col)
            dup_cols.append(drop)
            if keep == col:  # the suffixed copy holds the data
                df[base] = df[col]
            df = df.drop(columns=[col])
    rep.duplicate_columns_dropped = dup_cols

    # Column names: fix stray spaces such as 'spotting_ urination'
    renames = {c: clean_column_name(c) for c in df.columns if c != LABEL_COLUMN}
    rep.renamed_columns = {k: v for k, v in renames.items() if k != v}
    df = df.rename(columns=renames)

    symptom_cols = [c for c in df.columns if c != LABEL_COLUMN]
    zero_cols = [c for c in symptom_cols if df[c].sum() == 0]
    rep.constant_zero_columns_dropped = zero_cols
    df = df.drop(columns=zero_cols)
    symptom_cols = [c for c in df.columns if c != LABEL_COLUMN]

    # Value validation: indicators must be binary
    values = np.unique(df[symptom_cols].to_numpy())
    if not set(values.tolist()) <= {0, 1}:
        raise ValueError(f"Non-binary symptom values found: {values[:10]}")
    df[symptom_cols] = df[symptom_cols].astype("int8")

    # Label cleanup (trailing whitespace such as 'Diabetes ')
    df[LABEL_COLUMN] = df[LABEL_COLUMN].astype(str).str.strip().replace(LABEL_ALIASES)

    # Duplicate / inconsistency analysis
    rep.exact_duplicate_rows = int(df.duplicated().sum())
    uniq = df.drop_duplicates()
    rep.unique_rows = len(uniq)
    rep.conflicting_rows = int(uniq.duplicated(subset=symptom_cols, keep=False).sum())
    counts = df[LABEL_COLUMN].value_counts()
    ucounts = uniq[LABEL_COLUMN].value_counts()
    rep.n_symptoms = len(symptom_cols)
    rep.n_classes = int(counts.shape[0])
    rep.class_count_min, rep.class_count_max = int(counts.min()), int(counts.max())
    rep.class_count_min_after_dedup = int(ucounts.min())
    rep.class_count_max_after_dedup = int(ucounts.max())
    return df.reset_index(drop=True), rep


def deduplicate(df: pd.DataFrame) -> pd.DataFrame:
    """Remove exact duplicate rows. MUST be done before any split, otherwise the
    same record lands in both train and test (data leakage)."""
    return df.drop_duplicates().reset_index(drop=True)


def split_xy(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.Series]:
    return df.drop(columns=[LABEL_COLUMN]), df[LABEL_COLUMN]


def encode_symptoms(symptoms: list[str], vocabulary: list[str]) -> np.ndarray:
    """Encode a list of (already validated) symptom names to a 1 x N binary row."""
    index = {s: i for i, s in enumerate(vocabulary)}
    row = np.zeros((1, len(vocabulary)), dtype=np.float32)
    for s in symptoms:
        row[0, index[s]] = 1.0
    return row


class SymptomValidationError(ValueError):
    """Raised for invalid user input; message is safe to show to API clients."""


def validate_symptoms(
    raw: object, vocabulary: list[str], min_symptoms: int = 2, max_symptoms: int = 20
) -> list[str]:
    """Validate and normalise user-supplied symptoms. Returns de-duplicated,
    normalised names in vocabulary order."""
    if raw is None:
        raise SymptomValidationError("'symptoms' is required")
    if not isinstance(raw, list):
        raise SymptomValidationError("'symptoms' must be a list of strings")
    if not all(isinstance(s, str) for s in raw):
        raise SymptomValidationError("every symptom must be a string")
    normalised = {normalize_symptom(s) for s in raw if s.strip()}
    if len(normalised) < min_symptoms:
        raise SymptomValidationError(f"select at least {min_symptoms} distinct symptoms")
    if len(normalised) > max_symptoms:
        raise SymptomValidationError(f"select at most {max_symptoms} symptoms")
    known = set(vocabulary)
    unsupported = sorted(normalised - known)
    if unsupported:
        shown = ", ".join(unsupported[:5])
        raise SymptomValidationError(f"unsupported symptom(s): {shown}")
    return [s for s in vocabulary if s in normalised]
