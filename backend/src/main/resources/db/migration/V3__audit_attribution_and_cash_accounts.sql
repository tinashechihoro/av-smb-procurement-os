-- ============================================================================
-- V3: Fix audit attribution and seed cash accounts
--
-- 1. fn_audit_trigger previously required the app.current_org_id session
--    variable and silently recorded nothing when it was unset. The rewritten
--    function derives the organisation from the row itself (every org-scoped
--    table carries organisation_id), falling back to the session variable for
--    tables that don't. Rows without any organisation are skipped (global
--    reference data). The acting user comes from app.current_user_id when set.
-- 2. Seed one bank and one cash account per organisation so payments and
--    cashbook entries work on a fresh deployment.
-- ============================================================================

CREATE OR REPLACE FUNCTION audit.fn_audit_trigger()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
    v_org_id UUID;
    v_action TEXT;
    v_row JSONB;
BEGIN
    v_row := CASE WHEN TG_OP = 'DELETE' THEN to_jsonb(OLD) ELSE to_jsonb(NEW) END;

    v_org_id := NULLIF(v_row ->> 'organisation_id', '');
    IF v_org_id IS NULL THEN
        v_org_id := NULLIF(current_setting('app.current_org_id', true), '');
    END IF;

    -- Global tables (no organisation scope) are not audited.
    IF v_org_id IS NULL THEN
        IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
        RETURN NEW;
    END IF;
    v_org_id := v_org_id::uuid;

    v_user_id := NULLIF(current_setting('app.current_user_id', true), '');
    IF v_user_id IS NOT NULL THEN
        v_user_id := v_user_id::uuid;
    END IF;

    IF TG_OP = 'INSERT' THEN
        v_action := LOWER(TG_TABLE_NAME) || '.created';
        INSERT INTO audit.audit_log(organisation_id, user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (v_org_id, v_user_id, v_action, TG_TABLE_NAME, NEW.id, NULL, to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        v_action := LOWER(TG_TABLE_NAME) || '.updated';
        INSERT INTO audit.audit_log(organisation_id, user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (v_org_id, v_user_id, v_action, TG_TABLE_NAME, NEW.id, to_jsonb(OLD), to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        v_action := LOWER(TG_TABLE_NAME) || '.deleted';
        INSERT INTO audit.audit_log(organisation_id, user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (v_org_id, v_user_id, v_action, TG_TABLE_NAME, OLD.id, to_jsonb(OLD), NULL);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Cash accounts: one bank + one cash per organisation, linked to GL 1100/1110
INSERT INTO cash_accounts (organisation_id, account_name, account_type, account_number, bank_name, currency)
SELECT o.id, v.name, v.type, v.number, v.bank, 'USD'
FROM organisations o
CROSS JOIN (VALUES
    ('Main Bank Account', 'BANK', '1001234567890', 'ZB Bank'),
    ('Workshop Cash Float', 'CASH', NULL, NULL)
) AS v(name, type, number, bank)
WHERE o.code IN ('AV', 'SMB')
  AND NOT EXISTS (
      SELECT 1 FROM cash_accounts ca
      WHERE ca.organisation_id = o.id AND ca.account_name = v.name
  );
