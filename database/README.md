# Database

* `schema.sql` – identical copy of `backend/src/main/resources/db/migration/V1__init_schema.sql`.
  The backend applies it automatically through **Flyway** on first start (`ddl-auto=none`; Hibernate never alters the schema).
  Use it manually only if you want to inspect or pre-create the schema.
* `seed.sql` – optional, FICTIONAL demo doctors. The administrator account is created by the backend from
  `ADMIN_EMAIL`/`ADMIN_PASSWORD`, so no password hash is committed.

## Create the database and user (MySQL 8)

```sql
CREATE DATABASE healthcare_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'healthcare'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON healthcare_assistant.* TO 'healthcare'@'localhost';
```
(After the first run you may revoke `CREATE, ALTER, INDEX, REFERENCES` and use a separate migration user in production.)

## Tables

| Table | Purpose |
|---|---|
| `users` | accounts: name, unique email, BCrypt hash, role (`PATIENT`/`ADMIN`) |
| `patient_profiles` | 1:1 with a patient user: optional phone, date of birth, gender |
| `predictions` | one row per symptom check: predicted class, model name/version, disclaimer, JSON details |
| `prediction_symptoms` | symptoms submitted for a prediction (unique per prediction) |
| `prediction_results` | ranked model outputs (`result_rank` 1..3) with score |
| `doctors` | name, specialty, available weekdays, working window, slot length, active flag |
| `appointments` | patient ↔ doctor, date, time, status, `slot_active` (see below) |

Double-booking protection: `appointments.slot_active` is `TRUE` while an appointment is PENDING/CONFIRMED and `NULL`
otherwise. Unique indexes on `(doctor_id, date, time, slot_active)` and `(patient_id, date, time, slot_active)` reject a
second active booking (NULLs are distinct, so cancelled slots can be re-booked).
