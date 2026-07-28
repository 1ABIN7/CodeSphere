-- ============================================================================
-- V9: Proctoring Module Extensions
-- ============================================================================
-- Extends the existing V3 proctoring_events table with screenshot storage
-- and adds a per-assessment proctoring configuration table.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Extend proctoring_events with screenshot and metadata fields
-- ---------------------------------------------------------------------------
ALTER TABLE proctoring_events ADD COLUMN IF NOT EXISTS screenshot_url VARCHAR(1000);
ALTER TABLE proctoring_events ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;
ALTER TABLE proctoring_events ADD COLUMN IF NOT EXISTS resolved BOOLEAN DEFAULT FALSE;
ALTER TABLE proctoring_events ADD COLUMN IF NOT EXISTS resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE proctoring_events ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE proctoring_events ADD COLUMN IF NOT EXISTS notes TEXT;

-- ---------------------------------------------------------------------------
-- 2. Proctoring configuration per assessment
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS proctoring_config (
    id                          BIGSERIAL PRIMARY KEY,
    assessment_id               BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    enable_webcam               BOOLEAN DEFAULT TRUE,
    enable_screen_recording     BOOLEAN DEFAULT FALSE,
    enable_tab_switch_detection BOOLEAN DEFAULT TRUE,
    enable_face_detection       BOOLEAN DEFAULT FALSE,
    enable_audio_detection      BOOLEAN DEFAULT FALSE,
    max_allowed_violations      INT DEFAULT 5,
    warning_threshold           INT DEFAULT 3,
    auto_disqualify             BOOLEAN DEFAULT FALSE,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (assessment_id)
);

-- ---------------------------------------------------------------------------
-- 3. Proctoring anomaly score tracking per session
-- ---------------------------------------------------------------------------
ALTER TABLE assessment_sessions ADD COLUMN IF NOT EXISTS violation_count INT DEFAULT 0;
ALTER TABLE assessment_sessions ADD COLUMN IF NOT EXISTS is_flagged BOOLEAN DEFAULT FALSE;
ALTER TABLE assessment_sessions ADD COLUMN IF NOT EXISTS flag_reason TEXT;
