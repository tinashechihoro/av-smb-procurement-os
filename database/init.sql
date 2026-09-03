-- ============================================================================
-- AV Motors x SMB Procurement OS — Database Initialization
-- PostgreSQL 16 with extensions
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";        -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";          -- crypt / gen_random_bytes
CREATE EXTENSION IF NOT EXISTS "pg_trgm";           -- trigram similarity for fuzzy search
CREATE EXTENSION IF NOT EXISTS "btree_gist";        -- GiST index support for exclusion constraints
CREATE EXTENSION IF NOT EXISTS "moddatetime";       -- auto-update modified_at triggers

-- ============================================================================
-- AUDIT SCHEMA — append-only immutable audit log
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

CREATE INDEX idx_audit_org ON audit.audit_log(organisation_id);
CREATE INDEX idx_audit_entity ON audit.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit.audit_log(user_id);
CREATE INDEX idx_audit_action ON audit.audit_log(action);
CREATE INDEX idx_audit_created ON audit.audit_log(created_at DESC);
CREATE INDEX idx_audit_org_created ON audit.audit_log(organisation_id, created_at DESC);

-- Generic audit trigger function
CREATE OR REPLACE FUNCTION audit.fn_audit_trigger()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
    v_org_id UUID;
    v_action TEXT;
    v_setting TEXT;
BEGIN
    -- Gracefully handle missing session context (e.g. during seed/migration)
    v_setting := current_setting('app.current_org_id', true);
    IF v_setting IS NULL OR v_setting = '' THEN
        IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
        RETURN NEW;
    END IF;
    v_org_id := v_setting::uuid;

    v_setting := current_setting('app.current_user_id', true);
    IF v_setting IS NOT NULL AND v_setting <> '' THEN
        v_user_id := v_setting::uuid;
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

-- Helper to attach audit trigger to a table
CREATE OR REPLACE FUNCTION audit.attach_audit(p_table TEXT)
RETURNS void AS $$
BEGIN
    EXECUTE format(
        'CREATE TRIGGER trg_audit_%s AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_trigger()',
        p_table, p_table
    );
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- CORE: Organisations
-- ============================================================================
CREATE TABLE organisations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL UNIQUE,
    org_type        VARCHAR(20) NOT NULL CHECK (org_type IN ('BUYER', 'SUPPLIER')),
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    fiscal_year_start DATE,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- IDENTITY: Users, Roles, Permissions
-- ============================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(30),
    avatar_url      TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    mfa_enabled     BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_org ON users(organisation_id);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, code)
);

CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    module          VARCHAR(50) NOT NULL,
    description     TEXT
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE approval_limits (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id),
    approval_type   VARCHAR(50) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    single_limit    NUMERIC(18,2) NOT NULL DEFAULT 0,
    cumulative_limit NUMERIC(18,2),
    requires_second_approval BOOLEAN NOT NULL DEFAULT false,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- WORKSHOP: Customers, Insurers, Vehicles, Repair Jobs
-- ============================================================================
CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(30),
    address         TEXT,
    tax_number      VARCHAR(50),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE insurers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    name            VARCHAR(200) NOT NULL,
    contact_person  VARCHAR(200),
    email           VARCHAR(255),
    phone           VARCHAR(30),
    policy_prefix   VARCHAR(30),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vehicles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    customer_id     UUID REFERENCES customers(id),
    insurer_id      UUID REFERENCES insurers(id),
    registration    VARCHAR(30) NOT NULL,
    vin             VARCHAR(30),
    make            VARCHAR(100) NOT NULL,
    model           VARCHAR(100) NOT NULL,
    year            INTEGER,
    color           VARCHAR(50),
    engine_number   VARCHAR(50),
    mileage         INTEGER,
    fuel_type       VARCHAR(20),
    vehicle_type    VARCHAR(30),
    status          VARCHAR(30) NOT NULL DEFAULT 'IN_WORKSHOP' CHECK (status IN ('IN_WORKSHOP','UNDER_REPAIR','AWAITING_PARTS','COMPLETED','DELIVERED')),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vehicles_org ON vehicles(organisation_id);
CREATE INDEX idx_vehicles_reg ON vehicles(registration);
CREATE INDEX idx_vehicles_customer ON vehicles(customer_id);

