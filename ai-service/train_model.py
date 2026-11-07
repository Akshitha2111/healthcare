"""Reproducible training + evaluation pipeline (NOT used at inference time).

    python train_model.py                    # full run, writes model/ and reports/
    python train_model.py --skip-keras       # scikit-learn candidates only (faster)

Pipeline
1. Load + validate + clean the dataset, write a data-quality report.
2. Demonstrate the duplicate-leakage problem (raw split vs de-duplicated split).
3. De-duplicate, then split the UNIQUE records 60/20/20 (stratified).
4. Compare candidates by repeated stratified CV on train+val (macro-F1) and a
   hold-out validation split. Test data is NOT used for model selection.
5. Refit the selected model on train+val, evaluate once on the test split.
6. Save model, metadata, descriptions, feature sensitivity and reports.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import platform
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import sklearn
from sklearn.base import clone
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (accuracy_score, classification_report, confusion_matrix,
                             f1_score, precision_score, recall_score)
from sklearn.model_selection import RepeatedStratifiedKFold, train_test_split

from models import KerasMLP, build_candidates
from preprocessing import (LABEL_ALIASES, LABEL_COLUMN, deduplicate, load_dataset,
                           split_xy)

MODEL_VERSION = "1.0.0"
TIE_TOLERANCE = 0.01  # CV macro-F1 within 1 pt of the best => prefer the simpler model
HERE = Path(__file__).resolve().parent


def macro_metrics(y_true, y_pred) -> dict:
    return {
        "accuracy": float(accuracy_score(y_true, y_pred)),
        "precision_macro": float(precision_score(y_true, y_pred, average="macro", zero_division=0)),
        "recall_macro": float(recall_score(y_true, y_pred, average="macro", zero_division=0)),
        "f1_macro": float(f1_score(y_true, y_pred, average="macro", zero_division=0)),
        "f1_weighted": float(f1_score(y_true, y_pred, average="weighted", zero_division=0)),
    }


def top_k_accuracy(model, X, y, k: int = 3) -> float:
    proba = model.predict_proba(X)
    top = np.argsort(-proba, axis=1)[:, :k]
    classes = np.asarray(model.classes_)
    return float(np.mean([yt in classes[row] for yt, row in zip(y, top)]))


def wilson_interval(successes: int, n: int, z: float = 1.96) -> tuple[float, float]:
    if n == 0:
        return (0.0, 0.0)
    p = successes / n
    denom = 1 + z * z / n
    centre = (p + z * z / (2 * n)) / denom
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denom
    return (max(0.0, centre - half), min(1.0, centre + half))


def leakage_demo(df: pd.DataFrame, seed: int) -> dict:
    """Show how much a naive random split on the raw rows overstates accuracy."""
    X, y = split_xy(df)
    Xtr, Xte, ytr, yte = train_test_split(X, y, test_size=0.2, stratify=y, random_state=seed)
    train_keys = set(map(tuple, Xtr.to_numpy()))
    overlap = float(np.mean([tuple(r) in train_keys for r in Xte.to_numpy()]))
    rf = RandomForestClassifier(n_estimators=100, random_state=seed).fit(Xtr, ytr)
    return {
        "naive_split_test_rows_also_in_train": overlap,
        "naive_split_accuracy_(INFLATED)": float(rf.score(Xte, yte)),
        "note": "A raw-row split leaks duplicates into the test set; we de-duplicate first.",
    }


def robustness_check(model, X: np.ndarray, y: np.ndarray, seed: int, reps: int = 20) -> dict:
    """Users usually report FEWER symptoms than a full dataset record. Randomly drop a
    fraction of each test record's symptoms (keeping at least 2) and re-score."""
    rng = np.random.default_rng(seed)
    out = {}
    for frac in (0.0, 0.25, 0.5):
        Xs, ys = [], []
        for row, label in zip(X, y):
            on = np.flatnonzero(row)
            for _ in range(reps if frac else 1):
                keep = max(2, int(round(len(on) * (1 - frac))))
                chosen = rng.choice(on, size=min(keep, len(on)), replace=False)
                r = np.zeros_like(row); r[chosen] = 1
                Xs.append(r); ys.append(label)
        Xs = np.vstack(Xs); ys = np.array(ys)
        out[f"drop_{int(frac * 100)}pct"] = {
            "top1_accuracy": float(np.mean(model.predict(Xs) == ys)),
            "top3_accuracy": top_k_accuracy(model, Xs, ys, 3), "n_samples": int(len(ys))}
    return out


