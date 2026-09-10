#!/usr/bin/env bash
# ============================================================================
# AV Motors x SMB Procurement OS — Podman deployment
#
# Deploys the full stack (PostgreSQL, Spring Boot backend, nginx frontend)
# on Podman without needing a compose provider. Mirrors docker-compose.yml.
#
# Usage:
#   scripts/podman-deploy.sh up      # build images + start the stack
#   scripts/podman-deploy.sh down    # stop and remove containers (keeps data)
#   scripts/podman-deploy.sh destroy # down + remove volumes (DESTRUCTIVE)
#
# Configuration (override via environment or .env):
#   DB_PASSWORD, JWT_SECRET, SEED_ADMIN_PASSWORD, CORS_ORIGINS,
#   FRONTEND_PORT (default 3080), BACKEND_PORT (default 4000)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

NET=av-smb-net
VOL_PG=av-smb-pgdata
VOL_UPLOADS=av-smb-uploads
VOL_LOGS=av-smb-logs
BACKEND_IMAGE=av-smb-backend:1.0.0
FRONTEND_IMAGE=av-smb-frontend:1.0.0

FRONTEND_PORT="${FRONTEND_PORT:-3080}"
BACKEND_PORT="${BACKEND_PORT:-4000}"

# Load .env if present (same file used by docker compose deployments)
if [ -f "$ROOT/.env" ]; then
  set -a; . "$ROOT/.env"; set +a
fi

# The prod profile refuses to start with defaults; require real values here.
: "${DB_PASSWORD:?Set DB_PASSWORD (env or .env) — see .env.example}"
: "${JWT_SECRET:?Set JWT_SECRET (env or .env) — generate with: openssl rand -base64 64}"
: "${SEED_ADMIN_PASSWORD:=Admin@123}"
: "${CORS_ORIGINS:=http://av-smb.local}"

# ── WSL machine guard: DNS inside the podman machine is sometimes missing ──
if podman machine list --format "{{.Name}}" 2>/dev/null | grep -q . && ! podman machine ssh "test -f /etc/resolv.conf" 2>/dev/null; then
  echo ">> /etc/resolv.conf missing in podman machine — repairing DNS"
  podman machine ssh "printf 'nameserver 8.8.8.8\nnameserver 1.1.1.1\n' | sudo tee /etc/resolv.conf >/dev/null"
fi

cmd="${1:-up}"

down() {
  podman rm -f av-smb-frontend av-smb-backend av-smb-db >/dev/null 2>&1 || true
}

case "$cmd" in
  up)
    echo ">> Building backend image (multi-stage, self-contained)…"
    podman build -t "$BACKEND_IMAGE" "$ROOT/backend"
    echo ">> Building frontend image…"
    podman build -t "$FRONTEND_IMAGE" "$ROOT/frontend"

    podman network create "$NET" >/dev/null 2>&1 || true
    podman volume create "$VOL_PG" >/dev/null 2>&1 || true
    podman volume create "$VOL_UPLOADS" >/dev/null 2>&1 || true
    podman volume create "$VOL_LOGS" >/dev/null 2>&1 || true

    down

    echo ">> Starting PostgreSQL…"
    podman run -d --name av-smb-db \
      --network "$NET" \
      -e POSTGRES_DB=avsmc_procurement \
      -e POSTGRES_USER=avsmc_admin \
      -e POSTGRES_PASSWORD="$DB_PASSWORD" \
      -v "$VOL_PG:/var/lib/postgresql/data" \
      --health-cmd "pg_isready -U avsmc_admin -d avsmc_procurement" \
      --health-interval 5s --health-timeout 5s --health-retries 20 \
      docker.io/library/postgres:16-alpine \
      postgres -c max_connections=200 -c shared_buffers=256MB -c effective_cache_size=768MB

    echo ">> Waiting for PostgreSQL health…"
    for _ in $(seq 1 60); do
      [ "$(podman inspect -f '{{.State.Health.Status}}' av-smb-db 2>/dev/null)" = "healthy" ] && break
      sleep 2
    done
    [ "$(podman inspect -f '{{.State.Health.Status}}' av-smb-db)" = "healthy" ] || {
      podman logs av-smb-db | tail -30; echo "PostgreSQL never became healthy"; exit 1; }

    echo ">> Starting backend (prod profile)…"
    podman run -d --name av-smb-backend \
      --network "$NET" --network-alias backend \
      -p "127.0.0.1:${BACKEND_PORT}:4000" \
      -e SPRING_PROFILES_ACTIVE=prod \
      -e DB_URL=jdbc:postgresql://av-smb-db:5432/avsmc_procurement \
      -e DB_USERNAME=avsmc_admin \
      -e DB_PASSWORD="$DB_PASSWORD" \
      -e JWT_SECRET="$JWT_SECRET" \
      -e SEED_ADMIN_PASSWORD="$SEED_ADMIN_PASSWORD" \
      -e CORS_ORIGINS="$CORS_ORIGINS" \
      -e SERVER_PORT=4000 \
      -v "$VOL_UPLOADS:/app/uploads" \
      -v "$VOL_LOGS:/app/logs" \
      "$BACKEND_IMAGE"

    echo ">> Starting frontend (nginx, proxies /api to backend)…"
    podman run -d --name av-smb-frontend \
      --network "$NET" \
      -p "127.0.0.1:${FRONTEND_PORT}:80" \
      "$FRONTEND_IMAGE"

    echo ">> Waiting for backend health (first boot runs Flyway migrations)…"
    for _ in $(seq 1 60); do
      if curl -sf "http://localhost:${BACKEND_PORT}/api/actuator/health" >/dev/null 2>&1; then break; fi
      sleep 3
    done
    curl -sf "http://localhost:${BACKEND_PORT}/api/actuator/health" >/dev/null || {
      podman logs av-smb-backend | tail -40; echo "Backend never became healthy"; exit 1; }

    echo
    echo "Deployment ready:"
    echo "  Frontend : http://localhost:${FRONTEND_PORT}"
    echo "  Backend  : http://localhost:${BACKEND_PORT}/api/actuator/health"
    echo "  Admins   : admin@avmotors.com / admin@smbprocurement.com (password: SEED_ADMIN_PASSWORD)"
    ;;
  down)
    down
    echo ">> Stack stopped (volumes kept). Run 'up' to redeploy, 'destroy' to wipe data."
    ;;
  destroy)
    down
    podman volume rm "$VOL_PG" "$VOL_UPLOADS" "$VOL_LOGS" >/dev/null 2>&1 || true
    echo ">> Stack removed including data volumes."
    ;;
  *)
    echo "Usage: $0 {up|down|destroy}" >&2; exit 2 ;;
esac
