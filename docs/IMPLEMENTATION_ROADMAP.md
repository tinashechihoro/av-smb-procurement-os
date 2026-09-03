# AV Motors × SMB Procurement OS — Complete Implementation Roadmap

## STATUS: Phase 1-8 Complete ✅ | Remaining Work Below

---

## 🔴 PRIORITY 1 — Missing Backend Endpoints

### 1.1 Workshop Module
- [ ] `GET/POST /customers` — Customer CRUD
- [ ] `GET/POST /insurers` — Insurer CRUD
- [ ] `GET/POST /repair-jobs/{id}/damage-assessments` — Damage assessment CRUD
- [ ] `GET/POST /repair-jobs/{id}/operations` — Repair operation CRUD
- [ ] `POST /repair-jobs/{id}/quality-checks` — QC gate creation
- [ ] `POST /repair-jobs/{id}/stage-transition` — Stage change with history
- [ ] `GET /repair-jobs/{id}/timeline` — Full audit timeline for job

### 1.2 Procurement Module
- [ ] `GET/POST /requisitions/{id}/clarifications` — Clarification threads
- [ ] `POST /requisitions/{id}/clarifications/{threadId}/messages` — Messages
- [ ] `POST /quotations/{id}/negotiate` — Negotiation messages
- [ ] `GET /quotations/{id}/versions` — Version history
- [ ] `POST /quotations/{id}/revise` — Create new version
- [ ] `POST /approvals` — Create approval request
- [ ] `POST /approvals/{id}/decide` — Approve/reject with comments
- [ ] `GET /approvals/pending` — Pending approvals for current user
- [ ] `POST /orders/{id}/amend` — Order amendment with approval

### 1.3 Purchasing Module
- [ ] `GET/POST /supplier-quotes` — Supplier quote comparison
- [ ] `POST /supplier-pos/{id}/receive` — Mark as received
- [ ] `GET /supplier-pos/{id}/cost-analysis` — Cost breakdown

### 1.4 Inventory Module
- [ ] `GET/POST /locations` — Warehouse/bin CRUD
- [ ] `POST /inventory/{id}/reserve` — Reserve stock for job
- [ ] `POST /inventory/{id}/release` — Release reservation
- [ ] `POST /stock-movements` — Manual stock adjustment
- [ ] `GET /inventory/low-stock` — Low stock alert list
- [ ] `GET /inventory/valuation-report` — Full valuation with categories

### 1.5 Finance Module
- [ ] `POST /accounting-periods` — Open/close accounting periods
- [ ] `GET /accounting-periods` — List periods with status
- [ ] `POST /journals/{id}/reverse` — Create reversal journal
- [ ] `GET /debtor-ledger/{customerId}` — Customer statement
- [ ] `GET /creditor-ledger/{supplierId}` — Supplier statement
- [ ] `GET /debtor-ageing-report` — Ageing buckets with totals
- [ ] `GET /creditor-ageing-report` — Ageing buckets with totals
- [ ] `POST /cashbook/{id}/reconcile` — Mark as reconciled
- [ ] `POST /cashbook/bulk-reconcile` — Bulk reconciliation
- [ ] `GET /profit-and-loss` — P&L report by period
- [ ] `GET /balance-sheet` — Balance sheet by date

### 1.6 File & Document Management
- [ ] `POST /files/upload` — Multipart upload with virus scan
- [ ] `GET /files/{id}` — Download with auth check
- [ ] `GET /files/entity/{type}/{id}` — List files for entity
- [ ] `DELETE /files/{id}` — Soft delete
- [ ] `POST /documents/generate-invoice-pdf/{id}` — PDF generation
- [ ] `POST /documents/generate-receipt-pdf/{id}` — Receipt PDF

### 1.7 Notification System
- [ ] Auto-create notifications on status changes
- [ ] `GET /notifications/unread` — Unread count + list
- [ ] `POST /notifications/{id}/read` — Mark as read
- [ ] `POST /notifications/read-all` — Mark all as read
- [ ] Email notification integration (SendGrid/SES)
- [ ] Notification preferences per user

---

## 🟠 PRIORITY 2 — Missing Frontend Pages & Features

### 2.1 Deep Record Pages (Detail Views)
- [ ] `/vehicles/:id` — Full vehicle case file with tabs (Info, Jobs, Parts, Documents, Timeline)
- [ ] `/repair-jobs/:id` — Job detail with damage assessment UI, operation list, QC results, parts used, cost breakdown
- [ ] `/requisitions/:id` — Requisition detail with line items, clarification thread, approval history, amendment log
- [ ] `/quotations/:id` — Quotation detail with version comparison, negotiation thread, line items, approve/convert actions
- [ ] `/orders/:id` — Order detail with fulfilment tracker, delivery history, invoice links
- [ ] `/invoices/:id` — Invoice detail with three-way match (order ↔ receipt ↔ invoice), payment history
- [ ] `/suppliers/:id` — Supplier profile with PO history, performance ratings, cost analysis