def occlusion_sensitivity(model, X: np.ndarray, y: np.ndarray) -> dict[int, float]:
    """For each symptom j: mean drop in the true-class score when j is switched
    off in the records where it is present. Model-agnostic, shows what the
    MODEL relies on - not medical causality."""
    classes = list(model.classes_)
    y_idx = np.array([classes.index(v) for v in y])
    base = model.predict_proba(X)[np.arange(len(X)), y_idx]
    out = {}
    for j in range(X.shape[1]):
        rows = np.where(X[:, j] == 1)[0]
        if len(rows) == 0:
            continue
        Xm = X[rows].copy()
        Xm[:, j] = 0
        new = model.predict_proba(Xm)[np.arange(len(rows)), y_idx[rows]]
        out[j] = float(np.mean(base[rows] - new))
    return out


def save_confusion_png(cm: np.ndarray, classes: list[str], path: Path) -> bool:
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        return False
    fig, ax = plt.subplots(figsize=(14, 12))
    im = ax.imshow(cm, cmap="Blues")
    ax.set_xticks(range(len(classes)), classes, rotation=90, fontsize=6)
    ax.set_yticks(range(len(classes)), classes, fontsize=6)
    ax.set_xlabel("Predicted"); ax.set_ylabel("True")
    ax.set_title("Confusion matrix - held-out test split")
    fig.colorbar(im, ax=ax, fraction=0.03)
    fig.tight_layout(); fig.savefig(path, dpi=130); plt.close(fig)
    return True


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default=str(HERE / "data" / "Training.csv"))
    ap.add_argument("--descriptions", default=str(HERE / "data" / "symptom_Description.csv"))
    ap.add_argument("--model-dir", default=str(HERE / "model"))
    ap.add_argument("--report-dir", default=str(HERE / "reports"))
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--skip-keras", action="store_true")
    args = ap.parse_args()

    seed = args.seed
    np.random.seed(seed)
    model_dir, report_dir = Path(args.model_dir), Path(args.report_dir)
    model_dir.mkdir(parents=True, exist_ok=True); report_dir.mkdir(parents=True, exist_ok=True)

    # 1. load / validate -------------------------------------------------
    raw, quality = load_dataset(args.data)
    print(f"[data] {quality.raw_rows} raw rows -> {quality.unique_rows} unique "
          f"({quality.exact_duplicate_rows} exact duplicates), {quality.n_symptoms} symptoms, "
          f"{quality.n_classes} classes, conflicting rows: {quality.conflicting_rows}")
    (report_dir / "data_quality.json").write_text(json.dumps(quality.to_dict(), indent=2))

    # 2. leakage demonstration -------------------------------------------
    leak = leakage_demo(raw, seed)
    print("[leakage]", json.dumps(leak))

    # 3. de-duplicate THEN split -----------------------------------------
    df = deduplicate(raw)
    X, y = split_xy(df)
    symptoms = list(X.columns)
    X_rest, X_test, y_rest, y_test = train_test_split(X, y, test_size=0.20, stratify=y, random_state=seed)
    X_train, X_val, y_train, y_val = train_test_split(X_rest, y_rest, test_size=0.25, stratify=y_rest, random_state=seed)
    print(f"[split] train={len(X_train)} val={len(X_val)} test={len(X_test)} (unique records)")
    assert not (set(map(tuple, X_train.to_numpy())) & set(map(tuple, X_test.to_numpy()))), "leakage!"

    # 4. model comparison ------------------------------------------------
    candidates = build_candidates(seed)
    if args.skip_keras:
        candidates.pop("keras_mlp")
    cv = RepeatedStratifiedKFold(n_splits=4, n_repeats=3, random_state=seed)
    Xr, yr = X_rest.to_numpy(np.float32), y_rest.to_numpy()
    rows = []
    for name, est in candidates.items():
        fold_f1, fold_acc = [], []
        for tr, te in cv.split(Xr, yr):
            m = clone(est).fit(Xr[tr], yr[tr])
            pred = m.predict(Xr[te])
            fold_f1.append(f1_score(yr[te], pred, average="macro", zero_division=0))
            fold_acc.append(accuracy_score(yr[te], pred))
        m = clone(est).fit(X_train.to_numpy(np.float32), y_train.to_numpy())
        vm = macro_metrics(y_val, m.predict(X_val.to_numpy(np.float32)))
        row = {"model": name, "cv_f1_macro_mean": float(np.mean(fold_f1)),
               "cv_f1_macro_std": float(np.std(fold_f1)), "cv_accuracy_mean": float(np.mean(fold_acc)),
               **{f"val_{k}": v for k, v in vm.items()}}
        rows.append(row)
        print(f"[compare] {name:20s} CV macro-F1 {row['cv_f1_macro_mean']:.3f} ± {row['cv_f1_macro_std']:.3f}"
              f" | val macro-F1 {vm['f1_macro']:.3f}")
    comparison = pd.DataFrame(rows)
    comparison.to_csv(report_dir / "model_comparison.csv", index=False)

    best = comparison["cv_f1_macro_mean"].max()
    chosen = next(r["model"] for r in rows if r["cv_f1_macro_mean"] >= best - TIE_TOLERANCE)
    print(f"[select] chosen model: {chosen} (best CV macro-F1 {best:.3f}, tolerance {TIE_TOLERANCE})")

    # 5. refit on train+val, evaluate ONCE on test ------------------------
    final = clone(candidates[chosen]).fit(Xr, yr)
    Xt = X_test.to_numpy(np.float32)
    pred = final.predict(Xt)
    test = macro_metrics(y_test, pred)
    test["top3_accuracy"] = top_k_accuracy(final, Xt, y_test.to_numpy(), 3)
    correct = int(np.sum(pred == y_test.to_numpy()))
    test["accuracy_95ci_wilson"] = list(wilson_interval(correct, len(y_test)))
    test["n_test_records"] = int(len(y_test))
    test["robustness_to_missing_symptoms"] = robustness_check(final, Xt, y_test.to_numpy(), seed)
    print("[test]", json.dumps(test))

    classes = list(final.classes_)
    cm = confusion_matrix(y_test, pred, labels=classes)
    pd.DataFrame(cm, index=classes, columns=classes).to_csv(report_dir / "confusion_matrix.csv")
    png = save_confusion_png(cm, classes, report_dir / "confusion_matrix.png")
    per_class = pd.DataFrame(classification_report(y_test, pred, labels=classes, output_dict=True,
                                                   zero_division=0)).T
    per_class.to_csv(report_dir / "per_class_metrics.csv")

    # 6. explainability + artefacts --------------------------------------
    sens = occlusion_sensitivity(final, Xr, yr)
    top = sorted(sens.items(), key=lambda kv: -kv[1])[:30]
    importance = {
        "method": "occlusion sensitivity: mean drop in the true-class model score when the symptom is "
                  "switched off, over training records where it is present",
        "caveat": "Describes what THIS MODEL relies on in THIS dataset. It is not medical causality.",
        "top_symptoms": [{"symptom": symptoms[j], "mean_score_drop": round(v, 4)} for j, v in top],
    }
    (model_dir / "feature_importance.json").write_text(json.dumps(importance, indent=2))

    if isinstance(final, KerasMLP):
        model_file = "model.keras"; final.save(model_dir / model_file); fmt = "keras"
    else:
        model_file = "model.joblib"; joblib.dump(final, model_dir / model_file, compress=3); fmt = "sklearn"

    desc_df = pd.read_csv(args.descriptions, header=None, names=["disease", "description"])
    desc_df["disease"] = desc_df["disease"].str.strip().replace(LABEL_ALIASES)
    descriptions = dict(zip(desc_df["disease"], desc_df["description"].str.strip()))
    missing = sorted(set(classes) - set(descriptions))
    if missing:
        print(f"[warn] no description for: {missing}")
    (model_dir / "descriptions.json").write_text(json.dumps(descriptions, indent=2, ensure_ascii=False))

    metadata = {
        "model_version": MODEL_VERSION,
        "model_name": chosen,
        "model_format": fmt,
        "model_file": model_file,
        "trained_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "seed": seed,
        "score_type": "uncalibrated_model_score",
        "symptoms": symptoms,
        "classes": classes,
        "dataset": {"file": Path(args.data).name,
                    "sha256": hashlib.sha256(Path(args.data).read_bytes()).hexdigest(),
                    "raw_rows": quality.raw_rows, "unique_rows": quality.unique_rows},
        "test_metrics": test,
        "libraries": {"python": platform.python_version(), "scikit-learn": sklearn.__version__,
                      "numpy": np.__version__, "pandas": pd.__version__},
    }
    (model_dir / "metadata.json").write_text(json.dumps(metadata, indent=2))

    report = {"data_quality": quality.to_dict(), "leakage_demonstration": leak,
              "split_sizes": {"train": len(X_train), "validation": len(X_val), "test": len(X_test)},
              "class_counts_after_dedup": y.value_counts().to_dict(),
              "model_comparison": rows, "selected_model": chosen,
              "selection_rule": f"highest mean CV macro-F1 on train+val (4-fold x 3 repeats); models within "
                                f"{TIE_TOLERANCE} of the best are tied and the simplest wins",
              "test_metrics_selected_model": test, "confusion_matrix_png": png}
    (report_dir / "evaluation_report.json").write_text(json.dumps(report, indent=2, default=str))
    write_markdown_report(report_dir / "evaluation_report.md", report, comparison, test, chosen)
    print(f"[done] artefacts in {model_dir} and {report_dir}")
    return 0


