-- ============================================================================
-- V1: Initial Schema — AV Motors x SMB Procurement OS
-- Flyway migration baseline
-- ============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- ============================================================================
-- AUDIT SCHEMA
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.audit_log (
    id              BIGSERIAL PRIMARY KEY,
    organisation_id UUID NOT NULL,
    user_id         UUID,
    action          VARCHAR(120) NOT NULL,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       UUID,
    before_data     JSONB,
    after_data      JSONB,
    ip_address      INET,
    user_agent      TEXT,
    session_id      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_org ON audit.audit_log(organisation_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit.audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit.audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit.audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit.audit_log(created_at DESC);

-- Audit trigger function
CREATE OR REPLACE FUNCTION audit.fn_audit_trigger()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
    v_org_id UUID;
    v_action TEXT;
    v_setting TEXT;
BEGIN
    v_setting := current_setting('app.current_org_id', true);
    IF v_setting IS NULL OR v_setting = '' THEN
        IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
        RETURN NEW;
    END IF;
    v_org_id := v_setting::uuid;
    v_setting := current_setting('app.current_user_id', true);
    IF v_setting IS NOT NULL AND v_setting <> '' THEN v_user_id := v_setting::uuid; END IF;

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

CREATE OR REPLACE FUNCTION audit.attach_audit(p_table TEXT)
RETURNS void AS $$
BEGIN
    EXECUTE format('CREATE TRIGGER trg_audit_%s AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_trigger()', p_table, p_table);
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- CORE TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS organisations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), name VARCHAR(200) NOT NULL, code VARCHAR(20) NOT NULL UNIQUE,
    org_type VARCHAR(20) NOT NULL CHECK (org_type IN ('BUYER','SUPPLIER')), currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    fiscal_year_start DATE, is_active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30), avatar_url TEXT, is_active BOOLEAN NOT NULL DEFAULT true, mfa_enabled BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INT NOT NULL DEFAULT 0, locked_until TIMESTAMPTZ, last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_users_org ON users(organisation_id);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    name VARCHAR(100) NOT NULL, code VARCHAR(50) NOT NULL, description TEXT, is_system BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(organisation_id, code)
);

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), code VARCHAR(100) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL,
    module VARCHAR(50) NOT NULL, description TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS role_permissions (role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE, permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE, PRIMARY KEY (role_id, permission_id));
CREATE TABLE IF NOT EXISTS user_roles (user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE, PRIMARY KEY (user_id, role_id));

