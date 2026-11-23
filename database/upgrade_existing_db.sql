-- One-time upgrade for the original project schema. Back up hospital_db first.
-- Run this script after selecting the existing hospital_db database.
USE hospital_db;

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN role ENUM('ADMIN','DOCTOR','PATIENT') NOT NULL DEFAULT 'PATIENT',
    ADD COLUMN email VARCHAR(254) NULL,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users MODIFY password VARCHAR(255) NULL;
ALTER TABLE users ADD UNIQUE KEY uq_users_email (email);
UPDATE users SET role='ADMIN' WHERE username='admin';

CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
) ENGINE=InnoDB;
INSERT INTO departments(name, description) VALUES
('General Medicine', 'Primary and general medical care'),
('Cardiology', 'Heart and circulatory system care'),
('Pediatrics', 'Medical care for infants, children, and adolescents'),
('Orthopedics', 'Bone, joint, and musculoskeletal care');

ALTER TABLE patients
    ADD COLUMN user_id INT NULL UNIQUE,
    ADD COLUMN email VARCHAR(254) NULL,
    ADD UNIQUE KEY uq_patients_email (email),
    MODIFY age TINYINT UNSIGNED NOT NULL;
ALTER TABLE patients ADD CONSTRAINT fk_patients_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE doctors
    ADD COLUMN user_id INT NULL UNIQUE,
    ADD COLUMN department_id INT NULL,
    ADD COLUMN email VARCHAR(254) NULL,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    MODIFY fee DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE doctors ADD CONSTRAINT fk_doctors_department
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;
CREATE INDEX idx_doctors_specialization ON doctors(specialization);

CREATE TABLE doctor_availability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    day_of_week TINYINT UNSIGNED NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 30,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_availability_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    UNIQUE KEY uq_availability_window (doctor_id, day_of_week, start_time, end_time),
    CONSTRAINT chk_availability_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_availability_time CHECK (end_time > start_time),
    CONSTRAINT chk_availability_slot CHECK (slot_minutes BETWEEN 5 AND 240)
) ENGINE=InnoDB;

ALTER TABLE appointments
    ADD COLUMN appt_time TIME NOT NULL DEFAULT '09:00:00',
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    MODIFY status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    MODIFY reason VARCHAR(500) NOT NULL DEFAULT 'Consultation';
UPDATE appointments SET status=CASE UPPER(status)
    WHEN 'SCHEDULED' THEN 'SCHEDULED' WHEN 'COMPLETED' THEN 'COMPLETED'
    WHEN 'CANCELLED' THEN 'CANCELLED' ELSE 'SCHEDULED' END;
ALTER TABLE appointments MODIFY status ENUM('SCHEDULED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'SCHEDULED';
ALTER TABLE appointments DROP FOREIGN KEY appointments_ibfk_1;
ALTER TABLE appointments DROP FOREIGN KEY appointments_ibfk_2;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT;
CREATE INDEX idx_appointments_patient ON appointments(patient_id, appt_date);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id, appt_date);
CREATE INDEX idx_appointments_status ON appointments(status, appt_date);
ALTER TABLE appointments ADD COLUMN active_slot TIME
    GENERATED ALWAYS AS (CASE WHEN status = 'CANCELLED' THEN NULL ELSE appt_time END) STORED;
ALTER TABLE appointments ADD UNIQUE KEY uq_doctor_appointment_slot (doctor_id, appt_date, active_slot);
ALTER TABLE appointments ADD CONSTRAINT chk_appointment_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0);
