# Testing

| Suite | Command (run from) | Count | Verified in the authoring sandbox? |
|---|---|---|---|
| AI service | `python -m pytest -q` (`ai-service/`) | 30 | **Yes – all pass** |
| Frontend | `npm test` (`frontend/`) | 27 | **Yes – all pass**, `npm run build` OK |
| Backend | `mvn test` (`backend/`) | 43 | **No** – the sandbox could not download Maven dependencies (egress policy). Sources were only syntax-parsed. Run it yourself first and fix anything it reports. |

## AI service (`ai-service/tests`)
Preprocessing (normalisation, validation, encoding, dataset cleaning incl. duplicate-column handling), model loading and
failure, `/health` (ok and degraded), `/symptoms`, `/predict` (valid, normalised input, missing/too few/unsupported
symptoms, malformed JSON, wrong JSON type), API-key enforcement, 500s hiding details, no symptoms in logs, artefact sanity.

## Backend (`backend/src/test`)
Uses H2 (MySQL mode) and a fake Flask server built on the JDK HTTP server – no Docker or MySQL needed.
* `AuthIntegrationTest` – register/login, BCrypt, duplicate e-mail, validation, role cannot be chosen, invalid credentials,
  tampered/expired/foreign-signed tokens, DB-sourced role, CSRF header, logout.
* `AuthorizationIntegrationTest` – 401 vs 403 matrix for patient/admin/anonymous.
* `PredictionIntegrationTest` – persistence, pagination/search, ownership (404 for other patients), validation,
  unsupported symptom relay, **Flask timeout → 503**, **dropped connection → 503**, **Flask 500 → 502 without leaking text**.
* `AppointmentIntegrationTest` – slots, booking, double-booking (doctor and patient), invalid/past/off-day/after-hours,
  cancel + re-book, ownership, past-cancel, admin transitions, doctor availability management.
* `AdminAnalyticsIntegrationTest` – real counts and small-group suppression. `PatientProfileIntegrationTest`,
  `RateLimitIntegrationTest`.
* `SchemaMigrationTest` – applies the real Flyway migration on H2 (MySQL mode) and runs Hibernate `validate`.
  It is a drift check, **not** proof that the SQL runs on MySQL 8: also run the app once against MySQL (README step 3).

## Frontend (`frontend/src/__tests__`)
Validation rules; login (validation, success, admin redirect, server error, busy state); registration (mismatch,
success, duplicate); symptom checker (loading, load error + retry, search, select/remove, minimum, submit payload,
server error); prediction result rendering; protected-route behaviour for anonymous/patient/admin.

## Manual end-to-end checklist
1. Register → dashboard → symptom check → result → appears in History. 2. Second account cannot open the first one's
`/api/predictions/{id}`. 3. Stop Flask → symptom check shows the "temporarily unavailable" message. 4. Book, cancel,
re-book a slot; admin confirms. 5. `curl -i -X POST localhost:8080/api/predictions` with a stale cookie but no
`X-Requested-With` → 403.