CREATE TABLE IF NOT EXISTS approval_limits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), user_id UUID NOT NULL REFERENCES users(id), approval_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD', single_limit NUMERIC(18,2) NOT NULL DEFAULT 0, cumulative_limit NUMERIC(18,2),
    requires_second_approval BOOLEAN NOT NULL DEFAULT false, is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- WORKSHOP TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    name VARCHAR(200) NOT NULL, email VARCHAR(255), phone VARCHAR(30), address TEXT, tax_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS insurers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    name VARCHAR(200) NOT NULL, contact_person VARCHAR(200), email VARCHAR(255), phone VARCHAR(30), policy_prefix VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    customer_id UUID REFERENCES customers(id), insurer_id UUID REFERENCES insurers(id),
    registration VARCHAR(30) NOT NULL, vin VARCHAR(30), make VARCHAR(100) NOT NULL, model VARCHAR(100) NOT NULL,
    year INTEGER, color VARCHAR(50), engine_number VARCHAR(50), mileage INTEGER, fuel_type VARCHAR(20), vehicle_type VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'IN_WORKSHOP', notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_vehicles_org ON vehicles(organisation_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_reg ON vehicles(registration);

CREATE TABLE IF NOT EXISTS repair_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id), job_number VARCHAR(30) NOT NULL UNIQUE, title VARCHAR(300) NOT NULL, description TEXT,
    job_type VARCHAR(30) NOT NULL DEFAULT 'COLLISION', status VARCHAR(30) NOT NULL DEFAULT 'ASSESSMENT', repair_stage VARCHAR(30) NOT NULL DEFAULT 'INITIAL_ASSESSMENT',
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL', assigned_technician VARCHAR(200), bay_number VARCHAR(20),
    booked_hours NUMERIC(8,2), actual_hours NUMERIC(8,2), labour_rate NUMERIC(10,2),
    insurer_claim_number VARCHAR(80), insurer_authorised BOOLEAN NOT NULL DEFAULT false,
    excess_amount NUMERIC(12,2), supplement_amount NUMERIC(12,2), estimated_total NUMERIC(14,2) DEFAULT 0, actual_total NUMERIC(14,2) DEFAULT 0,
    started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, handed_over_at TIMESTAMPTZ, notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_org ON repair_jobs(organisation_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_status ON repair_jobs(status);

CREATE TABLE IF NOT EXISTS damage_assessments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), repair_job_id UUID NOT NULL REFERENCES repair_jobs(id), assessed_by UUID NOT NULL REFERENCES users(id),
    zone VARCHAR(50) NOT NULL, severity VARCHAR(10) NOT NULL, description TEXT, parts_affected JSONB, repair_method VARCHAR(100),
    estimated_cost NUMERIC(12,2), assessed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS repair_operations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), repair_job_id UUID NOT NULL REFERENCES repair_jobs(id),
    operation_name VARCHAR(200) NOT NULL, operation_type VARCHAR(50) NOT NULL, assigned_to VARCHAR(200),
    booked_hours NUMERIC(8,2), actual_hours NUMERIC(8,2), sequence_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', notes TEXT, started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS quality_checks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), repair_job_id UUID NOT NULL REFERENCES repair_jobs(id), operation_id UUID REFERENCES repair_operations(id),
    checked_by UUID NOT NULL REFERENCES users(id), check_type VARCHAR(50) NOT NULL, passed BOOLEAN NOT NULL DEFAULT false,
    comments TEXT, checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS job_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), repair_job_id UUID NOT NULL REFERENCES repair_jobs(id),
    document_type VARCHAR(30) NOT NULL, file_name VARCHAR(300) NOT NULL, file_path TEXT NOT NULL,
    file_size BIGINT, mime_type VARCHAR(100), uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- PROCUREMENT TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS requisitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    repair_job_id UUID REFERENCES repair_jobs(id), requisition_number VARCHAR(30) NOT NULL UNIQUE, title VARCHAR(300) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    requested_by UUID NOT NULL REFERENCES users(id), total_estimate NUMERIC(14,2) DEFAULT 0, amendment_number INTEGER NOT NULL DEFAULT 0,
    parent_requisition_id UUID REFERENCES requisitions(id), notes TEXT, submitted_at TIMESTAMPTZ, approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_requisitions_org ON requisitions(organisation_id);

CREATE TABLE IF NOT EXISTS requisition_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), requisition_id UUID NOT NULL REFERENCES requisitions(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL, part_number VARCHAR(80), description VARCHAR(500) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL DEFAULT 1, unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'EA',
    estimated_cost NUMERIC(12,2), notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS quotations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    requisition_id UUID NOT NULL REFERENCES requisitions(id), quotation_number VARCHAR(30) NOT NULL UNIQUE, title VARCHAR(300),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', current_version INTEGER NOT NULL DEFAULT 1,
    subtotal NUMERIC(14,2) DEFAULT 0, tax_rate NUMERIC(5,2) DEFAULT 0, tax_amount NUMERIC(14,2) DEFAULT 0, total_amount NUMERIC(14,2) DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD', valid_until DATE, prepared_by UUID REFERENCES users(id), notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_quotations_org ON quotations(organisation_id);

CREATE TABLE IF NOT EXISTS quotation_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), quotation_id UUID NOT NULL REFERENCES quotations(id),
    requisition_item_id UUID REFERENCES requisition_items(id), line_number INTEGER NOT NULL, part_number VARCHAR(80),
    description VARCHAR(500) NOT NULL, quantity NUMERIC(12,3) NOT NULL DEFAULT 1, unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_pct NUMERIC(5,2) DEFAULT 0, line_total NUMERIC(14,2) NOT NULL DEFAULT 0, notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS approvals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    entity_type VARCHAR(50) NOT NULL, entity_id UUID NOT NULL, approval_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', requested_by UUID NOT NULL REFERENCES users(id), approved_by UUID REFERENCES users(id),
    amount NUMERIC(14,2), comments TEXT, approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS av_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    quotation_id UUID REFERENCES quotations(id), repair_job_id UUID REFERENCES repair_jobs(id),
    order_number VARCHAR(30) NOT NULL UNIQUE, status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal NUMERIC(14,2) DEFAULT 0, tax_amount NUMERIC(14,2) DEFAULT 0, total_amount NUMERIC(14,2) DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD', expected_delivery DATE, notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_av_orders_org ON av_orders(organisation_id);

