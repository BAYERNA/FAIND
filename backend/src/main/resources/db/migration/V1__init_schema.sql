-- FAIND initial schema
-- Source: FAIND DB 설계서 v2.2 (12 tables: FIRO 11 + drone_dispatches)
-- Note: users/devices/incidents/... are owned by the Java monolith (backend/).
--       alerts / alert_acknowledgements are owned by notification-server (Node),
--       but are created here too because the demo runs a single shared PostgreSQL
--       instance (see MSA 아키텍처설계서 §6 및 인프라 다이어그램 "서비스별 스키마 분리").

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 3.1 users
CREATE TABLE users (
    user_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(50)  NOT NULL,
    role                 VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'COMMANDER', 'RESPONDER')),
    badge_number         VARCHAR(20)  NOT NULL UNIQUE,
    team                 VARCHAR(50),
    phone                VARCHAR(20),
    password_hash        VARCHAR(255) NOT NULL,
    is_initial_password  BOOLEAN      NOT NULL DEFAULT true,
    status               VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at           TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP
);

-- 3.2 devices (기기 · CCTV · 드론)
CREATE TABLE devices (
    device_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_type       VARCHAR(30) NOT NULL, -- BODYCAM/SMARTPHONE/DIGITAL_MASK/SENSOR/CCTV/DRONE
    serial_no         VARCHAR(50) NOT NULL UNIQUE,
    connection_type   VARCHAR(20), -- BLE/LTE_5G/WIFI
    current_user_id   UUID REFERENCES users(user_id),
    latitude          DECIMAL(9, 6),
    longitude         DECIMAL(9, 6),
    status            VARCHAR(15) NOT NULL DEFAULT 'NORMAL', -- NORMAL/WARNING/DISCONNECTED
    battery_level     INT,
    registered_at     TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP
);

CREATE INDEX idx_devices_current_user_id ON devices (current_user_id);

-- 3.3 incidents (출동)
-- v2.2: status has NO default (fail-closed) — every INSERT must state it explicitly.
CREATE TABLE incidents (
    incident_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_number  VARCHAR(30) NOT NULL UNIQUE,
    incident_type    VARCHAR(20) NOT NULL, -- FIRE/RESCUE/EMERGENCY
    address          TEXT,
    latitude         DECIMAL(9, 6),
    longitude        DECIMAL(9, 6),
    reported_at      TIMESTAMP   NOT NULL,
    closed_at        TIMESTAMP,
    status           VARCHAR(15) NOT NULL
        CHECK (status IN ('AI_SUSPECTED', 'DISPATCHED', 'IN_PROGRESS', 'CLOSED')),
    source           VARCHAR(20) NOT NULL DEFAULT 'MANUAL_REPORT'
        CHECK (source IN ('MANUAL_REPORT', 'CCTV_AUTO_DETECTION')),
    confirmed_by     UUID REFERENCES users(user_id),
    commander_id     UUID REFERENCES users(user_id),
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),
    -- NFR-08 fail-closed guard at the DB layer: an AI_SUSPECTED incident may
    -- never carry a confirmer, and anything beyond AI_SUSPECTED must.
    CONSTRAINT chk_incidents_confirmed_by_gate CHECK (
        (status = 'AI_SUSPECTED' AND confirmed_by IS NULL)
        OR (status <> 'AI_SUSPECTED' AND (source = 'MANUAL_REPORT' OR confirmed_by IS NOT NULL))
    )
);

CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_commander_id ON incidents (commander_id);

-- 3.4 pre_analysis_results
CREATE TABLE pre_analysis_results (
    result_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id       UUID NOT NULL UNIQUE REFERENCES incidents(incident_id),
    building_info     JSONB,
    hazard_info       JSONB,
    fire_history_info JSONB,
    data_source       VARCHAR(50) DEFAULT '소방청 공공데이터 API',
    analyzed_at       TIMESTAMP   NOT NULL DEFAULT now()
);

-- 3.5 incident_assignments
CREATE TABLE incident_assignments (
    assignment_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id       UUID NOT NULL REFERENCES incidents(incident_id),
    user_id           UUID NOT NULL REFERENCES users(user_id),
    role_in_incident  VARCHAR(30),
    is_first_wave     BOOLEAN NOT NULL DEFAULT false,
    is_comms_lead     BOOLEAN NOT NULL DEFAULT false,
    assigned_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_incident_assignments_incident_user UNIQUE (incident_id, user_id)
);

