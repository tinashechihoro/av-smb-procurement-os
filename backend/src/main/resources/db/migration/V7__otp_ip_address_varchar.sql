-- otp_codes.ip_address was created as INET (V6) while the OtpCode entity maps
-- it to a String. Hibernate binds a String as varchar, and PostgreSQL will not
-- coerce that to inet, so every OTP insert aborted the transaction:
--
--   ERROR: column "ip_address" is of type inet but expression is of type
--          character varying
--
-- which surfaced as HTTP 500 on /auth/login for any account with MFA enabled.
--
-- varchar(45) holds any IPv4 or IPv6 literal (45 = longest IPv4-mapped IPv6
-- form) and matches how the column is actually read and written. host() strips
-- any netmask from existing values; the table is only written by the OTP flow,
-- which has never successfully inserted a row.
ALTER TABLE otp_codes
    ALTER COLUMN ip_address TYPE VARCHAR(45)
    USING host(ip_address);