CREATE TABLE IF NOT EXISTS av_order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), order_id UUID NOT NULL REFERENCES av_orders(id),
    quotation_item_id UUID REFERENCES quotation_items(id), line_number INTEGER NOT NULL, part_number VARCHAR(80),
    description VARCHAR(500) NOT NULL, quantity NUMERIC(12,3) NOT NULL, unit_price NUMERIC(12,2) NOT NULL,
    line_total NUMERIC(14,2) NOT NULL, delivered_qty NUMERIC(12,3) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- SMB PURCHASING TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    name VARCHAR(300) NOT NULL, contact_person VARCHAR(200), email VARCHAR(255), phone VARCHAR(30), address TEXT,
    tax_number VARCHAR(50), payment_terms INTEGER DEFAULT 30, currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_active BOOLEAN NOT NULL DEFAULT true, rating NUMERIC(3,2), notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS supplier_purchase_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id), av_order_id UUID REFERENCES av_orders(id),
    po_number VARCHAR(30) NOT NULL UNIQUE, status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal NUMERIC(14,2) DEFAULT 0, freight_total NUMERIC(12,2) DEFAULT 0, total_amount NUMERIC(14,2) DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD', expected_delivery DATE, notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS supplier_po_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), po_id UUID NOT NULL REFERENCES supplier_purchase_orders(id),
    av_order_item_id UUID REFERENCES av_order_items(id), line_number INTEGER NOT NULL, description VARCHAR(500) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL, unit_cost NUMERIC(12,2) NOT NULL, freight_alloc NUMERIC(12,2) DEFAULT 0,
    line_total NUMERIC(14,2) NOT NULL, received_qty NUMERIC(12,3) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- INVENTORY TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    name VARCHAR(200) NOT NULL, code VARCHAR(30) NOT NULL, location_type VARCHAR(20) NOT NULL DEFAULT 'BIN',
    is_active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, code)
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    location_id UUID REFERENCES locations(id), part_number VARCHAR(80) NOT NULL, description VARCHAR(500) NOT NULL,
    quantity_on_hand NUMERIC(12,3) NOT NULL DEFAULT 0, quantity_reserved NUMERIC(12,3) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(12,3) DEFAULT 0, unit_cost NUMERIC(12,2), selling_price NUMERIC(12,2),
    category VARCHAR(50), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_inventory_org ON inventory_items(organisation_id);
CREATE INDEX IF NOT EXISTS idx_inventory_part ON inventory_items(part_number);

CREATE TABLE IF NOT EXISTS goods_receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    supplier_po_id UUID REFERENCES supplier_purchase_orders(id), av_order_id UUID REFERENCES av_orders(id),
    receipt_number VARCHAR(30) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    received_by UUID NOT NULL REFERENCES users(id), received_at TIMESTAMPTZ NOT NULL DEFAULT now(), notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS delivery_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    av_order_id UUID NOT NULL REFERENCES av_orders(id), delivery_number VARCHAR(30) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARED', delivery_date DATE, delivered_by VARCHAR(200), vehicle_reg VARCHAR(30), notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- FINANCE TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    account_code VARCHAR(20) NOT NULL, account_name VARCHAR(200) NOT NULL, account_type VARCHAR(20) NOT NULL,
    sub_type VARCHAR(50), parent_id UUID REFERENCES chart_of_accounts(id),
    is_postable BOOLEAN NOT NULL DEFAULT true, is_active BOOLEAN NOT NULL DEFAULT true,
    normal_balance VARCHAR(10) NOT NULL, opening_balance NUMERIC(18,2) NOT NULL DEFAULT 0, current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, account_code)
);

