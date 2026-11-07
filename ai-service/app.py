"""Flask inference microservice.

Run locally:   python app.py
Production:    gunicorn -w 2 -b 0.0.0.0:5000 "app:create_app()"

Environment variables
  AI_MODEL_DIR        directory with model artefacts          (default ./model)
  AI_SERVICE_API_KEY  if set, /predict and /symptoms require header X-Internal-Api-Key
  AI_CORS_ORIGINS     comma separated browser origins allowed (default: none - the
                      browser talks to Spring Boot, not to this service)
  AI_MAX_SYMPTOMS     upper bound for symptoms per request    (default 20)
  AI_LOG_LEVEL        logging level                           (default INFO)
"""
from __future__ import annotations

import hmac
import logging
import os
import time
from pathlib import Path

from flask import Flask, g, jsonify, request
from flask_cors import CORS
from werkzeug.exceptions import HTTPException

from predict import ModelLoadError, Predictor
from preprocessing import SymptomValidationError

log = logging.getLogger("ai-service")
MIN_SYMPTOMS = 2


def error(status: int, code: str, message: str):
    return jsonify({"success": False, "error": {"code": code, "message": message}}), status


def create_app(predictor: Predictor | None = None, config: dict | None = None) -> Flask:
    app = Flask(__name__)
    cfg = {
        "MODEL_DIR": os.getenv("AI_MODEL_DIR", str(Path(__file__).parent / "model")),
        "API_KEY": os.getenv("AI_SERVICE_API_KEY", ""),
        "MAX_SYMPTOMS": int(os.getenv("AI_MAX_SYMPTOMS", "20")),
        "CORS_ORIGINS": [o.strip() for o in os.getenv("AI_CORS_ORIGINS", "").split(",") if o.strip()],
        "MAX_CONTENT_LENGTH": 16 * 1024,
        "JSON_SORT_KEYS": False,
    }
    cfg.update(config or {})
    app.config.update(cfg)
    logging.basicConfig(level=os.getenv("AI_LOG_LEVEL", "INFO"),
                        format="%(asctime)s %(levelname)s %(name)s %(message)s")

    if app.config["CORS_ORIGINS"]:
        CORS(app, origins=app.config["CORS_ORIGINS"], methods=["GET", "POST"],
             allow_headers=["Content-Type", "X-Internal-Api-Key"])

    # Load the model ONCE at start-up. A failure leaves the service up but unhealthy.
    app.extensions["predictor"] = predictor
    app.extensions["load_error"] = None
    if predictor is None:
        try:
            app.extensions["predictor"] = Predictor.load(app.config["MODEL_DIR"])
            log.info("model loaded: %s v%s", app.extensions["predictor"].model_name,
                     app.extensions["predictor"].model_version)
        except ModelLoadError as exc:
            app.extensions["load_error"] = str(exc)
            log.error("model failed to load: %s", exc)

    def get_predictor() -> Predictor | None:
        return app.extensions["predictor"]

    @app.before_request
    def _start():
        g.t0 = time.perf_counter()

    @app.after_request
    def _log(resp):
        # Method, path, status, duration only. Request bodies (symptoms) are NEVER logged.
        log.info("%s %s -> %s (%.0f ms)", request.method, request.path, resp.status_code,
                 (time.perf_counter() - g.get("t0", time.perf_counter())) * 1000)
        resp.headers["Cache-Control"] = "no-store"
        resp.headers["X-Content-Type-Options"] = "nosniff"
        return resp

    def require_key():
        key = app.config["API_KEY"]
        if not key:
            return None
        supplied = request.headers.get("X-Internal-Api-Key", "")
        if not hmac.compare_digest(supplied.encode(), key.encode()):
            return error(401, "UNAUTHORIZED", "missing or invalid service key")
        return None

    @app.get("/health")
    def health():
        p = get_predictor()
        body = {"status": "ok" if p else "degraded", "model_loaded": p is not None}
        if p:
            body.update(model_version=p.model_version, model_name=p.model_name,
                        n_symptoms=len(p.symptoms), n_classes=len(p.classes))
        return jsonify(body), (200 if p else 503)

    @app.get("/symptoms")
    def symptoms():
        denied = require_key()
        if denied:
            return denied
        p = get_predictor()
        if not p:
            return error(503, "MODEL_UNAVAILABLE", "model is not loaded")
        return jsonify({"success": True, "count": len(p.symptoms), "min_symptoms": MIN_SYMPTOMS,
                        "max_symptoms": app.config["MAX_SYMPTOMS"], "symptoms": p.symptom_catalog()})

    @app.post("/predict")
    def predict():
        denied = require_key()
        if denied:
            return denied
        p = get_predictor()
        if not p:
            return error(503, "MODEL_UNAVAILABLE", "model is not loaded")
        payload = request.get_json(force=True, silent=True)
        if not isinstance(payload, dict):
            return error(400, "INVALID_JSON", "request body must be a JSON object")
        try:
            validated = p.validate(payload.get("symptoms"), MIN_SYMPTOMS, app.config["MAX_SYMPTOMS"])
        except SymptomValidationError as exc:
            return error(400, "INVALID_SYMPTOMS", str(exc))
        return jsonify(p.predict(validated))

    @app.errorhandler(HTTPException)
    def http_error(exc: HTTPException):
        return error(exc.code or 500, (exc.name or "ERROR").upper().replace(" ", "_"), exc.description or "")

    @app.errorhandler(Exception)
    def unexpected(exc: Exception):
        log.exception("unhandled error")  # stack trace goes to server logs only
        return error(500, "INTERNAL_ERROR", "unexpected server error")

    return app


if __name__ == "__main__":
    # Development server only. debug stays OFF (the Werkzeug debugger allows remote code execution).
    create_app().run(host=os.getenv("AI_HOST", "127.0.0.1"), port=int(os.getenv("AI_PORT", "5000")))
