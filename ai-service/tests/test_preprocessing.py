import pandas as pd
import pytest

from preprocessing import (SymptomValidationError, deduplicate, display_name, encode_symptoms,
                           load_dataset, normalize_symptom, validate_symptoms)

VOCAB = ["cough", "high_fever", "headache", "fatigue"]


def test_normalize_symptom_variants():
    assert normalize_symptom(" High Fever ") == "high_fever"
    assert normalize_symptom("high-fever") == "high_fever"
    assert display_name("high_fever") == "High fever"


def test_validate_symptoms_ok_dedupes_and_orders_by_vocabulary():
    assert validate_symptoms(["Headache", "cough", "COUGH"], VOCAB) == ["cough", "headache"]


@pytest.mark.parametrize("bad", [None, "fever", {"a": 1}, [1, 2], ["cough"], []])
def test_validate_symptoms_rejects_bad_shapes(bad):
    with pytest.raises(SymptomValidationError):
        validate_symptoms(bad, VOCAB)


def test_validate_symptoms_rejects_unsupported():
    with pytest.raises(SymptomValidationError, match="unsupported"):
        validate_symptoms(["cough", "teleportitis"], VOCAB)


def test_validate_symptoms_max():
    with pytest.raises(SymptomValidationError, match="at most"):
        validate_symptoms(VOCAB, VOCAB, min_symptoms=1, max_symptoms=2)


def test_encode_symptoms():
    row = encode_symptoms(["cough", "fatigue"], VOCAB)
    assert row.tolist() == [[1, 0, 0, 1]]


def test_load_dataset_cleans_and_reports(tmp_path):
    csv = tmp_path / "d.csv"
    pd.DataFrame({
        "cough": [1, 1, 0], "spotting_ urination": [0, 0, 1], "fluid_overload": [0, 0, 0],
        "fluid_overload.1": [1, 1, 0], "prognosis": ["A ", "A ", "B"],
    }).to_csv(csv, index=False)
    df, rep = load_dataset(csv)
    assert "spotting_urination" in df.columns
    assert rep.duplicate_columns_dropped == ["fluid_overload"]
    assert df["fluid_overload"].sum() == 2          # the copy that carries data is kept
    assert set(df["prognosis"]) == {"A", "B"}       # whitespace stripped
    assert rep.exact_duplicate_rows == 1 and rep.unique_rows == 2
    assert len(deduplicate(df)) == 2


def test_load_dataset_rejects_non_binary(tmp_path):
    csv = tmp_path / "d.csv"
    pd.DataFrame({"cough": [0, 2], "prognosis": ["A", "B"]}).to_csv(csv, index=False)
    with pytest.raises(ValueError, match="Non-binary"):
        load_dataset(csv)