CREATE TABLE IF NOT EXISTS cash_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    account_name VARCHAR(200) NOT NULL, account_type VARCHAR(20) NOT NULL, account_number VARCHAR(50),
    bank_name VARCHAR(200), branch_name VARCHAR(200), currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    gl_account_id UUID REFERENCES chart_of_accounts(id), balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS journals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    journal_number VARCHAR(30) NOT NULL UNIQUE, journal_type VARCHAR(30) NOT NULL, description TEXT,
    source_type VARCHAR(50), source_id UUID, total_debit NUMERIC(18,2) NOT NULL DEFAULT 0, total_credit NUMERIC(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', posted_by UUID REFERENCES users(id), posted_at TIMESTAMPTZ,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS journal_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), journal_id UUID NOT NULL REFERENCES journals(id),
    line_number INTEGER NOT NULL, account_id UUID NOT NULL REFERENCES chart_of_accounts(id),
    description VARCHAR(500), debit_amount NUMERIC(18,2) NOT NULL DEFAULT 0, credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    av_order_id UUID REFERENCES av_orders(id), repair_job_id UUID REFERENCES repair_jobs(id),
    invoice_number VARCHAR(30) NOT NULL UNIQUE, invoice_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', invoice_date DATE NOT NULL DEFAULT CURRENT_DATE, due_date DATE,
    from_entity VARCHAR(200) NOT NULL, to_entity VARCHAR(200) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0, tax_rate NUMERIC(5,2) DEFAULT 0, tax_amount NUMERIC(14,2) DEFAULT 0,
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0, amount_paid NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD', notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_invoices_org ON invoices(organisation_id);

CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), invoice_id UUID NOT NULL REFERENCES invoices(id),
    order_item_id UUID REFERENCES av_order_items(id), line_number INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL, quantity NUMERIC(12,3) NOT NULL, unit_price NUMERIC(12,2) NOT NULL, line_total NUMERIC(14,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    invoice_id UUID REFERENCES invoices(id), payment_number VARCHAR(30) NOT NULL UNIQUE,
    payment_type VARCHAR(20) NOT NULL, amount NUMERIC(14,2) NOT NULL, currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method VARCHAR(30) NOT NULL, reference VARCHAR(100), cash_account_id UUID REFERENCES cash_accounts(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', received_by UUID REFERENCES users(id), payment_date DATE NOT NULL DEFAULT CURRENT_DATE, notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cashbook_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    cash_account_id UUID NOT NULL REFERENCES cash_accounts(id), journal_id UUID REFERENCES journals(id),
    entry_type VARCHAR(10) NOT NULL, amount NUMERIC(18,2) NOT NULL, currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    counterparty VARCHAR(200), narration TEXT, reference VARCHAR(100), source_type VARCHAR(50), source_id UUID,
    running_balance NUMERIC(18,2), reconciliation_status VARCHAR(20) NOT NULL DEFAULT 'UNRECONCILED',
    reconciled_by UUID REFERENCES users(id), reconciled_at TIMESTAMPTZ, entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- PLATFORM TABLES
-- ============================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    user_id UUID NOT NULL REFERENCES users(id), title VARCHAR(300) NOT NULL, message TEXT,
    notification_type VARCHAR(30) NOT NULL, entity_type VARCHAR(50), entity_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);

CREATE TABLE IF NOT EXISTS document_sequences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    document_type VARCHAR(50) NOT NULL, prefix VARCHAR(10) NOT NULL, current_number BIGINT NOT NULL DEFAULT 0,
    padding INTEGER NOT NULL DEFAULT 5, updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, document_type)
);

-- ============================================================================
-- FILE UPLOADS TABLE (Phase 5)
-- ============================================================================
CREATE TABLE IF NOT EXISTS uploaded_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    original_name VARCHAR(300) NOT NULL, stored_name VARCHAR(300) NOT NULL, storage_path TEXT NOT NULL,
    file_size BIGINT NOT NULL, mime_type VARCHAR(100), checksum VARCHAR(64),
    entity_type VARCHAR(50), entity_id UUID,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_files_entity ON uploaded_files(entity_type, entity_id);

