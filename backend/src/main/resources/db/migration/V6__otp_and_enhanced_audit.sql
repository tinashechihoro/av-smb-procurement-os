-- ============================================================================
-- V6: OTP System and Enhanced Audit Trail
--
-- 1. OTP codes table for SMS-based verification
-- 2. Enhanced audit_log with GPS location and user agent details
-- 3. OTP verification tracking in audit trail
-- ============================================================================

-- OTP Codes table
CREATE TABLE IF NOT EXISTS otp_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    purpose VARCHAR(50) NOT NULL CHECK (purpose IN ('LOGIN', 'APPROVAL', 'SIGNATURE', 'PASSWORD_RESET', 'MFA')),
    phone_number VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    failed_attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    is_used BOOLEAN NOT NULL DEFAULT false,
    sms_message_id VARCHAR(200),
    sms_status VARCHAR(50),
    ip_address INET,
    user_agent TEXT,
    session_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_otp_user ON otp_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_purpose ON otp_codes(purpose);
CREATE INDEX IF NOT EXISTS idx_otp_expires ON otp_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_unused ON otp_codes(is_used, verified_at) WHERE is_used = false;

-- Enhanced audit_log columns
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS gps_latitude DOUBLE PRECISION;
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS gps_longitude DOUBLE PRECISION;
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS gps_accuracy DOUBLE PRECISION;
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS location_source VARCHAR(20) CHECK (location_source IN ('GPS', 'IP', 'WIFI', 'MANUAL'));
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS formatted_address TEXT;
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(200);
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS otp_verified BOOLEAN DEFAULT false;
ALTER TABLE audit.audit_log ADD COLUMN IF NOT EXISTS otp_id UUID;

-- Indexes for enhanced audit queries
CREATE INDEX IF NOT EXISTS idx_audit_gps ON audit.audit_log(gps_latitude, gps_longitude) WHERE gps_latitude IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_audit_otp ON audit.audit_log(otp_id) WHERE otp_id IS NOT NULL;

-- Function to clean up expired OTPs (run via scheduled job or manually)
CREATE OR REPLACE FUNCTION audit.cleanup_expired_otps()
RETURNS INT AS $$
DECLARE
    v_count INT;
BEGIN
    DELETE FROM otp_codes 
    WHERE expires_at < now() - INTERVAL '24 hours'
    RETURNING COUNT(*) INTO v_count;
    RETURN v_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
