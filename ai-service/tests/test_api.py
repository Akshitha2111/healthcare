import json
from pathlib import Path

import pytest

from app import create_app
from predict import ModelLoadError, Predictor


def post(client, body, **kw):
    return client.post("/predict", data=json.dumps(body), content_type="application/json", **kw)


def test_model_loads_and_is_consistent(predictor):
    assert len(predictor.symptoms) > 100 and len(predictor.classes) == 41
    assert predictor.model_version


def test_missing_model_dir_raises(tmp_path):
    with pytest.raises(ModelLoadError):
        Predictor.load(tmp_path)


def test_health_ok(client):
    r = client.get("/health")
    assert r.status_code == 200
    body = r.get_json()
    assert body["status"] == "ok" and body["model_loaded"] is True


def test_health_degraded_when_model_missing(tmp_path):
    app = create_app(config={"MODEL_DIR": str(tmp_path)})
    c = app.test_client()
    assert c.get("/health").status_code == 503
    assert c.get("/health").get_json()["model_loaded"] is False
    assert post(c, {"symptoms": ["cough", "high_fever"]}).status_code == 503


def test_symptoms_endpoint(client):
    body = client.get("/symptoms").get_json()
    assert body["success"] and body["count"] == len(body["symptoms"])
    assert {"name": "high_fever", "label": "High fever"} in body["symptoms"]


def test_predict_valid(client):
    r = post(client, {"symptoms": ["itching", "skin_rash", "nodal_skin_eruptions"]})
    assert r.status_code == 200
    b = r.get_json()
    assert b["success"] is True
    assert b["top_prediction"] == b["predictions"][0]["disease"]
    assert len(b["predictions"]) == 3 and [p["rank"] for p in b["predictions"]] == [1, 2, 3]
    scores = [p["score"] for p in b["predictions"]]
    assert scores == sorted(scores, reverse=True) and all(0 <= s <= 1 for s in scores)
    assert b["score_type"] == "uncalibrated_model_score"
    for key in ("model_version", "disclaimer", "urgent_care_notice", "limitations", "explanation",
                "general_guidance", "symptoms_used"):
        assert b[key]
    assert b["explanation"]["influential_symptoms"][0]["symptom"] in b["symptoms_used"]


def test_predict_normalises_labels_and_dedupes(client):
    r = post(client, {"symptoms": ["Itching", " skin rash ", "itching"]})
    assert r.status_code == 200
    assert r.get_json()["symptoms_used"] == ["itching", "skin_rash"]


def test_predict_missing_symptoms(client):
    r = post(client, {})
    assert r.status_code == 400 and r.get_json()["error"]["code"] == "INVALID_SYMPTOMS"


def test_predict_too_few_symptoms(client):
    assert post(client, {"symptoms": ["cough"]}).status_code == 400


def test_predict_unsupported_symptom(client):
    r = post(client, {"symptoms": ["cough", "not_a_symptom"]})
    assert r.status_code == 400 and "unsupported" in r.get_json()["error"]["message"]


def test_predict_malformed_json(client):
    r = client.post("/predict", data="{not json", content_type="application/json")
    assert r.status_code == 400 and r.get_json()["error"]["code"] == "INVALID_JSON"


def test_predict_non_object_json(client):
    assert post(client, ["cough", "fever"]).status_code == 400


def test_unknown_route_and_method_use_json_errors(client):
    assert client.get("/nope").get_json()["success"] is False
    assert client.get("/predict").status_code == 405


def test_api_key_enforced_when_configured(predictor):
    c = create_app(predictor=predictor, config={"API_KEY": "s3cret"}).test_client()
    body = {"symptoms": ["itching", "skin_rash"]}
    assert post(c, body).status_code == 401
    assert post(c, body, headers={"X-Internal-Api-Key": "wrong"}).status_code == 401
    assert post(c, body, headers={"X-Internal-Api-Key": "s3cret"}).status_code == 200
    assert c.get("/health").status_code == 200       # health stays open for probes


def test_symptoms_not_logged(predictor, caplog):
    c = create_app(predictor=predictor).test_client()
    with caplog.at_level("INFO"):
        post(c, {"symptoms": ["itching", "skin_rash"]})
    assert "itching" not in caplog.text and "skin_rash" not in caplog.text


def test_unexpected_error_hides_details(predictor, monkeypatch):
    app = create_app(predictor=predictor)
    monkeypatch.setattr(predictor, "predict", lambda *a, **k: 1 / 0)
    r = app.test_client().post("/predict", json={"symptoms": ["itching", "skin_rash"]})
    assert r.status_code == 500 and "division" not in r.get_data(as_text=True)


def test_every_training_record_is_predicted_in_top3(predictor):
    """Sanity check on artefacts: the deployed model reproduces dataset records in its top-3."""
    from preprocessing import deduplicate, load_dataset, split_xy
    df, _ = load_dataset(Path("data/Training.csv"))
    X, y = split_xy(deduplicate(df))
    ok = 0
    for (_, row), label in list(zip(X.iterrows(), y))[:60]:
        syms = [s for s, v in row.items() if v == 1]
        ok += label in [p["disease"] for p in predictor.predict(syms)["predictions"]]
    assert ok >= 57
