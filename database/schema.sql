-- AI-Powered Smart Healthcare Diagnosis Assistant - schema v1 (MySQL 8)
-- Create the database first (see ../../../../../database/README or the root README):
--   CREATE DATABASE healthcare_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- patient_id columns reference users.id of a user whose role is PATIENT.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('PATIENT', 'ADMIN'))
);

CREATE TABLE patient_profiles (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    phone         VARCHAR(20) NULL,
    date_of_birth DATE        NULL,
    gender        VARCHAR(20) NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_patient_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_patient_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE predictions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    patient_id      BIGINT       NOT NULL,
    predicted_class VARCHAR(120) NOT NULL,
    model_name      VARCHAR(60)  NOT NULL,
    model_version   VARCHAR(30)  NOT NULL,
    score_type      VARCHAR(60)  NOT NULL,
    disclaimer      TEXT         NOT NULL,
    details_json    TEXT         NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_predictions_patient FOREIGN KEY (patient_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_predictions_patient_created ON predictions (patient_id, created_at);
CREATE INDEX idx_predictions_created ON predictions (created_at);

CREATE TABLE prediction_symptoms (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    prediction_id BIGINT      NOT NULL,
    symptom_name  VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_prediction_symptom UNIQUE (prediction_id, symptom_name),
    CONSTRAINT fk_prediction_symptoms_prediction FOREIGN KEY (prediction_id) REFERENCES predictions (id) ON DELETE CASCADE
);
CREATE INDEX idx_prediction_symptoms_name ON prediction_symptoms (symptom_name);

-- "rank" is a reserved word in MySQL 8, so the column is called result_rank.
CREATE TABLE prediction_results (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    prediction_id   BIGINT       NOT NULL,
    predicted_class VARCHAR(120) NOT NULL,
    score           DOUBLE       NOT NULL,
    result_rank     INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_prediction_result_rank UNIQUE (prediction_id, result_rank),
    CONSTRAINT fk_prediction_results_prediction FOREIGN KEY (prediction_id) REFERENCES predictions (id) ON DELETE CASCADE
);

CREATE TABLE doctors (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(120) NOT NULL,
    specialty      VARCHAR(120) NOT NULL,
    available_days VARCHAR(100) NOT NULL,   -- comma separated java.time.DayOfWeek names, e.g. MONDAY,WEDNESDAY
    start_time     TIME         NOT NULL,
    end_time       TIME         NOT NULL,
    slot_minutes   INT          NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_doctors_slot CHECK (slot_minutes BETWEEN 10 AND 120)
);
CREATE INDEX idx_doctors_active ON doctors (active);

-- slot_active is TRUE while the appointment occupies its slot (PENDING/CONFIRMED) and NULL otherwise.
-- NULLs are distinct in a UNIQUE index, so cancelled slots can be re-booked, while two ACTIVE
-- appointments for the same doctor/slot (or the same patient/slot) are rejected by the database itself.
CREATE TABLE appointments (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    patient_id       BIGINT      NOT NULL,
    doctor_id        BIGINT      NOT NULL,
    appointment_date DATE        NOT NULL,
    appointment_time TIME        NOT NULL,
    status           VARCHAR(20) NOT NULL,
    slot_active      BOOLEAN     NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_appt_doctor_slot UNIQUE (doctor_id, appointment_date, appointment_time, slot_active),
    CONSTRAINT uq_appt_patient_slot UNIQUE (patient_id, appointment_date, appointment_time, slot_active),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT chk_appointments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_appointments_patient_date ON appointments (patient_id, appointment_date);
CREATE INDEX idx_appointments_status ON appointments (status);
