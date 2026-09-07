# Production deployment — proc.coresign.africa

## Topology

```
browser → Traefik (79.143.188.3, TLS + Let's Encrypt)
        → 173.249.38.38:8181            (ufw: only from the Traefik host)
        → frontend container (nginx)
              ├── /      → React SPA (static bundle, history fallback)
              └── /api/  → Spring Boot (backend:4000, context-path /api)
        → Postgres 16 (internal compose network only)
```

Everything lives in `/opt/av-smb-procurement` on the app host.
`.env.production` is written by the deploy workflow and is `chmod 600`; it is
never in git.

## Continuous deployment

`push to main/master` → **CI/CD Pipeline** (`.github/workflows/ci.yml`) → on
success **Deploy** (`.github/workflows/deploy.yml`) runs automatically. There
is no approval gate; a green build ships.

Deploy steps: rsync the tree to `/opt/av-smb-procurement`, write
`.env.production` from repository secrets, open the edge port to Traefik in
ufw, `docker compose up -d --build`, install the Traefik route, then poll
`https://proc.coresign.africa/api/actuator/health` for up to 6 minutes. A
failed smoke test fails the job and dumps `docker compose ps` plus the last
120 log lines of `backend` and `frontend`.

Traefik's file provider runs with `--providers.file.watch=true`, so dropping
the route file is enough — the proxy is never restarted.

## Required repository secrets

| Secret | Purpose |
| --- | --- |
| `DEPLOY_SSH_KEY` | private key authorised for `root` on both hosts |
| `DEPLOY_HOST` | `173.249.38.38` |
| `TRAEFIK_HOST` | `79.143.188.3` |
| `PROD_DB_PASSWORD` | Postgres password |
| `PROD_JWT_SECRET` | `openssl rand -base64 64` |
| `PROD_SEED_ADMIN_PASSWORD` | password applied to the seeded admin accounts |

`ProductionConfigGuard` fails startup if the JWT secret, DB password or CORS
origins are missing or left at a development default, so a misconfigured
rollout stops at the health gate rather than going live.

## Seeded accounts

The V2 Flyway migration creates `admin@avmotors.com` and
`admin@smbprocurement.com` with `SEED_ADMIN_PASSWORD`. **Change both from the
UI after the first deploy.** Avoid `'` in that value — it is interpolated into
SQL by the migration.

## Manual operations

Run from `/opt/av-smb-procurement` on the app host:

```bash
COMPOSE="docker compose -p av-smb-prod -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.production"

$COMPOSE ps
$COMPOSE logs -f backend
$COMPOSE exec -T postgres pg_dump -U avsmc_admin avsmc_procurement | gzip > backup-$(date +%F).sql.gz
```

## Notes

- Only the frontend container is published on the host. The frontend nginx
  passes Traefik's `X-Forwarded-Proto` through instead of overwriting it with
  `$scheme`, so Spring's `forward-headers-strategy` builds correct absolute
  URLs behind the proxy.
- `npm ci` in the frontend image retries on a slow registry. The stock npm
  timeouts give up with `EIDLETIMEOUT`, which fails an unattended deploy for
  no code reason.
- CI has no deploy job. `deploy.yml` triggers on `workflow_run` for the
  CI/CD Pipeline workflow. A placeholder job that declared
  `environment: production` was removed: with required reviewers on that
  environment the run sits pending, and `workflow_run` never fires for a run
  that has not concluded, so it would have blocked the real deploy.

## Port map on 173.249.38.38

| Port | Project | Domain |
| --- | --- | --- |
| 3001 / 3002 | coresign | sign.airzim / api.sign.airzim |
| 8180 | form700 | forms.coresign.africa |
| 8181 | **this project** | proc.coresign.africa |
| 8182 | novashop | nova.coresign.africa |
| 8280 | isqsp | isqsp.coresign.africa |
