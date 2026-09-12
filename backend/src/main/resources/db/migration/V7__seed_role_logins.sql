-- ============================================================================
-- V7: One seeded login per role.
--  - Normalises both admin accounts (and any lock state from failed attempts)
--    to the documented shared password 'Admin@123', and gives every seeded
--    account a phone number (required for login OTP delivery; test mode
--    skips real SMS).
--  - Seeds one user for each of the ten non-admin system roles so every
--    user group/type has a working login with the same password.
-- Accounts are for review/demo; rotate to real per-user passwords before
-- genuine production use.
-- ============================================================================

UPDATE users
SET password_hash = crypt('Admin@123', gen_salt('bf', 12)),
    phone = COALESCE(phone, '+263771000001'),
    failed_login_attempts = 0,
    locked_until = NULL,
    is_active = true
WHERE email = 'admin@avmotors.com';

UPDATE users
SET password_hash = crypt('Admin@123', gen_salt('bf', 12)),
    phone = COALESCE(phone, '+263771000002'),
    failed_login_attempts = 0,
    locked_until = NULL,
    is_active = true
WHERE email = 'admin@smbprocurement.com';

INSERT INTO users (id, organisation_id, email, password_hash, first_name, last_name, phone) VALUES
    ('a2000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'manager@avmotors.com',      crypt('Admin@123', gen_salt('bf', 12)), 'AV',  'Manager',     '+263771000101'),
    ('a2000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'estimator@avmotors.com',    crypt('Admin@123', gen_salt('bf', 12)), 'AV',  'Estimator',   '+263771000102'),
    ('a2000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'technician@avmotors.com',   crypt('Admin@123', gen_salt('bf', 12)), 'AV',  'Technician',  '+263771000103'),
    ('a2000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'procurement@avmotors.com',  crypt('Admin@123', gen_salt('bf', 12)), 'AV',  'Procurement', '+263771000104'),
    ('a2000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'accounts@avmotors.com',     crypt('Admin@123', gen_salt('bf', 12)), 'AV',  'Accounts',    '+263771000105'),
    ('b2000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'manager@smbprocurement.com',      crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Manager',    '+263771000201'),
    ('b2000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'quotation@smbprocurement.com',    crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Quotation',  '+263771000202'),
    ('b2000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'purchasing@smbprocurement.com',   crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Purchasing', '+263771000203'),
    ('b2000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'stores@smbprocurement.com',       crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Stores',     '+263771000204'),
    ('b2000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 'accounts@smbprocurement.com',     crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Accounts',   '+263771000205')
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES
    ('a2000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000003'),
    ('a2000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000004'),
    ('a2000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000005'),
    ('a2000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000006'),
    ('b2000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002'),
    ('b2000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000003'),
    ('b2000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004'),
    ('b2000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005'),
    ('b2000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;
