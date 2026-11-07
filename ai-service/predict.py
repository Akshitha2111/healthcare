"""Inference only. Loads saved artefacts once; never trains."""
from __future__ import annotations

import json
from pathlib import Path

import joblib
import numpy as np

from preprocessing import display_name, encode_symptoms, validate_symptoms

DISCLAIMER = ("This is an educational prediction produced by a statistical model. It is not a medical "
              "diagnosis and does not replace a qualified healthcare professional.")
URGENT_NOTICE = ("Seek urgent medical care (or call your local emergency number) for severe or rapidly "
                 "worsening symptoms, chest pain, difficulty breathing, confusion, fainting, sudden weakness "
                 "or numbness, severe bleeding, or any situation you believe is an emergency.")
LIMITATIONS = [
    "The model was trained on a small public dataset (about 300 unique symptom lists for 41 conditions) "
    "that appears to be curated or synthetic, not real patient records.",
    "Scores are relative model scores, not calibrated probabilities and not clinical risk estimates.",
    "Many different conditions share symptoms; the model only knows the conditions in its dataset.",
    "Age, sex, medical history, test results and symptom severity are not considered.",
    "The 'influence' values show what this model reacts to; they do not explain a medical cause.",
]
GENERAL_GUIDANCE = [
    "Consider discussing these symptoms with a qualified healthcare professional, especially if they "
    "persist, worsen, or worry you.",
    "Do not start, stop or change any medicine based on this result.",
    "Rest, drink fluids and monitor how your symptoms change; keep a note of when they started.",
]


class ModelLoadError(RuntimeError):
    pass


class Predictor:
    def __init__(self, model, metadata: dict, descriptions: dict[str, str]):
        self.model = model
        self.metadata = metadata
        self.symptoms: list[str] = metadata["symptoms"]
        self.classes = list(model.classes_)
        self.descriptions = descriptions
        self.model_version: str = metadata["model_version"]
        self.model_name: str = metadata["model_name"]

    # ---- loading ---------------------------------------------------
    @classmethod
    def load(cls, model_dir: str | Path) -> "Predictor":
        model_dir = Path(model_dir)
        try:
            metadata = json.loads((model_dir / "metadata.json").read_text())
            model_path = model_dir / metadata["model_file"]
            if metadata["model_format"] == "keras":
                from models import KerasMLP  # lazy: TensorFlow only when needed
                model = KerasMLP.load(model_path, metadata["classes"])
            else:
                model = joblib.load(model_path)
            desc_path = model_dir / "descriptions.json"
            descriptions = json.loads(desc_path.read_text()) if desc_path.exists() else {}
        except (OSError, KeyError, ValueError) as exc:
            raise ModelLoadError(f"could not load model artefacts: {type(exc).__name__}") from exc
        if list(model.classes_) != list(metadata["classes"]):
            raise ModelLoadError("model classes do not match metadata")
        return cls(model, metadata, descriptions)

    # ---- inference -------------------------------------------------
    def validate(self, raw, min_symptoms: int = 2, max_symptoms: int = 20) -> list[str]:
        return validate_symptoms(raw, self.symptoms, min_symptoms, max_symptoms)

    def predict(self, symptoms: list[str], top_n: int = 3) -> dict:
        """`symptoms` must already be validated (see validate())."""
        x = encode_symptoms(symptoms, self.symptoms)
        scores = self.model.predict_proba(x)[0]
        order = np.argsort(-scores)[:top_n]
        predictions = [{"rank": i + 1, "disease": self.classes[j], "score": round(float(scores[j]), 4)}
                       for i, j in enumerate(order)]
        top_idx = int(order[0])
        top = predictions[0]["disease"]
        return {
            "success": True,
            "model_version": self.model_version,
            "model_name": self.model_name,
            "score_type": self.metadata.get("score_type", "uncalibrated_model_score"),
            "top_prediction": top,
            "predictions": predictions,
            "symptoms_used": symptoms,
            "explanation": {
                "method": "occlusion sensitivity: how much the top score falls when one symptom is removed",
                "influential_symptoms": self._influence(x, symptoms, top_idx),
                "caveat": "Shows what the model reacts to, not a medical explanation.",
            },
            "description": self.descriptions.get(top),
            "general_guidance": GENERAL_GUIDANCE,
            "urgent_care_notice": URGENT_NOTICE,
            "limitations": LIMITATIONS,
            "disclaimer": DISCLAIMER,
        }

    def _influence(self, x: np.ndarray, symptoms: list[str], class_idx: int) -> list[dict]:
        base = float(self.model.predict_proba(x)[0, class_idx])
        variants = np.repeat(x, len(symptoms), axis=0)
        for r, s in enumerate(symptoms):
            variants[r, self.symptoms.index(s)] = 0.0
        without = self.model.predict_proba(variants)[:, class_idx]
        items = [{"symptom": s, "label": display_name(s), "influence": round(base - float(w), 4)}
                 for s, w in zip(symptoms, without)]
        return sorted(items, key=lambda d: -d["influence"])

    def symptom_catalog(self) -> list[dict]:
        return [{"name": s, "label": display_name(s)} for s in self.symptoms]
