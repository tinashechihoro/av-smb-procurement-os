-- ============================================================================
-- V8: otp_codes.ip_address was created as INET (V6) while the JPA entity maps
-- it as String, so every OTP insert failed with
-- "column ip_address is of type inet but expression is of type character
-- varying" and login could never complete. Align the column to text.
-- ============================================================================

ALTER TABLE otp_codes ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::TEXT;
