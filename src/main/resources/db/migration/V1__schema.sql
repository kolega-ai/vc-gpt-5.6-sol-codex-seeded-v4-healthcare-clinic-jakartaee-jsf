CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE user_accounts (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 email VARCHAR(160) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL, display_name VARCHAR(120) NOT NULL,
 role VARCHAR(30) NOT NULL CHECK (role IN ('CLINIC_ADMIN','CLINICIAN','FRONT_DESK','PATIENT')), enabled BOOLEAN NOT NULL DEFAULT true,
 patient_id BIGINT, provider_id BIGINT, reset_token_hash VARCHAR(64), reset_expires_at TIMESTAMPTZ, reset_used_at TIMESTAMPTZ, last_login_at TIMESTAMPTZ
);
CREATE TABLE providers (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(120) NOT NULL, specialty VARCHAR(100) NOT NULL, license_number VARCHAR(50) NOT NULL UNIQUE,
 schedule_text VARCHAR(300) NOT NULL, active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE patients (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 medical_record_number VARCHAR(24) NOT NULL UNIQUE, first_name VARCHAR(80) NOT NULL, last_name VARCHAR(80) NOT NULL,
 date_of_birth DATE NOT NULL, phone VARCHAR(30) NOT NULL, email VARCHAR(160) NOT NULL, address VARCHAR(300) NOT NULL,
 emergency_contact VARCHAR(180) NOT NULL, insurance_provider VARCHAR(120), insurance_member_id VARCHAR(80), assigned_provider_id BIGINT REFERENCES providers(id)
);
ALTER TABLE user_accounts ADD CONSTRAINT fk_user_patient FOREIGN KEY(patient_id) REFERENCES patients(id);
ALTER TABLE user_accounts ADD CONSTRAINT fk_user_provider FOREIGN KEY(provider_id) REFERENCES providers(id);

CREATE TABLE appointment_types (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(80) NOT NULL UNIQUE, duration_minutes INT NOT NULL CHECK(duration_minutes BETWEEN 10 AND 240),
 fee NUMERIC(12,2) NOT NULL CHECK(fee >= 0), currency CHAR(3) NOT NULL, active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE appointments (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 patient_id BIGINT NOT NULL REFERENCES patients(id), provider_id BIGINT NOT NULL REFERENCES providers(id),
 appointment_type VARCHAR(80) NOT NULL, starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('REQUESTED','SCHEDULED','CHECKED_IN','COMPLETED','CANCELLED','NO_SHOW')),
 reason VARCHAR(500) NOT NULL, scheduling_notes VARCHAR(1000), checked_in_at TIMESTAMPTZ,
 CONSTRAINT appointment_time_valid CHECK(ends_at > starts_at)
);
CREATE INDEX idx_appointment_patient ON appointments(patient_id, starts_at DESC);
CREATE INDEX idx_appointment_provider ON appointments(provider_id, starts_at DESC);

CREATE TABLE visit_summaries (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id), patient_id BIGINT NOT NULL REFERENCES patients(id), provider_id BIGINT NOT NULL REFERENCES providers(id),
 diagnosis VARCHAR(1000) NOT NULL, care_instructions VARCHAR(4000) NOT NULL, clinician_notes VARCHAR(6000) NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','RELEASED')), released_at TIMESTAMPTZ
);
CREATE TABLE prescriptions (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 patient_id BIGINT NOT NULL REFERENCES patients(id), provider_id BIGINT NOT NULL REFERENCES providers(id), medication_name VARCHAR(160) NOT NULL,
 dosage VARCHAR(100) NOT NULL, instructions VARCHAR(1000) NOT NULL, status VARCHAR(20) NOT NULL CHECK(status IN ('ACTIVE','ON_HOLD','COMPLETED','CANCELLED')),
 refill_status VARCHAR(20) NOT NULL CHECK(refill_status IN ('NONE','REQUESTED','NEEDS_INFO','APPROVED','DENIED','CANCELLED')),
 refill_note VARCHAR(1000), start_date DATE NOT NULL, end_date DATE
);
CREATE TABLE clinical_documents (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 patient_id BIGINT NOT NULL REFERENCES patients(id), uploaded_by BIGINT NOT NULL REFERENCES user_accounts(id),
 category VARCHAR(30) NOT NULL CHECK(category IN ('PRE_VISIT_FORM','CLINICAL_DOCUMENT','INSURANCE','IDENTITY')),
 status VARCHAR(20) NOT NULL CHECK(status IN ('SUBMITTED','VERIFIED','REJECTED','ARCHIVED')),
 original_name VARCHAR(255) NOT NULL, stored_name VARCHAR(100) NOT NULL UNIQUE, content_type VARCHAR(100) NOT NULL,
 size_bytes BIGINT NOT NULL CHECK(size_bytes >= 0), sha256 CHAR(64) NOT NULL, review_note VARCHAR(1000)
);
CREATE TABLE secure_messages (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 thread_key VARCHAR(36) NOT NULL, patient_id BIGINT NOT NULL REFERENCES patients(id), sender_id BIGINT NOT NULL REFERENCES user_accounts(id),
 subject VARCHAR(180) NOT NULL, body VARCHAR(4000) NOT NULL, visibility VARCHAR(30) NOT NULL CHECK(visibility IN ('PATIENT_VISIBLE','INTERNAL_ONLY')), read_at TIMESTAMPTZ
);
CREATE INDEX idx_message_patient ON secure_messages(patient_id, created_at DESC);
CREATE TABLE audit_events (
 id BIGSERIAL PRIMARY KEY, actor_id BIGINT REFERENCES user_accounts(id), action VARCHAR(100) NOT NULL, resource_type VARCHAR(80) NOT NULL,
 resource_id VARCHAR(80), before_value TEXT, after_value TEXT, authorization_basis VARCHAR(300) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE notifications (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 user_id BIGINT NOT NULL REFERENCES user_accounts(id), title VARCHAR(180) NOT NULL, body VARCHAR(500) NOT NULL, link VARCHAR(120) NOT NULL, read_at TIMESTAMPTZ
);
CREATE TABLE session_identities (token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES user_accounts(id), expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ);
CREATE TABLE outbox_emails (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 recipient VARCHAR(160) NOT NULL, subject VARCHAR(200) NOT NULL, body TEXT NOT NULL, sent_at TIMESTAMPTZ
);
CREATE TABLE webhook_deliveries (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 event_type VARCHAR(100) NOT NULL, payload TEXT NOT NULL, status VARCHAR(20) NOT NULL, attempts INT NOT NULL DEFAULT 0, last_error VARCHAR(500)
);
CREATE TABLE api_keys (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(100) NOT NULL, key_hash CHAR(64) NOT NULL UNIQUE, scope VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT true
);

-- Database guard: audit rows are append-only even if an application bug attempts mutation.
CREATE FUNCTION reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'audit_events are append-only'; END $$;
CREATE TRIGGER audit_no_update BEFORE UPDATE OR DELETE ON audit_events FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