CREATE UNIQUE INDEX uq_incident_assignments_comms_lead
    ON incident_assignments (incident_id)
    WHERE is_comms_lead = true;

-- 3.6 responder_status_logs
CREATE TABLE responder_status_logs (
    log_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id         UUID NOT NULL REFERENCES incidents(incident_id),
    user_id             UUID NOT NULL REFERENCES users(user_id),
    recorded_at         TIMESTAMP NOT NULL,
    biometric_data      JSONB,
    environment_data    JSONB,
    risk_level          VARCHAR(10), -- NORMAL/CAUTION/DANGER
    connection_status   VARCHAR(15)  -- CONNECTED/MESH/SMS/DISCONNECTED
);

CREATE INDEX idx_responder_status_logs_incident_recorded
    ON responder_status_logs (incident_id, recorded_at);

-- 3.7 alerts (Node notification-server 소유 — 공유 인스턴스라 함께 생성)
CREATE TABLE alerts (
    alert_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id       UUID NOT NULL REFERENCES incidents(incident_id),
    target_user_id    UUID REFERENCES users(user_id),
    author_id         UUID REFERENCES users(user_id),
    alert_type        VARCHAR(20), -- RISK_WARNING/EVACUATION/STATUS_CHANGE/ENTRY_INFO/SUPPLY_REQUEST
    info_category     VARCHAR(20), -- ENTRY/HAZARD
    location_label    VARCHAR(50),
    status_tag        VARCHAR(20), -- PASSABLE/BLOCKED/DANGER
    requested_items   JSONB,
    source_type       VARCHAR(20) NOT NULL DEFAULT 'HUMAN', -- HUMAN/SENSOR/AI/EXTERNAL
    channel           VARCHAR(10), -- VOICE/TEXT
    message           TEXT,
    sent_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_incident_id ON alerts (incident_id);

-- 3.8 alert_acknowledgements (Node notification-server 소유)
CREATE TABLE alert_acknowledgements (
    ack_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id          UUID NOT NULL REFERENCES alerts(alert_id),
    user_id           UUID NOT NULL REFERENCES users(user_id),
    acknowledged_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_alert_acknowledgements_alert_id ON alert_acknowledgements (alert_id);

-- 3.9 reports
CREATE TABLE reports (
    report_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id    UUID NOT NULL REFERENCES incidents(incident_id),
    author_id      UUID NOT NULL REFERENCES users(user_id),
    content        TEXT,
    video_ref      VARCHAR(255),
    status         VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED')),
    submitted_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP
);

CREATE INDEX idx_reports_incident_id ON reports (incident_id);
CREATE INDEX idx_reports_author_id ON reports (author_id);

-- 3.10 report_analyses
CREATE TABLE report_analyses (
    analysis_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id          UUID NOT NULL UNIQUE REFERENCES reports(report_id),
    sop_match_result    JSONB,
    risk_pattern        TEXT,
    recommendation      TEXT,
    pdf_url             VARCHAR(255),
    review_status       VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (review_status IN ('PENDING', 'REVIEWED')),
    analyzed_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- 3.11 ai_judgment_logs
CREATE TABLE ai_judgment_logs (
    judgment_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    judgment_type          VARCHAR(30) NOT NULL, -- PRE_ANALYSIS/REPORT_SOP_MATCH/RISK_DETECTION/CCTV_DETECTION/DRONE_RECON
    related_incident_id    UUID REFERENCES incidents(incident_id),
    related_report_id      UUID REFERENCES reports(report_id),
    source_device_id       UUID REFERENCES devices(device_id),
    confidence_score       DECIMAL(5, 2),
    summary                TEXT,
    created_at             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_judgment_logs_type_created ON ai_judgment_logs (judgment_type, created_at);
CREATE INDEX idx_ai_judgment_logs_related_incident ON ai_judgment_logs (related_incident_id);

-- 3.12 drone_dispatches (신규, FR-25/26)
CREATE TABLE drone_dispatches (
    dispatch_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id     UUID NOT NULL REFERENCES incidents(incident_id),
    drone_id        UUID NOT NULL REFERENCES devices(device_id),
    dispatched_at   TIMESTAMP NOT NULL DEFAULT now(),
    arrived_at      TIMESTAMP,
    status          VARCHAR(15) NOT NULL DEFAULT 'EN_ROUTE' CHECK (status IN ('EN_ROUTE', 'ON_SITE', 'RETURNED')),
    video_ref       VARCHAR(255)
);

CREATE INDEX idx_drone_dispatches_incident_id ON drone_dispatches (incident_id);