-- ============================================================================
-- ACCOUNTING PERIODS TABLE (Phase 1 - financial controls)
-- ============================================================================
CREATE TABLE IF NOT EXISTS accounting_periods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), organisation_id UUID NOT NULL REFERENCES organisations(id),
    period_name VARCHAR(50) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED','LOCKED')),
    closed_by UUID REFERENCES users(id), closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, start_date, end_date)
);

-- ============================================================================
-- ATTACH AUDIT TRIGGERS
-- ============================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOR t IN SELECT unnest(ARRAY['organisations','users','roles','permissions','customers','insurers','vehicles','repair_jobs',
        'damage_assessments','repair_operations','quality_checks','requisitions','requisition_items','quotations','quotation_items',
        'approvals','av_orders','av_order_items','suppliers','supplier_purchase_orders','supplier_po_items','inventory_items',
        'goods_receipts','delivery_notes','invoices','invoice_items','payments','cashbook_entries','journals','journal_lines',
        'chart_of_accounts','notifications','uploaded_files','accounting_periods'])
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_audit_%s ON %I', t, t);
        EXECUTE format('CREATE TRIGGER trg_audit_%s AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_trigger()', t, t);
    END LOOP;
END $$;

-- ============================================================================
-- SEED DATA
-- ============================================================================
INSERT INTO organisations (id, name, code, org_type, currency) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'AV Motors', 'AV', 'BUYER', 'USD'),
    ('b0000000-0000-0000-0000-000000000001', 'SMB Procurement', 'SMB', 'SUPPLIER', 'USD')
ON CONFLICT DO NOTHING;

