-- ============================================================================
-- V2: Production hardening
--  1. Rotate seeded admin passwords through a Flyway placeholder so each
--     environment controls the initial admin credential via SEED_ADMIN_PASSWORD.
--     Default keeps the documented development password for local setups.
--  2. Add read permissions used by the permission-enforced list endpoints.
-- ============================================================================

UPDATE users
SET password_hash = crypt('${seedAdminPassword}', gen_salt('bf', 12))
WHERE email IN ('admin@avmotors.com', 'admin@smbprocurement.com');

INSERT INTO permissions (code, name, module) VALUES
    ('payments.view', 'View Payments', 'FINANCE'),
    ('supplier_po.view', 'View Supplier POs', 'PURCHASING')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('payments.view', 'supplier_po.view')
WHERE r.code IN (
    'AV_SYS_ADMIN', 'AV_MANAGER', 'AV_ACCOUNTS',
    'SMB_ADMIN', 'SMB_MANAGER', 'SMB_ACCOUNTS', 'SMB_PURCHASING'
)
ON CONFLICT DO NOTHING;