CREATE TABLE repair_jobs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    vehicle_id      UUID NOT NULL REFERENCES vehicles(id),
    job_number      VARCHAR(30) NOT NULL UNIQUE,
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    job_type        VARCHAR(30) NOT NULL DEFAULT 'COLLISION' CHECK (job_type IN ('COLLISION','MECHANICAL','ELECTRICAL','PAINT_ONLY','FULL_RESTORE')),
    status          VARCHAR(30) NOT NULL DEFAULT 'ASSESSMENT' CHECK (status IN ('ASSESSMENT','ESTIMATING','AWAITING_AUTHORITY','IN_REPAIR','PAINTING','REASSEMBLY','QC','COMPLETED','HANDED_OVER')),
    repair_stage    VARCHAR(30) NOT NULL DEFAULT 'INITIAL_ASSESSMENT',
    priority        VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    assigned_technician VARCHAR(200),
    bay_number      VARCHAR(20),
    booked_hours    NUMERIC(8,2),
    actual_hours    NUMERIC(8,2),
    labour_rate     NUMERIC(10,2),
    insurer_claim_number VARCHAR(80),
    insurer_authorised BOOLEAN NOT NULL DEFAULT false,
    excess_amount   NUMERIC(12,2),
    supplement_amount NUMERIC(12,2),
    estimated_total NUMERIC(14,2) DEFAULT 0,
    actual_total    NUMERIC(14,2) DEFAULT 0,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    handed_over_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_repair_jobs_org ON repair_jobs(organisation_id);
CREATE INDEX idx_repair_jobs_vehicle ON repair_jobs(vehicle_id);
CREATE INDEX idx_repair_jobs_status ON repair_jobs(status);

-- ============================================================================
-- WORKSHOP: Damage Assessments, Repair Operations, QC, Photos
-- ============================================================================
CREATE TABLE damage_assessments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repair_job_id   UUID NOT NULL REFERENCES repair_jobs(id),
    assessed_by     UUID NOT NULL REFERENCES users(id),
    zone            VARCHAR(50) NOT NULL,
    severity        VARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH')),
    description     TEXT,
    parts_affected  JSONB,
    repair_method   VARCHAR(100),
    estimated_cost  NUMERIC(12,2),
    assessed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE repair_operations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repair_job_id   UUID NOT NULL REFERENCES repair_jobs(id),
    operation_name  VARCHAR(200) NOT NULL,
    operation_type  VARCHAR(50) NOT NULL CHECK (operation_type IN ('PANEL','STRUCTURAL','PAINT','MECHANICAL','ELECTRICAL','REASSEMBLY','DETAILING')),
    assigned_to     VARCHAR(200),
    booked_hours    NUMERIC(8,2),
    actual_hours    NUMERIC(8,2),
    sequence_order  INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','QC_PASSED','QC_FAILED')),
    notes           TEXT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ
);

