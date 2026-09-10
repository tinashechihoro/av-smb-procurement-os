-- ============================================================================
-- V4: Complete document number sequences for both organisations
--
-- V1 seeded only a subset: REPAIR_JOB was missing entirely (workshop job
-- creation failed), and the SMB organisation lacked JOURNAL/INVOICE/PAYMENT/
-- QUOTATION/ORDER/REQUISITION sequences used by its finance and cross-organisation
-- flows. The generator now self-heals missing rows, but seeding all types
-- keeps document numbering deterministic and pretty-prefixed.
-- ============================================================================

INSERT INTO document_sequences (organisation_id, document_type, prefix, current_number, padding)
SELECT o.id, t.type, t.prefix, t.start, 5
FROM organisations o
CROSS JOIN (VALUES
    ('REQUISITION',  'REQ', 1000),
    ('QUOTATION',    'QT',  2000),
    ('ORDER',        'ORD', 3000),
    ('REPAIR_JOB',   'RJ',  2500),
    ('INVOICE',      'INV', 4000),
    ('PAYMENT',      'PAY', 5000),
    ('JOURNAL',      'JNL', 6000),
    ('SUPPLIER_PO',  'SPO', 7000),
    ('GOODS_RECEIPT','GR',  8000),
    ('DELIVERY',     'DLV', 9000)
) AS t(type, prefix, start)
ON CONFLICT DO NOTHING;
