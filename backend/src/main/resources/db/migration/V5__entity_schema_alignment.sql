-- ============================================================================
-- V5: Align schema with the entity model (quotation versioning, negotiation,
-- clarifications, order amendments, ledgers, stock movements, job history)
--
-- These entities exist in the application but their tables were never
-- migrated, which crashed quotation creation (quotation_items.version_id).
-- DDL mirrors the Hibernate entity mappings.
-- ============================================================================

ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS version_id UUID;

CREATE TABLE IF NOT EXISTS clarification_threads (
    id UUID PRIMARY KEY,
    requisition_id UUID NOT NULL REFERENCES requisitions(id),
    raised_by UUID NOT NULL,
    subject VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS clarification_messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL REFERENCES clarification_threads(id),
    sender_id UUID NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS creditor_ledger (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    supplier_po_id UUID REFERENCES supplier_purchase_orders(id),
    payment_id UUID,
    entry_date DATE NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    balance_after NUMERIC(18,2),
    narration TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS debtor_ledger (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_id UUID,
    payment_id UUID,
    entry_date DATE NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    balance_after NUMERIC(18,2),
    narration TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS negotiation_messages (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL REFERENCES quotations(id),
    sender_id UUID NOT NULL,
    is_av_message BOOLEAN NOT NULL,
    proposed_amount NUMERIC(14,2),
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS order_amendments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES av_orders(id),
    amendment_number INTEGER NOT NULL,
    requested_by UUID NOT NULL,
    approved_by UUID,
    status VARCHAR(20) NOT NULL,
    reason TEXT NOT NULL,
    amount_change NUMERIC(14,2),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS quotation_versions (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL REFERENCES quotations(id),
    version_number INTEGER NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,
    tax_amount NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    change_summary TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    UNIQUE (quotation_id, version_number)
);

CREATE TABLE IF NOT EXISTS repair_stage_history (
    id UUID PRIMARY KEY,
    repair_job_id UUID NOT NULL REFERENCES repair_jobs(id),
    changed_by UUID NOT NULL,
    from_stage VARCHAR(30),
    to_stage VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL,
    from_location_id UUID,
    to_location_id UUID,
    reference_type VARCHAR(30),
    reference_id UUID,
    performed_by UUID,
    notes TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    UNIQUE (organisation_id, code)
);

CREATE INDEX IF NOT EXISTS idx_creditor_org_supplier ON creditor_ledger(organisation_id, supplier_id);
CREATE INDEX IF NOT EXISTS idx_debtor_org_customer ON debtor_ledger(organisation_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_item ON stock_movements(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_quotation_versions_quote ON quotation_versions(quotation_id);

-- Audit the new org-scoped and referenced tables
DO $$
DECLARE t TEXT;
BEGIN
    FOR t IN SELECT unnest(ARRAY['clarification_threads','clarification_messages','creditor_ledger',
        'debtor_ledger','negotiation_messages','order_amendments','quotation_versions',
        'repair_stage_history','stock_movements'])
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_audit_%s ON %I', t, t);
        EXECUTE format('CREATE TRIGGER trg_audit_%s AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_trigger()', t, t);
    END LOOP;
END $$;
