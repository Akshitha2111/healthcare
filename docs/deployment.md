# Deployment guide

Topology: static React build → HTTPS → Spring Boot (public) → Flask (private network only) and managed MySQL (private).

## 1. Managed MySQL
Create a MySQL 8 instance (RDS, Cloud SQL, Azure Database for MySQL, PlanetScale-compatible…). Create database
`healthcare_assistant` (utf8mb4) and an application user. Require TLS (`DB_URL=...?sslMode=REQUIRED`), restrict inbound
access to the backend only, enable automated backups. Flyway applies `V1__init_schema.sql` on first start.

## 2. AI service
Build once: `pip install -r requirements.txt` (for serving with a scikit-learn model you may drop `tensorflow`/`matplotlib`/`tabulate`).
Ship `model/` with the code (≈50 KB here; use artefact storage if models grow large; do not commit big binaries).
Run: `gunicorn -w 2 -b 0.0.0.0:5000 "app:create_app()"` inside a private network. Set `AI_SERVICE_API_KEY` (same value in the backend).
Do **not** expose it publicly. Never run `flask --debug` in production.

## 3. Backend
`mvn -DskipTests package` → `java -jar target/healthcare-backend-1.0.0.jar`. Required environment: `DB_URL, DB_USERNAME, DB_PASSWORD,
JWT_SECRET, AI_SERVICE_URL, CORS_ALLOWED_ORIGINS` (exact https origin of the frontend), `COOKIE_SECURE=true`, optional
`AI_SERVICE_API_KEY, ADMIN_EMAIL, ADMIN_PASSWORD` (remove `ADMIN_PASSWORD` after the first start). Terminate TLS at a load balancer/reverse proxy.
Cookie settings: frontend and API on the same site (e.g. `app.example.com` and `api.example.com`) → `COOKIE_SAME_SITE=Lax`; on different sites →
`None` with `COOKIE_SECURE=true`. Best: serve both behind one domain (`/` static, `/api` → backend) so the browser sees one origin.
If a proxy sits in front, set `server.forward-headers-strategy=framework` and make the rate limiter see real client IPs (or rate-limit at the gateway).

## 4. Frontend
`VITE_API_BASE_URL=https://api.example.com npm run build` (empty if same origin) → upload `dist/` to a static host/CDN with SPA
fallback to `index.html` (rewrite all unknown paths). No secrets belong in `VITE_*` variables.

## 5. Health checks and logs
* Backend: liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness` (checks DB and the AI service).
* AI: `GET /health` (503 if artefacts failed to load).
* Logs contain method/path/status and exception class names, never symptoms, passwords or tokens. Inspect with your platform's log viewer
  (`docker logs`, `journalctl`, CloudWatch…); restrict who can read them anyway, because timestamps + paths can still be sensitive.

## 6. Checklist
- [ ] HTTPS everywhere; HSTS at the proxy; `COOKIE_SECURE=true`
- [ ] `CORS_ALLOWED_ORIGINS` is the exact frontend origin (no `*`)
- [ ] `JWT_SECRET` random, ≥ 32 chars, stored in a secret manager; different per environment
- [ ] DB reachable from the backend only; TLS to DB; backups tested; least-privilege DB user after the first migration
- [ ] Flask not publicly reachable; API key set on both sides; `model/` present (`/health` shows `model_loaded: true`)
- [ ] Backend → Flask round trip works (`/actuator/health/readiness` is UP); timeouts tuned (`AI_TIMEOUT_MS`)
- [ ] Admin account created, bootstrap password removed from the environment
- [ ] `SEED_DEMO_DOCTORS=false`; real doctors created in the admin UI
- [ ] Privacy review done (consent, retention, deletion, hosting region) before handling real users' health data
