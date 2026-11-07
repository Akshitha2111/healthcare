-- OPTIONAL demo data: fictional doctors only. (The admin account is created by the backend from
-- ADMIN_EMAIL / ADMIN_PASSWORD so that no password hash is ever committed.)
-- Alternatively set SEED_DEMO_DOCTORS=true and the backend inserts the same rows when the table is empty.
USE healthcare_assistant;
INSERT INTO doctors (name, specialty, available_days, start_time, end_time, slot_minutes, active, created_at, updated_at) VALUES
('Dr. Asha Verma (demo)',  'General Medicine', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', '09:00:00', '13:00:00', 30, TRUE, NOW(6), NOW(6)),
('Dr. Rohan Iyer (demo)',  'Internal Medicine', 'MONDAY,WEDNESDAY,FRIDAY',                  '14:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
('Dr. Meera Nair (demo)',  'Family Medicine',  'TUESDAY,THURSDAY,SATURDAY',                 '10:00:00', '14:00:00', 20, TRUE, NOW(6), NOW(6));