### 2.2 Interactive Workshop Features
- [ ] Vehicle damage assessment UI — clickable vehicle silhouette with zone selection
- [ ] Repair stage strip — visual 8-stage progress with drag-to-advance
- [ ] Operation timeline — Gantt-style operation scheduling
- [ ] QC checklist — pass/fail checklist with photo attachment
- [ ] Job photo gallery — upload and view job photos

### 2.3 Procurement Workflow UI
- [ ] Kanban board for sourcing — drag quotations between stages
- [ ] Negotiation thread UI — bidirectional message thread with amount proposals
- [ ] Version comparison — side-by-side quotation version diff
- [ ] Approval workflow UI — approval chain visualization with comments
- [ ] Order amendment UI — amendment form with approval routing

### 2.4 Finance Advanced UI
- [ ] Bank reconciliation UI — side-by-side bank statement vs cashbook
- [ ] Journal entry template — recurring journal templates
- [ ] Period close wizard — step-by-period closing with validation
- [ ] Debtor/creditor statement print layout
- [ ] Invoice print/PDF preview with company branding
- [ ] Payment receipt generation

### 2.5 Platform Features
- [ ] Notification center dropdown — real-time bell icon with unread list
- [ ] Global search results panel — cross-entity search with typeahead
- [ ] User profile page — edit profile, change password, view permissions
- [ ] Profile photo upload with crop
- [ ] Dashboard widget configurator — drag-and-drop dashboard layout
- [ ] Dark mode toggle
- [ ] Language switcher (EN/FR)
- [ ] Keyboard shortcuts overlay (⌘K search, etc.)
- [ ] Print stylesheet for all report pages

---

## 🟡 PRIORITY 3 — Testing

### 3.1 Backend Tests
- [ ] IdentityServiceTest — login, RBAC, org switching, account lockout
- [ ] WorkshopServiceTest — vehicle CRUD, job lifecycle, stage transitions
- [ ] ProcurementServiceTest — requisition workflow, quotation creation, order conversion
- [ ] PurchasingServiceTest — supplier CRUD, PO creation, receiving
- [ ] InventoryServiceTest — stock movements, reservations, low stock alerts
- [ ] FinanceServiceTest — journal balance validation, invoice creation, payment posting
- [ ] ReportControllerTest — all report endpoints return correct data
- [ ] SecurityTest — unauthorized access, RBAC enforcement, rate limiting
- [ ] FileUploadControllerTest — upload, download, type validation, size limits

### 3.2 Frontend Tests
- [ ] AuthStore tests — login, logout, org switching
- [ ] useTable hook tests — sorting, filtering, search, pagination, CSV export
- [ ] DataTable component tests — column rendering, sort indicators
- [ ] Login page tests — form validation, error handling
- [ ] Dashboard tests — KPI card rendering, data loading

### 3.3 E2E Tests (Playwright)
- [ ] Full login → dashboard → create requisition → approve → convert to order flow
- [ ] Invoice creation → payment → reconciliation flow
- [ ] Vehicle creation → repair job → damage assessment → parts requisition flow
- [ ] Report generation and CSV export flow
- [ ] RBAC test — AV user cannot access SMB-only pages

---

## 🟢 PRIORITY 4 — Infrastructure & DevOps

### 4.1 Caching & Performance
- [ ] Redis container for session cache and rate limiting
- [ ] Spring Cache on read-heavy endpoints (chart of accounts, roles, permissions)
- [ ] Database query optimization — add composite indexes
- [ ] Hibernate batch fetching for nested collections
- [ ] Frontend React Query for data caching and deduplication

### 4.2 Monitoring & Observability
- [ ] Prometheus metrics endpoint (already exposed via actuator)
- [ ] Grafana dashboards for API latency, error rates, DB connections
- [ ] Structured JSON logging with correlation IDs
- [ ] ELK/Loki log aggregation
- [ ] Health check improvements — DB, cache, storage checks
- [ ] Alerting rules — error rate > 1%, latency > 2s, DB connection pool exhaustion

### 4.3 Backup & Disaster Recovery
- [ ] Automated pg_dump cron job (daily)
- [ ] Offsite backup storage (S3/Azure Blob)
- [ ] Backup retention policy (30 daily, 12 monthly)
- [ ] Disaster recovery runbook
- [ ] Database point-in-time recovery config