def write_markdown_report(path: Path, report: dict, comparison: pd.DataFrame, test: dict, chosen: str) -> None:
    q = report["data_quality"]
    lines = [
        "# Model evaluation report (generated by train_model.py)", "",
        "## Data quality",
        f"- Raw rows: {q['raw_rows']}; exact duplicate rows: {q['exact_duplicate_rows']}; "
        f"**unique rows: {q['unique_rows']}**",
        f"- Symptoms used: {q['n_symptoms']}; classes: {q['n_classes']}; missing values: {q['missing_values']}",
        f"- Dropped duplicate columns: {q['duplicate_columns_dropped']}; "
        f"all-zero columns: {q['constant_zero_columns_dropped']}",
        f"- Records with identical symptoms but different labels: {q['conflicting_rows']}",
        f"- Records per class after de-duplication: {q['class_count_min_after_dedup']}-"
        f"{q['class_count_max_after_dedup']} (raw data is perfectly balanced at "
        f"{q['class_count_min']} per class only because of duplication)", "",
        "## Leakage demonstration",
        f"- Naive split on raw rows: {report['leakage_demonstration']['naive_split_test_rows_also_in_train']:.0%} of "
        f"test rows also appear in train; accuracy {report['leakage_demonstration']['naive_split_accuracy_(INFLATED)']:.3f} "
        "(inflated). All numbers below use the de-duplicated split.", "",
        "## Model comparison (train+val CV, hold-out validation)", "",
        comparison.round(3).to_markdown(index=False), "",
        f"**Selected: `{chosen}`** - {report['selection_rule']}.", "",
        "## Held-out test results (selected model, evaluated once)",
        f"- n = {test['n_test_records']} unique records (about 1-2 per class)",
        f"- Accuracy {test['accuracy']:.3f} (95% Wilson CI {test['accuracy_95ci_wilson'][0]:.3f}-"
        f"{test['accuracy_95ci_wilson'][1]:.3f}); top-3 accuracy {test['top3_accuracy']:.3f}",
        f"- Macro precision {test['precision_macro']:.3f}, macro recall {test['recall_macro']:.3f}, "
        f"macro F1 {test['f1_macro']:.3f}", "",
        "## Robustness to incomplete symptom lists (test records, random symptoms removed)",
        "", "| symptoms removed | top-1 accuracy | top-3 accuracy |", "|---|---|---|",
        *[f"| {k.replace('drop_', '').replace('pct', '%')} | {v['top1_accuracy']:.3f} | {v['top3_accuracy']:.3f} |"
          for k, v in test["robustness_to_missing_symptoms"].items()], "",
        "## How to read these numbers",
        "- With only ~5-10 unique records per disease, per-class metrics rest on 1-2 test records and are very noisy.",
        "- The dataset looks like curated/synthetic symptom lists, not real patient records. High scores here "
        "do NOT imply clinical accuracy.",
        "- A neural network is not justified by ~300 unique records; it is reported for comparison only.",
        "- Scores are uncalibrated; no calibration was fitted because there is too little data to validate one.",
    ]
    path.write_text("\n".join(lines) + "\n")


if __name__ == "__main__":
    sys.exit(main())
