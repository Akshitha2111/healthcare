# AI-Powered Smart Healthcare Diagnosis Assistant

Educational full-stack project: React + Spring Boot + Flask (scikit-learn / Keras) + MySQL.
Patients enter symptoms, get ranked **model predictions** with limitations and general guidance, keep a private history and
book appointments; administrators manage appointments/doctors and see aggregate analytics.

> **Not a medical device.** Output is an educational statistic from a small public dataset, not a diagnosis, and must not
> replace a qualified professional. No HIPAA/GDPR (or other) compliance is claimed – see `docs/architecture.md`.

## Status – what has and has not been verified

| Part | State |
|---|---|
| AI service (training, Flask, 30 tests) | built, run and tested |
| React frontend (27 tests, production build) | built, run and tested |
| Spring Boot backend + tests + Flyway schema | **fully written but never compiled or run by the author** (sandbox had no access to Maven Central). Run `mvn test` first. |
| MySQL | schema parsed as valid MySQL syntax; never executed against a MySQL server |

## Repository layout
```
ai-service/  Flask API, training pipeline, data/, model/ (trained artefacts), reports/, tests/
backend/     Spring Boot 3.5 (Java 17+), Flyway migration, tests
frontend/    React 18 + Vite + Bootstrap + Recharts, Vitest tests
database/    schema.sql, seed.sql, README
docs/        architecture.md, api-documentation.md, testing.md, deployment.md, requests.http
```

## Dataset (included: `ai-service/data/`)
* `Training.csv`, `symptom_Description.csv` – the public Kaggle dataset **"Disease Symptom Prediction" (Pranay Patil,
  `itachi9604`)**, downloaded from the GitHub repository `itachi9604/healthcare-chatbot`. 4,920 rows, 132 symptom columns, 41 diseases.
  Check the dataset's licence on its Kaggle page before redistributing publicly (not verified by the author).
* Disease descriptions come from that dataset. Its "precaution" file is **deliberately not used**: it is not medically reviewed
  (e.g. "stop taking drug"), and this app must stay non-prescriptive. `Testing.csv` is not used: all its rows already occur in the training file.
* **Limitations (important):** 94 % of rows are exact duplicates – only **304 unique** records, 5–10 per disease. It looks
  curated/synthetic, not real patient data. Nearly every model scores ≈100 % on it, which says more about the dataset than about
  real-world accuracy (a robustness test with half the symptoms removed drops top-1 to ≈ 92 %). Cleaning applied: fixed header typos,
  removed a repeated `fluid_overload` header, removed one all-zero column, trimmed labels, de-duplicated before splitting.
  Full numbers: `ai-service/reports/evaluation_report.md`.

## Prerequisites
Python 3.10–3.13, Node 18+ (tested 22), JDK 17+ (tested 21), Maven 3.9+, MySQL 8 (or Docker for `docker compose`).

## Run locally (4 terminals)

**1. Database** (repo root)
```bash
cp .env.example .env            # then edit: passwords, JWT_SECRET (openssl rand -base64 48), ADMIN_PASSWORD
docker compose --env-file .env up -d     # or install MySQL 8 and follow database/README.md
```

**2. AI service** (`ai-service/`) – trained artefacts are already included
```bash
python -m venv .venv && source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python -m pytest -q                       # 30 tests
python app.py                             # http://127.0.0.1:5000/health
# optional – retrain and regenerate model/ and reports/ (≈2 min with TensorFlow; --skip-keras is faster)
python train_model.py
```

**3. Backend** (`backend/`)
```bash
set -a; source ../.env; set +a            # export the variables from .env (see .env.example for PowerShell)
mvn test                                  # H2 + fake AI server; no MySQL needed
mvn spring-boot:run                       # http://localhost:8080  (Flyway creates the tables)
curl localhost:8080/actuator/health/readiness
```
No MySQL handy? `SPRING_PROFILES_ACTIVE=h2 DB_PASSWORD=x JWT_SECRET=<32+ chars> mvn spring-boot:run` runs on in-memory H2 (data lost on exit).
If startup says `Could not resolve placeholder 'JWT_SECRET'` or `DB_PASSWORD`, the variables are not exported – by design there are no defaults.
To make Hibernate check entities against the schema once: `JPA_DDL_AUTO=validate`.

**4. Frontend** (`frontend/`)
```bash
npm install
npm test            # 27 tests
npm run dev         # http://localhost:5173  (proxies /api to :8080)
```
Log in as the administrator with `ADMIN_EMAIL` / `ADMIN_PASSWORD`; register normal users in the UI.

## Quick API check (see `docs/requests.http` and `docs/api-documentation.md`)
```bash
curl -s localhost:5000/predict -H 'Content-Type: application/json' -d '{"symptoms":["itching","skin_rash","nodal_skin_eruptions"]}'
curl -si -c jar localhost:8080/api/auth/register -H 'Content-Type: application/json' -H 'X-Requested-With: XMLHttpRequest' \
     -d '{"fullName":"Asha Rao","email":"asha@example.com","password":"Passw0rd123"}'
curl -s -b jar localhost:8080/api/predictions -H 'Content-Type: application/json' -H 'X-Requested-With: XMLHttpRequest' \
     -d '{"symptoms":["itching","skin_rash"]}'
```

## Deployment
See `docs/deployment.md` (frontend static host, backend + AI service containers/VMs, managed MySQL, HTTPS, CORS, checklist).

## Known limitations / future work
* Tiny, likely synthetic dataset; no clinical validation; scores uncalibrated; no age/sex/severity inputs.
* Backend not yet compiled/run by the author (see Status) – expect to fix small issues on first `mvn test`.
* Rate limiting is in-memory (single instance); no account deletion/data-export endpoints; no e-mail verification, password
  reset or MFA; JWTs cannot be revoked before expiry; admin accounts are created only via environment variables.
* No CI pipeline, container images or load tests. Doctors cannot be deleted (deactivate instead).
* Future: better data (licensed clinical sources), calibration with sufficient data, SHAP-style explanations, audit log,
  notifications, Redis rate limiting, OpenAPI/Swagger, Playwright end-to-end tests.


terminal 1:

cd ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python -m pytest -q
python app.py


terminal 2:

cd backend
$env:SPRING_PROFILES_ACTIVE="h2"
$env:DB_PASSWORD="x"
$env:JWT_SECRET="#akshitha@110108akshitha@110108#"
$env:ADMIN_EMAIL="admin@example.com"
$env:ADMIN_PASSWORD="Admin12345"
$env:SEED_DEMO_DOCTORS="true"
$env:ANALYTICS_MIN_GROUP_SIZE="1"
mvn test
mvn spring-boot:run


or:
CREATE DATABASE healthcare_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'healthcare'@'localhost' IDENTIFIED BY 'YourDbPassword1';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON healthcare_assistant.* TO 'healthcare'@'localhost';



terminal 3:

cd frontend
npm install
npm test
npm run dev