### 4.4 CI/CD Pipeline Enhancements
- [ ] Automated test execution in CI
- [ ] Docker image vulnerability scanning (Trivy)
- [ ] Staging environment deployment
- [ ] Blue/green deployment strategy
- [ ] Database migration validation in CI
- [ ] Performance regression testing

---

## 🔵 PRIORITY 5 — Security Hardening

### 5.1 Authentication
- [ ] MFA/TOTP implementation for admin roles
- [ ] Session inactivity timeout (15 min) with re-auth
- [ ] Password complexity enforcement (min 8 chars, upper, lower, number, special)
- [ ] Password history (prevent reuse of last 5)
- [ ] Account self-service password reset via email

### 5.2 Authorization
- [ ] Field-level security enforcement in API responses
- [ ] SMB data isolation — AV users cannot see supplier costs/creditors
- [ ] Approval amount limits enforced in service layer
- [ ] Segregation of duties — maker/checker enforcement
- [ ] IP whitelist for admin endpoints

### 5.3 Data Protection
- [ ] Database encryption at rest (TDE)
- [ ] PII field identification and masking in logs
- [ ] API response field masking for sensitive data
- [ ] Input sanitization (XSS prevention)
- [ ] SQL injection prevention (already using JPA named params)
- [ ] File upload virus scanning (ClamAV)

### 5.4 Compliance
- [ ] GDPR data export endpoint (already have `/compliance/data-export`)
- [ ] GDPR right to erasure with audit preservation
- [ ] Data retention policy enforcement (auto-archive old audit logs)
- [ ] Terms of service acceptance tracking
- [ ] Cookie consent banner
- [ ] Accessibility audit (WCAG 2.1 AA)

---

## 🟣 PRIORITY 6 — Advanced Features

### 6.1 Reporting & Analytics
- [ ] PDF report generation (iText/OpenPDF)
- [ ] Excel export with formatting (Apache POI)
- [ ] Scheduled report generation and email delivery
- [ ] Custom report builder UI
- [ ] Dashboard analytics charts (requisition trends, cost analysis)
- [ ] Supplier performance scorecard
- [ ] Inventory turnover analysis
- [ ] Job profitability analysis

### 6.2 Integration
- [ ] REST API documentation (OpenAPI 3.0 spec auto-generated)
- [ ] Webhook system for external integrations
- [ ] Accounting software integration (Xero, Sage, QuickBooks)
- [ ] SMS/WhatsApp notifications for job status updates
- [ ] Barcode/QR code generation for inventory items
- [ ] Email templates for invoices, receipts, notifications

### 6.3 Mobile & Offline
- [ ] PWA manifest and service worker
- [ ] Offline-capable data entry with sync
- [ ] Mobile-responsive bottom navigation
- [ ] Camera integration for photo capture
- [ ] Barcode scanner integration for stock receiving

### 6.4 Multi-tenancy
- [ ] Support for multiple workshop chains
- [ ] Inter-company transactions
- [ ] Consolidated reporting across organisations
- [ ] Organisation-level branding/theming

---

## 📊 COMPLETION TRACKER

| Category | Total Items | Complete | Remaining | % Done |
|----------|------------|----------|-----------|--------|
| Backend Endpoints | 55 | 35 | 20 | 64% |
| Frontend Pages | 35 | 17 | 18 | 49% |
| Testing | 25 | 2 | 23 | 8% |
| Infrastructure | 20 | 8 | 12 | 40% |
| Security | 18 | 10 | 8 | 56% |
| Advanced Features | 18 | 0 | 18 | 0% |
| **TOTAL** | **171** | **72** | **99** | **42%** |

---

## 🚀 RECOMMENDED NEXT SPRINT (2 weeks)

1. **Backend**: Customer/Insurer CRUD, Damage Assessment endpoints, Clarification threads
2. **Frontend**: Vehicle detail page, Repair Job detail page, Notification center
3. **Testing**: Backend service tests for Identity + Finance
4. **Security**: Password complexity, session timeout, field-level security
5. **Reports**: PDF invoice generation, Excel export

## 🎯 PRODUCTION READINESS CHECKLIST

- [ ] All backend endpoints have tests (>80% coverage)
- [ ] All frontend pages have E2E tests for critical flows
- [ ] MFA enabled for admin roles
- [ ] SSL/TLS configured
- [ ] Automated backups running
- [ ] Monitoring dashboards active
- [ ] Alerting rules configured
- [ ] Load testing passed (100 concurrent users)
- [ ] Security audit completed
- [ ] Accessibility audit passed (WCAG 2.1 AA)
- [ ] Documentation complete (API docs, user guide, admin guide)
- [ ] Disaster recovery tested
- [ ] GDPR compliance verified