CREATE TABLE quality_checks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repair_job_id   UUID NOT NULL REFERENCES repair_jobs(id),
    operation_id    UUID REFERENCES repair_operations(id),
    checked_by      UUID NOT NULL REFERENCES users(id),
    check_type      VARCHAR(50) NOT NULL,
    passed          BOOLEAN NOT NULL DEFAULT false,
    comments        TEXT,
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE job_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repair_job_id   UUID NOT NULL REFERENCES repair_jobs(id),
    document_type   VARCHAR(30) NOT NULL CHECK (document_type IN ('PHOTO','INSURANCE_DOC','ESTIMATE','INVOICE','AUTHORITY','OTHER')),
    file_name       VARCHAR(300) NOT NULL,
    file_path       TEXT NOT NULL,
    file_size       BIGINT,
    mime_type       VARCHAR(100),
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE repair_stage_history (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repair_job_id   UUID NOT NULL REFERENCES repair_jobs(id),
    from_stage      VARCHAR(30),
    to_stage        VARCHAR(30) NOT NULL,
    changed_by      UUID NOT NULL REFERENCES users(id),
    notes           TEXT,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- PROCUREMENT: Requisitions
-- ============================================================================
CREATE TABLE requisitions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    repair_job_id   UUID REFERENCES repair_jobs(id),
    requisition_number VARCHAR(30) NOT NULL UNIQUE,
    title           VARCHAR(300) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','CLARIFICATION','QUOTED','APPROVED','CONVERTED_TO_ORDER','CLOSED','CANCELLED')),
    priority        VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    requested_by    UUID NOT NULL REFERENCES users(id),
    total_estimate  NUMERIC(14,2) DEFAULT 0,
    amendment_number INTEGER NOT NULL DEFAULT 0,
    parent_requisition_id UUID REFERENCES requisitions(id),
    notes           TEXT,
    submitted_at    TIMESTAMPTZ,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_requisitions_org ON requisitions(organisation_id);
CREATE INDEX idx_requisitions_job ON requisitions(repair_job_id);
CREATE INDEX idx_requisitions_status ON requisitions(status);

CREATE TABLE requisition_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requisition_id  UUID NOT NULL REFERENCES requisitions(id) ON DELETE CASCADE,
    line_number     INTEGER NOT NULL,
    part_number     VARCHAR(80),
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL DEFAULT 1,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'EA',
    estimated_cost  NUMERIC(12,2),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE clarification_threads (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requisition_id  UUID NOT NULL REFERENCES requisitions(id),
    raised_by       UUID NOT NULL REFERENCES users(id),
    subject         VARCHAR(300) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESPONDED','CLOSED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE clarification_messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id       UUID NOT NULL REFERENCES clarification_threads(id),
    sender_id       UUID NOT NULL REFERENCES users(id),
    message         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- PROCUREMENT: Quotations
-- ============================================================================
CREATE TABLE quotations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    requisition_id  UUID NOT NULL REFERENCES requisitions(id),
    quotation_number VARCHAR(30) NOT NULL UNIQUE,
    title           VARCHAR(300),
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','UNDER_NEGOTIATION','REVISED','APPROVED','REJECTED','CONVERTED_TO_ORDER','EXPIRED')),
    current_version INTEGER NOT NULL DEFAULT 1,
    subtotal        NUMERIC(14,2) DEFAULT 0,
    tax_rate        NUMERIC(5,2) DEFAULT 0,
    tax_amount      NUMERIC(14,2) DEFAULT 0,
    total_amount    NUMERIC(14,2) DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    valid_until     DATE,
    prepared_by     UUID REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_quotations_org ON quotations(organisation_id);
CREATE INDEX idx_quotations_req ON quotations(requisition_id);
CREATE INDEX idx_quotations_status ON quotations(status);

CREATE TABLE quotation_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quotation_id    UUID NOT NULL REFERENCES quotations(id),
    version_number  INTEGER NOT NULL,
    subtotal        NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    change_summary  TEXT,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(quotation_id, version_number)
);

CREATE TABLE quotation_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quotation_id    UUID NOT NULL REFERENCES quotations(id),
    version_id      UUID REFERENCES quotation_versions(id),
    requisition_item_id UUID REFERENCES requisition_items(id),
    line_number     INTEGER NOT NULL,
    part_number     VARCHAR(80),
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_pct    NUMERIC(5,2) DEFAULT 0,
    line_total      NUMERIC(14,2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE negotiation_messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quotation_id    UUID NOT NULL REFERENCES quotations(id),
    sender_id       UUID NOT NULL REFERENCES users(id),
    message         TEXT NOT NULL,
    proposed_amount NUMERIC(14,2),
    is_av_message   BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- PROCUREMENT: Approvals
-- ============================================================================
CREATE TABLE approvals (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    approval_type   VARCHAR(50) NOT NULL CHECK (approval_type IN ('QUOTATION','ORDER','ORDER_AMENDMENT','PAYMENT','JOURNAL','CREDIT_NOTE')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    requested_by    UUID NOT NULL REFERENCES users(id),
    approved_by     UUID REFERENCES users(id),
    amount          NUMERIC(14,2),
    comments        TEXT,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_approvals_entity ON approvals(entity_type, entity_id);
CREATE INDEX idx_approvals_status ON approvals(status);

-- ============================================================================
-- PROCUREMENT: AV Orders
-- ============================================================================
CREATE TABLE av_orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    quotation_id    UUID REFERENCES quotations(id),
    repair_job_id   UUID REFERENCES repair_jobs(id),
    order_number    VARCHAR(30) NOT NULL UNIQUE,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','CONFIRMED','PARTIAL_DELIVERY','DELIVERED','INVOICED','CLOSED','CANCELLED')),
    order_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal        NUMERIC(14,2) DEFAULT 0,
    tax_amount      NUMERIC(14,2) DEFAULT 0,
    total_amount    NUMERIC(14,2) DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    expected_delivery DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_av_orders_org ON av_orders(organisation_id);
CREATE INDEX idx_av_orders_status ON av_orders(status);

CREATE TABLE av_order_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL REFERENCES av_orders(id),
    quotation_item_id UUID REFERENCES quotation_items(id),
    line_number     INTEGER NOT NULL,
    part_number     VARCHAR(80),
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL,
    delivered_qty   NUMERIC(12,3) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_amendments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL REFERENCES av_orders(id),
    amendment_number INTEGER NOT NULL DEFAULT 1,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    requested_by    UUID NOT NULL REFERENCES users(id),
    approved_by     UUID REFERENCES users(id),
    amount_change   NUMERIC(14,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at     TIMESTAMPTZ
);

-- ============================================================================
-- SMB PURCHASING: Suppliers
-- ============================================================================
CREATE TABLE suppliers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    name            VARCHAR(300) NOT NULL,
    contact_person  VARCHAR(200),
    email           VARCHAR(255),
    phone           VARCHAR(30),
    address         TEXT,
    tax_number      VARCHAR(50),
    payment_terms   INTEGER DEFAULT 30,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    rating          NUMERIC(3,2),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_quotes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_id     UUID NOT NULL REFERENCES suppliers(id),
    av_order_item_id UUID REFERENCES av_order_items(id),
    unit_cost       NUMERIC(12,2) NOT NULL,
    freight_cost    NUMERIC(12,2) DEFAULT 0,
    lead_time_days  INTEGER,
    valid_until     DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED')),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_purchase_orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    supplier_id     UUID NOT NULL REFERENCES suppliers(id),
    av_order_id     UUID REFERENCES av_orders(id),
    po_number       VARCHAR(30) NOT NULL UNIQUE,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SENT','CONFIRMED','SHIPPED','RECEIVED','PARTIAL_RECEIPT','CANCELLED')),
    order_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal        NUMERIC(14,2) DEFAULT 0,
    freight_total   NUMERIC(12,2) DEFAULT 0,
    total_amount    NUMERIC(14,2) DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    expected_delivery DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_po_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    po_id           UUID NOT NULL REFERENCES supplier_purchase_orders(id),
    av_order_item_id UUID REFERENCES av_order_items(id),
    line_number     INTEGER NOT NULL,
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    unit_cost       NUMERIC(12,2) NOT NULL,
    freight_alloc   NUMERIC(12,2) DEFAULT 0,
    line_total      NUMERIC(14,2) NOT NULL,
    received_qty    NUMERIC(12,3) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- INVENTORY: Goods Receipts, Inventory, Stock Movements
-- ============================================================================
CREATE TABLE locations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(30) NOT NULL,
    location_type   VARCHAR(20) NOT NULL DEFAULT 'BIN' CHECK (location_type IN ('WAREHOUSE','SHELF','BIN','TRANSIT','QUARANTINE')),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, code)
);

CREATE TABLE inventory_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    location_id     UUID REFERENCES locations(id),
    part_number     VARCHAR(80) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    quantity_on_hand NUMERIC(12,3) NOT NULL DEFAULT 0,
    quantity_reserved NUMERIC(12,3) NOT NULL DEFAULT 0,
    quantity_available NUMERIC(12,3) GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    reorder_level   NUMERIC(12,3) DEFAULT 0,
    unit_cost       NUMERIC(12,2),
    selling_price   NUMERIC(12,2),
    category        VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISCONTINUED','QUARANTINED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_org ON inventory_items(organisation_id);
CREATE INDEX idx_inventory_part ON inventory_items(part_number);

CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    movement_type   VARCHAR(30) NOT NULL CHECK (movement_type IN ('RECEIPT','ISSUE','TRANSFER','ADJUSTMENT','RESERVATION','RELEASE')),
    quantity        NUMERIC(12,3) NOT NULL,
    from_location_id UUID REFERENCES locations(id),
    to_location_id  UUID REFERENCES locations(id),
    reference_type  VARCHAR(50),
    reference_id    UUID,
    performed_by    UUID NOT NULL REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE goods_receipts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    supplier_po_id  UUID REFERENCES supplier_purchase_orders(id),
    av_order_id     UUID REFERENCES av_orders(id),
    receipt_number  VARCHAR(30) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','RECEIVED','INSPECTED','ACCEPTED','REJECTED')),
    received_by     UUID NOT NULL REFERENCES users(id),
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE goods_receipt_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    receipt_id      UUID NOT NULL REFERENCES goods_receipts(id),
    po_item_id      UUID REFERENCES supplier_po_items(id),
    order_item_id   UUID REFERENCES av_order_items(id),
    inventory_item_id UUID REFERENCES inventory_items(id),
    line_number     INTEGER NOT NULL,
    description     VARCHAR(500) NOT NULL,
    expected_qty    NUMERIC(12,3) NOT NULL,
    received_qty    NUMERIC(12,3) NOT NULL,
    accepted_qty    NUMERIC(12,3) NOT NULL DEFAULT 0,
    rejected_qty    NUMERIC(12,3) NOT NULL DEFAULT 0,
    location_id     UUID REFERENCES locations(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- FULFILMENT: Delivery Notes, AV Receipt Confirmations
-- ============================================================================
CREATE TABLE delivery_notes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    av_order_id     UUID NOT NULL REFERENCES av_orders(id),
    delivery_number VARCHAR(30) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PREPARED' CHECK (status IN ('PREPARED','DISPATCHED','IN_TRANSIT','DELIVERED','PARTIAL_DELIVERY')),
    delivery_date   DATE,
    delivered_by    VARCHAR(200),
    vehicle_reg     VARCHAR(30),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE delivery_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    delivery_id     UUID NOT NULL REFERENCES delivery_notes(id),
    order_item_id   UUID REFERENCES av_order_items(id),
    line_number     INTEGER NOT NULL,
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE av_receipt_confirmations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    delivery_id     UUID NOT NULL REFERENCES delivery_notes(id),
    confirmation_number VARCHAR(30) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','DISCREPANCY_REPORTED')),
    confirmed_by    UUID NOT NULL REFERENCES users(id),
    confirmed_at    TIMESTAMPTZ,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- FINANCE: Chart of Accounts, Journals, Invoices, Payments
-- ============================================================================
CREATE TABLE chart_of_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    account_code    VARCHAR(20) NOT NULL,
    account_name    VARCHAR(200) NOT NULL,
    account_type    VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    sub_type        VARCHAR(50),
    parent_id       UUID REFERENCES chart_of_accounts(id),
    is_postable     BOOLEAN NOT NULL DEFAULT true,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    normal_balance  VARCHAR(10) NOT NULL CHECK (normal_balance IN ('DEBIT','CREDIT')),
    opening_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, account_code)
);

CREATE TABLE cash_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    account_name    VARCHAR(200) NOT NULL,
    account_type    VARCHAR(20) NOT NULL CHECK (account_type IN ('BANK','CASH','MOBILE_MONEY')),
    account_number  VARCHAR(50),
    bank_name       VARCHAR(200),
    branch_name     VARCHAR(200),
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    gl_account_id   UUID REFERENCES chart_of_accounts(id),
    balance         NUMERIC(18,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE journals (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    journal_number  VARCHAR(30) NOT NULL UNIQUE,
    journal_type    VARCHAR(30) NOT NULL CHECK (journal_type IN ('MANUAL','AUTO_INVOICE','AUTO_PAYMENT','AUTO_RECEIPT','AUTO_PURCHASE','AUTO_CASHBOOK','REVERSAL','CORRECTION')),
    description     TEXT,
    source_type     VARCHAR(50),
    source_id       UUID,
    total_debit     NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_credit    NUMERIC(18,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','POSTED','REVERSED')),
    posted_by       UUID REFERENCES users(id),
    posted_at       TIMESTAMPTZ,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_journals_org ON journals(organisation_id);
CREATE INDEX idx_journals_type ON journals(journal_type);

CREATE TABLE journal_lines (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_id      UUID NOT NULL REFERENCES journals(id),
    line_number     INTEGER NOT NULL,
    account_id      UUID NOT NULL REFERENCES chart_of_accounts(id),
    description     VARCHAR(500),
    debit_amount    NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    av_order_id     UUID REFERENCES av_orders(id),
    repair_job_id   UUID REFERENCES repair_jobs(id),
    invoice_number  VARCHAR(30) NOT NULL UNIQUE,
    invoice_type    VARCHAR(20) NOT NULL DEFAULT 'STANDARD' CHECK (invoice_type IN ('STANDARD','CREDIT_NOTE','DEBIT_NOTE')),
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','SENT','PARTIAL_PAYMENT','PAID','OVERDUE','CANCELLED')),
    invoice_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE,
    from_entity     VARCHAR(200) NOT NULL,
    to_entity       VARCHAR(200) NOT NULL,
    subtotal        NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(5,2) DEFAULT 0,
    tax_amount      NUMERIC(14,2) DEFAULT 0,
    total_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    amount_paid     NUMERIC(14,2) NOT NULL DEFAULT 0,
    balance_due     NUMERIC(14,2) GENERATED ALWAYS AS (total_amount - amount_paid) STORED,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoices_org ON invoices(organisation_id);
CREATE INDEX idx_invoices_status ON invoices(status);

CREATE TABLE invoice_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id      UUID NOT NULL REFERENCES invoices(id),
    order_item_id   UUID REFERENCES av_order_items(id),
    line_number     INTEGER NOT NULL,
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    invoice_id      UUID REFERENCES invoices(id),
    payment_number  VARCHAR(30) NOT NULL UNIQUE,
    payment_type    VARCHAR(20) NOT NULL CHECK (payment_type IN ('RECEIPT','REFUND')),
    amount          NUMERIC(14,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method  VARCHAR(30) NOT NULL CHECK (payment_method IN ('BANK_TRANSFER','CASH','CHEQUE','MOBILE_MONEY','CARD')),
    reference       VARCHAR(100),
    cash_account_id UUID REFERENCES cash_accounts(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED','FAILED','REVERSED')),
    received_by     UUID REFERENCES users(id),
    payment_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_org ON payments(organisation_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);

-- ============================================================================
-- FINANCE: Cashbook Entries
-- ============================================================================
CREATE TABLE cashbook_entries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    cash_account_id UUID NOT NULL REFERENCES cash_accounts(id),
    journal_id      UUID REFERENCES journals(id),
    entry_type      VARCHAR(10) NOT NULL CHECK (entry_type IN ('RECEIPT','PAYMENT','TRANSFER')),
    amount          NUMERIC(18,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    counterparty    VARCHAR(200),
    narration       TEXT,
    reference       VARCHAR(100),
    source_type     VARCHAR(50),
    source_id       UUID,
    running_balance NUMERIC(18,2),
    reconciliation_status VARCHAR(20) NOT NULL DEFAULT 'UNRECONCILED' CHECK (reconciliation_status IN ('UNRECONCILED','RECONCILED','PENDING_REVIEW')),
    reconciled_by   UUID REFERENCES users(id),
    reconciled_at   TIMESTAMPTZ,
    entry_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cashbook_org ON cashbook_entries(organisation_id);
CREATE INDEX idx_cashbook_account ON cashbook_entries(cash_account_id);
CREATE INDEX idx_cashbook_date ON cashbook_entries(entry_date DESC);

-- ============================================================================
-- FINANCE: Debtor / Creditor Ledger
-- ============================================================================
CREATE TABLE debtor_ledger (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    invoice_id      UUID REFERENCES invoices(id),
    payment_id      UUID REFERENCES payments(id),
    entry_type      VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount          NUMERIC(18,2) NOT NULL,
    balance_after   NUMERIC(18,2),
    entry_date      DATE NOT NULL,
    narration       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE creditor_ledger (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    supplier_id     UUID NOT NULL REFERENCES suppliers(id),
    supplier_po_id  UUID REFERENCES supplier_purchase_orders(id),
    payment_id      UUID REFERENCES payments(id),
    entry_type      VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount          NUMERIC(18,2) NOT NULL,
    balance_after   NUMERIC(18,2),
    entry_date      DATE NOT NULL,
    narration       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- PLATFORM: Notifications, Document Numbering
-- ============================================================================
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    title           VARCHAR(300) NOT NULL,
    message         TEXT,
    notification_type VARCHAR(30) NOT NULL CHECK (notification_type IN ('INFO','APPROVAL','ALERT','SYSTEM','TASK')),
    entity_type     VARCHAR(50),
    entity_id       UUID,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

CREATE TABLE document_sequences (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    document_type   VARCHAR(50) NOT NULL,
    prefix          VARCHAR(10) NOT NULL,
    current_number  BIGINT NOT NULL DEFAULT 0,
    padding         INTEGER NOT NULL DEFAULT 5,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organisation_id, document_type)
);

-- ============================================================================
-- ATTACH AUDIT TRIGGERS to all material tables
-- ============================================================================
SELECT audit.attach_audit('organisations');
SELECT audit.attach_audit('users');
SELECT audit.attach_audit('roles');
SELECT audit.attach_audit('permissions');
SELECT audit.attach_audit('customers');
SELECT audit.attach_audit('insurers');
SELECT audit.attach_audit('vehicles');
SELECT audit.attach_audit('repair_jobs');
SELECT audit.attach_audit('damage_assessments');
SELECT audit.attach_audit('repair_operations');
SELECT audit.attach_audit('quality_checks');
SELECT audit.attach_audit('requisitions');
SELECT audit.attach_audit('requisition_items');
SELECT audit.attach_audit('quotations');
SELECT audit.attach_audit('quotation_versions');
SELECT audit.attach_audit('quotation_items');
SELECT audit.attach_audit('approvals');
SELECT audit.attach_audit('av_orders');
SELECT audit.attach_audit('av_order_items');
SELECT audit.attach_audit('order_amendments');
SELECT audit.attach_audit('suppliers');
SELECT audit.attach_audit('supplier_purchase_orders');
SELECT audit.attach_audit('goods_receipts');
SELECT audit.attach_audit('inventory_items');
SELECT audit.attach_audit('stock_movements');
SELECT audit.attach_audit('delivery_notes');
SELECT audit.attach_audit('av_receipt_confirmations');
SELECT audit.attach_audit('invoices');
SELECT audit.attach_audit('invoice_items');
SELECT audit.attach_audit('payments');
SELECT audit.attach_audit('cashbook_entries');
SELECT audit.attach_audit('journals');
SELECT audit.attach_audit('journal_lines');
SELECT audit.attach_audit('chart_of_accounts');
SELECT audit.attach_audit('notifications');

-- ============================================================================
-- SEED: Organisations and default roles
-- ============================================================================
INSERT INTO organisations (id, name, code, org_type, currency) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'AV Motors', 'AV', 'BUYER', 'USD'),
    ('b0000000-0000-0000-0000-000000000001', 'SMB Procurement', 'SMB', 'SUPPLIER', 'USD');

-- AV Roles
INSERT INTO roles (id, organisation_id, name, code, is_system) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'System Administrator', 'AV_SYS_ADMIN', true),
    ('a1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Manager / Approver', 'AV_MANAGER', true),
    ('a1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Estimator', 'AV_ESTIMATOR', true),
    ('a1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Workshop Technician', 'AV_TECHNICIAN', true),
    ('a1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Procurement Officer', 'AV_PROCUREMENT', true),
    ('a1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'Accounts Officer', 'AV_ACCOUNTS', true);

-- SMB Roles
INSERT INTO roles (id, organisation_id, name, code, is_system) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'SMB Administrator', 'SMB_ADMIN', true),
    ('b1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'SMB Manager', 'SMB_MANAGER', true),
    ('b1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'Quotation Officer', 'SMB_QUOTATION', true),
    ('b1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'Purchasing Officer', 'SMB_PURCHASING', true),
    ('b1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'Stores Officer', 'SMB_STORES', true),
    ('b1000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 'Sales & Accounts', 'SMB_ACCOUNTS', true);

-- Permissions
INSERT INTO permissions (code, name, module) VALUES
    ('vehicles.view', 'View Vehicles', 'WORKSHOP'),
    ('vehicles.manage', 'Manage Vehicles', 'WORKSHOP'),
    ('jobs.view', 'View Repair Jobs', 'WORKSHOP'),
    ('jobs.manage', 'Manage Repair Jobs', 'WORKSHOP'),
    ('jobs.approve', 'Approve Repair Jobs', 'WORKSHOP'),
    ('requisitions.view', 'View Requisitions', 'PROCUREMENT'),
    ('requisitions.create', 'Create Requisitions', 'PROCUREMENT'),
    ('requisitions.submit', 'Submit Requisitions', 'PROCUREMENT'),
    ('requisitions.approve', 'Approve Requisitions', 'PROCUREMENT'),
    ('quotations.view', 'View Quotations', 'PROCUREMENT'),
    ('quotations.create', 'Create Quotations', 'PROCUREMENT'),
    ('quotations.negotiate', 'Negotiate Quotations', 'PROCUREMENT'),
    ('quotations.approve', 'Approve Quotations', 'PROCUREMENT'),
    ('orders.view', 'View Orders', 'PROCUREMENT'),
    ('orders.create', 'Create Orders', 'PROCUREMENT'),
    ('orders.approve', 'Approve Orders', 'PROCUREMENT'),
    ('orders.amend', 'Amend Orders', 'PROCUREMENT'),
    ('suppliers.view', 'View Suppliers', 'PURCHASING'),
    ('suppliers.manage', 'Manage Suppliers', 'PURCHASING'),
    ('supplier_po.create', 'Create Supplier POs', 'PURCHASING'),
    ('supplier_po.approve', 'Approve Supplier POs', 'PURCHASING'),
    ('inventory.view', 'View Inventory', 'INVENTORY'),
    ('inventory.manage', 'Manage Inventory', 'INVENTORY'),
    ('goods_receipt.create', 'Create Goods Receipts', 'INVENTORY'),
    ('delivery.create', 'Create Deliveries', 'INVENTORY'),
    ('invoices.view', 'View Invoices', 'FINANCE'),
    ('invoices.create', 'Create Invoices', 'FINANCE'),
    ('invoices.approve', 'Approve Invoices', 'FINANCE'),
    ('payments.create', 'Create Payments', 'FINANCE'),
    ('payments.approve', 'Approve Payments', 'FINANCE'),
    ('cashbook.view', 'View Cashbook', 'FINANCE'),
    ('cashbook.post', 'Post Cashbook Entries', 'FINANCE'),
    ('cashbook.reconcile', 'Reconcile Cashbook', 'FINANCE'),
    ('gl.view', 'View General Ledger', 'FINANCE'),
    ('gl.post_journal', 'Post Manual Journals', 'FINANCE'),
    ('reports.view', 'View Reports', 'PLATFORM'),
    ('audit.view', 'View Audit Trail', 'PLATFORM'),
    ('users.manage', 'Manage Users', 'PLATFORM'),
    ('roles.manage', 'Manage Roles', 'PLATFORM'),
    ('notifications.manage', 'Manage Notifications', 'PLATFORM');

-- Seed AV Admin user (password: Admin@123)
INSERT INTO users (id, organisation_id, email, password_hash, first_name, last_name) VALUES
    ('a2000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'admin@avmotors.com', crypt('Admin@123', gen_salt('bf', 12)), 'AV', 'Administrator');

-- Seed SMB Admin user (password: Admin@123)
INSERT INTO users (id, organisation_id, email, password_hash, first_name, last_name) VALUES
    ('b2000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'admin@smbprocurement.com', crypt('Admin@123', gen_salt('bf', 12)), 'SMB', 'Administrator');

-- Assign admin roles
INSERT INTO user_roles (user_id, role_id) VALUES
    ('a2000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'),
    ('b2000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001');

-- Grant all permissions to admin roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('AV_SYS_ADMIN', 'SMB_ADMIN');

-- Document sequences
INSERT INTO document_sequences (organisation_id, document_type, prefix, current_number) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'REQUISITION', 'REQ', 1000),
    ('a0000000-0000-0000-0000-000000000001', 'QUOTATION', 'QT', 2000),
    ('a0000000-0000-0000-0000-000000000001', 'ORDER', 'ORD', 3000),
    ('a0000000-0000-0000-0000-000000000001', 'INVOICE', 'INV', 4000),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENT', 'PAY', 5000),
    ('a0000000-0000-0000-0000-000000000001', 'JOURNAL', 'JNL', 6000),
    ('b0000000-0000-0000-0000-000000000001', 'SUPPLIER_PO', 'SPO', 7000),
    ('b0000000-0000-0000-0000-000000000001', 'GOODS_RECEIPT', 'GR', 8000),
    ('b0000000-0000-0000-0000-000000000001', 'DELIVERY', 'DLV', 9000);

-- Default Chart of Accounts for SMB
INSERT INTO chart_of_accounts (organisation_id, account_code, account_name, account_type, sub_type, normal_balance) VALUES
    ('b0000000-0000-0000-0000-000000000001', '1000', 'Assets', 'ASSET', 'CONTROL', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '1100', 'Bank Accounts', 'ASSET', 'BANK', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '1110', 'Cash on Hand', 'ASSET', 'CASH', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '1200', 'Trade Debtors', 'ASSET', 'DEBTOR_CONTROL', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '1300', 'Inventory', 'ASSET', 'INVENTORY', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '2000', 'Liabilities', 'LIABILITY', 'CONTROL', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '2100', 'Trade Creditors', 'LIABILITY', 'CREDITOR_CONTROL', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '2200', 'Tax Payable', 'LIABILITY', 'TAX', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '3000', 'Equity', 'EQUITY', 'CONTROL', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '3100', 'Share Capital', 'EQUITY', 'CAPITAL', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '3200', 'Retained Earnings', 'EQUITY', 'RESERVE', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '4000', 'Revenue', 'REVENUE', 'CONTROL', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '4100', 'Sales - Parts', 'REVENUE', 'SALES', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '4200', 'Sales - Labour', 'REVENUE', 'SALES', 'CREDIT'),
    ('b0000000-0000-0000-0000-000000000001', '5000', 'Expenses', 'EXPENSE', 'CONTROL', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '5100', 'Cost of Sales - Parts', 'EXPENSE', 'COS', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '5200', 'Cost of Sales - Labour', 'EXPENSE', 'COS', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '5300', 'Freight & Delivery', 'EXPENSE', 'DIRECT_COST', 'DEBIT'),
    ('b0000000-0000-0000-0000-000000000001', '5400', 'Operating Expenses', 'EXPENSE', 'OPEX', 'DEBIT');
