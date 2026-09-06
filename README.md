# AV Motors × SMB Procurement OS

Full-stack enterprise procurement and spare parts management platform for a panel-beating workshop (AV Motors) and its procurement entity (SMB).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | React 18 + TypeScript + Vite |
| **Backend** | Spring Boot 3.3 + Java 21 |
| **Database** | PostgreSQL 16 |
| **Auth** | JWT (stateless) |
| **ORM** | Spring Data JPA / Hibernate 6 |
| **State** | Zustand |
| **HTTP** | Axios |

## PostgreSQL Extensions

- **uuid-ossp** — UUID primary key generation
- **pgcrypto** — Password hashing (`crypt`/`gen_salt`)
- **pg_trgm** — Trigram similarity for fuzzy search
- **btree_gist** — GiST exclusion constraints
- **Custom audit schema** — Trigger-based immutable audit logging

## Project Structure

```
av-smb-platform/
├── backend/                          # Spring Boot API
│   ├── pom.xml
│   └── src/main/java/com/avsmc/procurement/
│       ├── config/                   # Security, CORS, Audit
│       ├── security/                 # JWT, RBAC, Permissions
│       ├── shared/                   # Base entities, exceptions, utils
│       ├── identity/                 # Users, Roles, Permissions, Auth
│       ├── workshop/                 # Vehicles, Repair Jobs, Damage, QC
│       ├── procurement/              # Requisitions, Quotations, Orders
│       ├── purchasing/               # Suppliers, Supplier POs
│       ├── inventory/                # Stock, Goods Receipts, Deliveries
│       ├── finance/                  # Invoices, Payments, Cashbook, GL
│       └── platform/                 # Audit Trail, Notifications
├── frontend/                         # React SPA
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/                      # Axios client + service layer
│       ├── components/layout/        # Sidebar, Topbar
│       ├── pages/                    # All domain pages
│       ├── store/                    # Zustand auth store
│       ├── types/                    # TypeScript interfaces
│       └── styles/                   # Global CSS design system
├── database/
│   └── init.sql                      # Full schema + extensions + seed data
└── docker-compose.yml                # PostgreSQL container
```

## Domain Modules

### Workshop (AV Motors)
- Vehicles & customer management
- Repair job case files with stage tracking
- Damage assessments with zone mapping
- Repair operations and technician allocation
- Quality control gates
- Job-linked photos and documents

### Procurement
- Spare parts requisitions with amendment control
- Quotation versions and negotiation threads
- Approval workflows with amount authority
- Order conversion from approved quotations
- Order amendments (immutable approved records)

### SMB Purchasing (Confidential)
- Supplier directory
- Supplier purchase orders
- Supplier quote comparison
- Freight cost allocation

### Inventory & Fulfilment
- Stock management with reservation tracking
- Goods receipt and inspection
- Delivery notes and AV receipt confirmation
- Stock movements (receipt, issue, transfer, adjustment)

### Finance
- **Invoices** — with three-way verification (order ↔ receipt ↔ invoice)
- **Payments** — linked to invoices, auto-updates balances
- **Cashbook** — bank/cash/mobile money with reconciliation
- **General Ledger** — double-entry chart of accounts
- **Manual Journals** — server-validated balanced entries
- **Debtor/Creditor Ledger** — ageing and control accounts

### Platform Controls
- Immutable audit trail (PostgreSQL trigger-based)
- Notification system
- Document number sequencing
- Strict RBAC with field-level security

## Security Model

- **Deny-by-default RBAC** — AV users cannot see SMB supplier costs, creditor balances, banking or margins
- **Permission-enforced endpoints** — every module endpoint validates the caller's role against the permission matrix (`PermissionService`); unauthenticated or unauthorised calls get 401/403
- **Organisation-scoped data** — every query filtered by authenticated user's organisation, including file downloads
- **Immutable audit log** — append-only via PostgreSQL triggers
- **JWT auth** — stateless access tokens (1 h) + refresh tokens (24 h); refresh tokens are rejected as access tokens
- **Account lockout** — configurable failed attempts (default 5, prod 3) → configurable lock (default 15 min, prod 30 min)
- **Login rate limiting** — per-client-IP throttling on `/auth/login`
- **Password hashing** — bcrypt (cost 12) via pgcrypto
- **Upload hardening** — MIME-type allowlist, 25 MB cap, filename sanitisation, per-organisation storage isolation