INSERT INTO roles (id, organisation_id, name, code, is_system) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'System Administrator', 'AV_SYS_ADMIN', true),
    ('a1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Manager / Approver', 'AV_MANAGER', true),
    ('a1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Estimator', 'AV_ESTIMATOR', true),
    ('a1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Workshop Technician', 'AV_TECHNICIAN', true),
    ('a1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Procurement Officer', 'AV_PROCUREMENT', true),
    ('a1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'Accounts Officer', 'AV_ACCOUNTS', true),
    ('b1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'SMB Administrator', 'SMB_ADMIN', true),
    ('b1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'SMB Manager', 'SMB_MANAGER', true),
    ('b1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'Quotation Officer', 'SMB_QUOTATION', true),
    ('b1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'Purchasing Officer', 'SMB_PURCHASING', true),
    ('b1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'Stores Officer', 'SMB_STORES', true),
    ('b1000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 'Sales & Accounts', 'SMB_ACCOUNTS', true)
ON CONFLICT DO NOTHING;

INSERT INTO permissions (code, name, module) VALUES
    ('vehicles.view','View Vehicles','WORKSHOP'),('vehicles.manage','Manage Vehicles','WORKSHOP'),
    ('jobs.view','View Repair Jobs','WORKSHOP'),('jobs.manage','Manage Repair Jobs','WORKSHOP'),('jobs.approve','Approve Repair Jobs','WORKSHOP'),
    ('requisitions.view','View Requisitions','PROCUREMENT'),('requisitions.create','Create Requisitions','PROCUREMENT'),
    ('requisitions.submit','Submit Requisitions','PROCUREMENT'),('requisitions.approve','Approve Requisitions','PROCUREMENT'),
    ('quotations.view','View Quotations','PROCUREMENT'),('quotations.create','Create Quotations','PROCUREMENT'),
    ('quotations.negotiate','Negotiate Quotations','PROCUREMENT'),('quotations.approve','Approve Quotations','PROCUREMENT'),
    ('orders.view','View Orders','PROCUREMENT'),('orders.create','Create Orders','PROCUREMENT'),
    ('orders.approve','Approve Orders','PROCUREMENT'),('orders.amend','Amend Orders','PROCUREMENT'),
    ('suppliers.view','View Suppliers','PURCHASING'),('suppliers.manage','Manage Suppliers','PURCHASING'),
    ('supplier_po.create','Create Supplier POs','PURCHASING'),('supplier_po.approve','Approve Supplier POs','PURCHASING'),
    ('inventory.view','View Inventory','INVENTORY'),('inventory.manage','Manage Inventory','INVENTORY'),
    ('goods_receipt.create','Create Goods Receipts','INVENTORY'),('delivery.create','Create Deliveries','INVENTORY'),
    ('invoices.view','View Invoices','FINANCE'),('invoices.create','Create Invoices','FINANCE'),
    ('invoices.approve','Approve Invoices','FINANCE'),('payments.create','Create Payments','FINANCE'),('payments.approve','Approve Payments','FINANCE'),
    ('cashbook.view','View Cashbook','FINANCE'),('cashbook.post','Post Cashbook Entries','FINANCE'),('cashbook.reconcile','Reconcile Cashbook','FINANCE'),
    ('gl.view','View General Ledger','FINANCE'),('gl.post_journal','Post Manual Journals','FINANCE'),
    ('reports.view','View Reports','PLATFORM'),('audit.view','View Audit Trail','PLATFORM'),
    ('users.manage','Manage Users','PLATFORM'),('roles.manage','Manage Roles','PLATFORM'),('notifications.manage','Manage Notifications','PLATFORM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (id, organisation_id, email, password_hash, first_name, last_name) VALUES
    ('a2000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'admin@avmotors.com', crypt('Admin@123', gen_salt('bf', 12)), 'AV', 'Administrator'),
    ('b2000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'admin@smbprocurement.com', crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Administrator')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES
    ('a2000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'),
    ('b2000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN ('AV_SYS_ADMIN', 'SMB_ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO document_sequences (organisation_id, document_type, prefix, current_number) VALUES
    ('a0000000-0000-0000-0000-000000000001','REQUISITION','REQ',1000),('a0000000-0000-0000-0000-000000000001','QUOTATION','QT',2000),
    ('a0000000-0000-0000-0000-000000000001','ORDER','ORD',3000),('a0000000-0000-0000-0000-000000000001','INVOICE','INV',4000),
    ('a0000000-0000-0000-0000-000000000001','PAYMENT','PAY',5000),('a0000000-0000-0000-0000-000000000001','JOURNAL','JNL',6000),
    ('b0000000-0000-0000-0000-000000000001','SUPPLIER_PO','SPO',7000),('b0000000-0000-0000-0000-000000000001','GOODS_RECEIPT','GR',8000),
    ('b0000000-0000-0000-0000-000000000001','DELIVERY','DLV',9000)
ON CONFLICT DO NOTHING;

-- Default Chart of Accounts for SMB
INSERT INTO chart_of_accounts (organisation_id, account_code, account_name, account_type, sub_type, normal_balance) VALUES
    ('b0000000-0000-0000-0000-000000000001','1000','Assets','ASSET','CONTROL','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','1100','Bank Accounts','ASSET','BANK','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','1110','Cash on Hand','ASSET','CASH','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','1200','Trade Debtors','ASSET','DEBTOR_CONTROL','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','1300','Inventory','ASSET','INVENTORY','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','2000','Liabilities','LIABILITY','CONTROL','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','2100','Trade Creditors','LIABILITY','CREDITOR_CONTROL','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','2200','Tax Payable','LIABILITY','TAX','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','3000','Equity','EQUITY','CONTROL','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','3100','Share Capital','EQUITY','CAPITAL','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','3200','Retained Earnings','EQUITY','RESERVE','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','4000','Revenue','REVENUE','CONTROL','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','4100','Sales - Parts','REVENUE','SALES','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','4200','Sales - Labour','REVENUE','SALES','CREDIT'),
    ('b0000000-0000-0000-0000-000000000001','5000','Expenses','EXPENSE','CONTROL','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','5100','Cost of Sales - Parts','EXPENSE','COS','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','5200','Cost of Sales - Labour','EXPENSE','COS','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','5300','Freight & Delivery','EXPENSE','DIRECT_COST','DEBIT'),
    ('b0000000-0000-0000-0000-000000000001','5400','Operating Expenses','EXPENSE','OPEX','DEBIT')
ON CONFLICT DO NOTHING;