## Quick Start

### 1. Start PostgreSQL
```bash
docker-compose up -d
```

### 2. Start Backend
```bash
cd backend
mvn spring-boot:run
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Login
- **AV Admin**: `admin@avmotors.com` / `Admin@123`
- **SMB Admin**: `admin@smbprocurement.com` / `Admin@123`

> Development only. The `prod` profile refuses to start while these defaults are in place — see *Production Deployment* below.

## Production Deployment

### 1. Configure the environment
```bash
cp .env.example .env
```
Then set **at minimum** (the stack will not start without them):

| Variable | Notes |
|----------|-------|
| `DB_PASSWORD` | Strong, unique. The dev default is rejected in prod. |
| `JWT_SECRET` | `openssl rand -base64 64`. Min 32 bytes; dev default rejected in prod. |
| `CORS_ORIGINS` | Your production origins only — `localhost` entries are rejected in prod. |
| `SEED_ADMIN_PASSWORD` | Initial password for the seeded admin accounts (applied by Flyway migration V2). Avoid single quotes. |

The backend runs with the `prod` profile by default in Docker, which additionally:
- disables Swagger/OpenAPI endpoints
- hides actuator health details and stack traces
- tightens lockout (3 attempts / 30 min)
- honours `X-Forwarded-*` headers from the reverse proxy
- validates on startup (`ProductionConfigGuard`) that secrets are set and non-default

### 2. Build and start
```bash
docker compose up -d --build
```
The backend image is a self-contained multi-stage build — no local Maven/JDK required. PostgreSQL is published on `127.0.0.1` only; expose/tunnel it explicitly if remote access is needed.

### 3. After first boot
- Rotate the seeded admin passwords (or set `SEED_ADMIN_PASSWORD` before the first migration run) and create real user accounts.
- Terminate TLS at your edge/reverse proxy in front of the frontend container (HSTS headers are already emitted).
- Scrape `/api/actuator/prometheus` (authenticated) for metrics; `/api/actuator/health` is public for liveness probes.

### CI/CD
GitHub Actions (`.github/workflows/ci.yml`) runs on `master`: backend tests (against a Postgres service), frontend lint/build/tests, Trivy vulnerability scan, Docker image build and a containerised smoke test, then a gated `production` environment deploy step to configure with your target.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate |
| POST | `/api/auth/refresh` | Exchange refresh token for new tokens |
| GET | `/api/auth/me` | Current user profile |
| GET/POST | `/api/vehicles` | Vehicle CRUD |
| GET/POST | `/api/repair-jobs` | Repair job CRUD |
| GET/POST | `/api/requisitions` | Requisition CRUD |
| POST | `/api/requisitions/:id/submit` | Submit requisition |
| GET/POST | `/api/quotations` | Quotation CRUD |
| POST | `/api/quotations/:id/approve` | Approve quotation |
| POST | `/api/quotations/:id/convert-to-order` | Convert to order |
| GET/POST | `/api/orders` | Order CRUD |
| GET/POST | `/api/suppliers` | Supplier CRUD |
| GET/POST | `/api/supplier-pos` | Supplier PO CRUD |
| GET/POST | `/api/inventory` | Inventory CRUD |
| GET/POST | `/api/delivery-notes` | Delivery CRUD |
| GET/POST | `/api/invoices` | Invoice CRUD |
| GET/POST | `/api/payments` | Payment CRUD |
| GET/POST | `/api/cashbook` | Cashbook entries |
| GET/POST | `/api/journals` | Journal CRUD |
| POST | `/api/journals/:id/post` | Post journal (double-entry validated) |
| GET | `/api/chart-of-accounts` | Chart of accounts |
| GET | `/api/audit` | Audit trail |
| GET | `/api/notifications` | User notifications |
