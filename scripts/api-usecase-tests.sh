#!/usr/bin/env bash
# ============================================================================
# AV Motors x SMB Procurement OS — full API use-case test suite
#
# Exercises every module end to end against a running deployment:
# auth/identity, workshop, procurement, purchasing, inventory, finance,
# platform (dashboard, reports, audit, notifications, files, compliance) —
# plus security negatives (401, RBAC 403, cross-tenant isolation, lockout).
#
# Usage:
#   BASE_URL=http://localhost:3080 ADMIN_PASSWORD=... scripts/api-usecase-tests.sh
#
# BASE_URL  defaults to http://localhost:3080 (through nginx, like the browser)
# ADMIN_PASSWORD defaults to Admin@123 (the seeded dev value)
# ============================================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:3080}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
API="$BASE_URL/api"

PASS=0; FAIL=0; FAILED_TESTS=()

ok()   { PASS=$((PASS+1)); echo "  PASS  $1"; }
fail() { FAIL=$((FAIL+1)); FAILED_TESTS+=("$1"); echo "  FAIL  $1${2:+ — $2}"; }

expect() { # desc expected_status actual_status [extra]
  if [ "$2" = "$3" ]; then ok "$1 ($3)"; else fail "$1" "expected $2, got $3${4:+ | $4}"; fi
}

json() { jq -r "$1" <<<"$2" 2>/dev/null; }

req() { # method path token data -> sets STATUS and BODY
  local method="$1" path="$2" token="${3:-}" data="${4:-}"
  local args=(-s -w "\n%{http_code}" -X "$method" "$API$path" -H "Content-Type: application/json")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-d "$data")
  local out; out="$(curl "${args[@]}" 2>&1)"
  STATUS="${out##*$'\n'}"; BODY="${out%$'\n'*}"
}

section() { echo; echo "══ $1 ══"; }

# Two-step login: password -> OTP challenge -> tokens (test code: 123456).
# Leaves STATUS/BODY on the final auth response; unwraps .user so downstream
# field extraction (.accessToken, .orgType, .roleCode, …) keeps working.
OTP_CODE="${OTP_CODE:-123456}"
login() { # email password
  req POST /auth/login "" '{"email":"'"$1"'","password":"'"$2"'"}'
  if [ "$STATUS" = "200" ] && [ "$(json .requiresOtp "$BODY")" = "true" ]; then
    local uid; uid=$(json .userId "$BODY")
    req POST /auth/login/verify "" '{"userId":"'"$uid"'","otpCode":"'"$OTP_CODE"'"}'
    if [ "$STATUS" = "200" ]; then BODY=$(json .user "$BODY"); fi
  fi
}

RUN_TAG=$RANDOM
echo "AV SMB Procurement OS — use-case suite against $BASE_URL (run $RUN_TAG)"

# ═══════════════════════════ 1. AUTH & IDENTITY ═══════════════════════════
section "Auth & Identity"

login admin@avmotors.com "$ADMIN_PASSWORD"
expect "AV admin login" 200 "$STATUS" "${BODY:0:120}"
AV_TOKEN=$(json .accessToken "$BODY"); AV_REFRESH=$(json .refreshToken "$BODY")
AV_ORG=$(json .organisationId "$BODY")
[ "$AV_TOKEN" != "null" ] && [ -n "$AV_TOKEN" ] && ok "access token issued" || fail "access token issued"

login admin@smbprocurement.com "$ADMIN_PASSWORD"
expect "SMB admin login" 200 "$STATUS" "${BODY:0:120}"
SMB_TOKEN=$(json .accessToken "$BODY"); SMB_REFRESH=$(json .refreshToken "$BODY")
[ "$(json .orgType "$BODY")" = "SUPPLIER" ] && ok "SMB org type = SUPPLIER" || fail "SMB org type" "$(json .orgType "$BODY")"

req POST /auth/login "" '{"email":"admin@avmotors.com","password":"wrong-password"}'
expect "wrong password rejected" 401 "$STATUS"

req GET /vehicles "" ""
expect "unauthenticated request rejected" 401 "$STATUS"

req GET /vehicles "garbage.token.here" ""
expect "malformed token rejected" 401 "$STATUS"

req GET /auth/me "$AV_TOKEN" ""
expect "GET /auth/me" 200 "$STATUS"
[ "$(json .email "$BODY")" = "admin@avmotors.com" ] && ok "/auth/me returns caller" || fail "/auth/me identity" "$(json .email "$BODY")"

# Refresh flow
req POST /auth/refresh "" '{"refreshToken":"'"$AV_REFRESH"'"}'
expect "refresh token exchange" 200 "$STATUS" "${BODY:0:120}"
NEW_AV_TOKEN=$(json .accessToken "$BODY")
req GET /auth/me "$NEW_AV_TOKEN" ""
expect "new access token works" 200 "$STATUS"

# Refresh token must NOT be usable as an access token
req GET /auth/me "$AV_REFRESH" ""
expect "refresh token rejected as access token" 401 "$STATUS"

# Seeded users & roles management
req GET /auth/users "$AV_TOKEN" ""
expect "list users (admin)" 200 "$STATUS"
req GET /auth/roles "$AV_TOKEN" ""
expect "list roles (admin)" 200 "$STATUS"
AV_ROLE_ID=$(json '.[] | select(.code=="AV_TECHNICIAN") | .id' "$BODY")
[ -n "$AV_ROLE_ID" ] && [ "$AV_ROLE_ID" != "null" ] && ok "technician role resolved" || fail "technician role resolved" "${BODY:0:120}"

# Create a low-privilege user and log in as them
req POST /auth/users "$AV_TOKEN" '{"email":"tech-'"$RUN_TAG"'@avmotors.com","password":"Tech@12345","firstName":"Tec","lastName":"Hnician","phone":"+263778000111","roleIds":["'"$AV_ROLE_ID"'"]}'
CREATE_USER_STATUS="$STATUS"
if [ "$CREATE_USER_STATUS" = "200" ]; then
  ok "create technician user (role: $(json '.roles[0].code' "$BODY" 2>/dev/null || echo '?'))"
  login "tech-$RUN_TAG@avmotors.com" "Tech@12345"
  TECH_TOKEN=$(json .accessToken "$BODY")
  [ "$(json .roleCode "$BODY")" != "null" ] && ok "new user can log in as $(json .roleCode "$BODY")" || fail "new user login" "${BODY:0:120}"
  req POST /vehicles "$TECH_TOKEN" '{"registration":"TECH1","make":"Toyota","model":"Hilux"}'
  expect "low-privilege user denied vehicles.manage (403)" 403 "$STATUS"
  req GET /vehicles "$TECH_TOKEN" ""
  expect "low-privilege user allowed vehicles.view" 200 "$STATUS"
else
  fail "create technician user" "status $CREATE_USER_STATUS ${BODY:0:160}"
  TECH_TOKEN=""
fi

# Weak password rejected
req POST /auth/users "$AV_TOKEN" '{"email":"weak@avmotors.com","password":"short","firstName":"W","lastName":"C","roleIds":["'"$AV_ROLE_ID"'"]}'
expect "weak password rejected (422)" 422 "$STATUS"

# Account lockout (prod policy: 3 attempts / 30 min) — use a disposable user
req POST /auth/users "$AV_TOKEN" '{"email":"locked-'"$RUN_TAG"'@avmotors.com","password":"Locked@123","firstName":"Lock","lastName":"Out","phone":"+263778000112","roleIds":["'"$AV_ROLE_ID"'"]}'
req POST /auth/login "" '{"email":"locked-'"$RUN_TAG"'@avmotors.com","password":"bad-pass-1"}' >/dev/null
req POST /auth/login "" '{"email":"locked-'"$RUN_TAG"'@avmotors.com","password":"bad-pass-2"}' >/dev/null
req POST /auth/login "" '{"email":"locked-'"$RUN_TAG"'@avmotors.com","password":"bad-pass-3"}' >/dev/null
req POST /auth/login "" '{"email":"locked-'"$RUN_TAG"'@avmotors.com","password":"Locked@123"}'
expect "account locked after repeated failures (423)" 423 "$STATUS"

# ═══════════════════════════ 2. WORKSHOP (AV) ═══════════════════════════
section "Workshop — vehicles & repair jobs"

req POST /vehicles "$AV_TOKEN" '{"registration":"AVH'+$RANDOM+'","make":"Toyota","model":"Land Cruiser","year":2021,"color":"White","vehicleType":"SUV","fuelType":"Diesel","vin":"JTMHV05J204'+$RANDOM'"}'
expect "create vehicle" 200 "$STATUS" "${BODY:0:120}"
VEHICLE_ID=$(json .id "$BODY")

req GET /vehicles "$AV_TOKEN" ""
expect "list vehicles" 200 "$STATUS"
req GET "/vehicles/$VEHICLE_ID" "$AV_TOKEN" ""
expect "get vehicle by id" 200 "$STATUS"
[ "$(json .registration "$BODY")" != "null" ] && ok "vehicle fetch matches" || fail "vehicle fetch" "${BODY:0:120}"

req POST /vehicles "$AV_TOKEN" '{"registration":"","make":"X","model":"Y"}'
expect "vehicle validation (blank registration → 400)" 400 "$STATUS"

req POST /repair-jobs "$AV_TOKEN" '{"vehicleId":"'"$VEHICLE_ID"'","title":"Panel beating - front left door","jobType":"INSURANCE","priority":"HIGH","bookedHours":12,"labourRate":45.00}'
expect "create repair job" 200 "$STATUS" "${BODY:0:120}"
JOB_ID=$(json .id "$BODY")

req GET "/repair-jobs/$JOB_ID" "$AV_TOKEN" ""
expect "get repair job" 200 "$STATUS"

req PATCH "/repair-jobs/$JOB_ID/status" "$AV_TOKEN" '{"status":"IN_PROGRESS","notes":"Work started"}'
expect "update job status" 200 "$STATUS"

req POST "/repair-jobs/$JOB_ID/damage-assessments" "$AV_TOKEN" '{"zone":"FRONT_LEFT_DOOR","severity":"MODERATE","description":"Deep dent plus paint damage","repairMethod":"PDR","estimatedCost":850.00}'
expect "add damage assessment" 200 "$STATUS" "${BODY:0:120}"

req POST "/repair-jobs/$JOB_ID/operations" "$AV_TOKEN" '{"operationName":"Door skin replacement","operationType":"PANEL","bookedHours":4,"sequenceOrder":1,"status":"PENDING"}'
expect "add repair operation" 200 "$STATUS" "${BODY:0:120}"

# Cross-workspace negative: SMB user cannot manage AV vehicles
req POST /vehicles "$SMB_TOKEN" '{"registration":"SMB1","make":"Nissan","model":"NP300"}'
expect "SMB user denied AV workshop (403)" 403 "$STATUS"

# ═══════════════════════════ 3. PROCUREMENT (AV) ═══════════════════════════
section "Procurement — requisitions, quotations, orders"

req POST /requisitions "$AV_TOKEN" '{"repairJobId":"'"$JOB_ID"'","title":"Parts for Land Cruiser door repair","priority":"HIGH","items":[{"partNumber":"DOOR-LC-001","description":"Front left door skin","quantity":1,"unitOfMeasure":"EACH","estimatedCost":450.00},{"partNumber":"PAINT-WH-5L","description":"Pearl white paint 5L","quantity":2,"unitOfMeasure":"LITRE","estimatedCost":120.00}]}'
expect "create requisition with items" 200 "$STATUS" "${BODY:0:120}"
REQ_ID=$(json .id "$BODY")

req POST "/requisitions/$REQ_ID/submit" "$AV_TOKEN" ''
expect "submit requisition" 200 "$STATUS"
[ "$(json .status "$BODY")" != "DRAFT" ] && ok "requisition left DRAFT (now $(json .status "$BODY"))" || fail "requisition submit status" "$(json .status "$BODY")"

req POST "/requisitions/$REQ_ID/submit" "$AV_TOKEN" ''
expect "double-submit rejected (422)" 422 "$STATUS"

req GET /requisitions "$AV_TOKEN" ""
expect "list requisitions" 200 "$STATUS"

req POST /quotations "$AV_TOKEN" '{"requisitionId":"'"$REQ_ID"'","title":"SMB quote - door repair parts","taxRate":15.0,"validUntil":"2027-06-30","items":[{"partNumber":"DOOR-LC-001","description":"Front left door skin","quantity":1,"unitPrice":465.00},{"partNumber":"PAINT-WH-5L","description":"Pearl white paint 5L","quantity":2,"unitPrice":118.50}]}'
expect "create quotation" 200 "$STATUS" "${BODY:0:120}"
QUOTE_ID=$(json .id "$BODY")

req GET /quotations "$AV_TOKEN" ""
expect "list quotations" 200 "$STATUS"

req POST "/quotations/$QUOTE_ID/approve" "$AV_TOKEN" ''
expect "approve quotation" 200 "$STATUS"

req POST "/quotations/$QUOTE_ID/convert-to-order" "$AV_TOKEN" ''
expect "convert approved quotation to order" 200 "$STATUS" "${BODY:0:120}"
ORDER_ID=$(json .id "$BODY")
[ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ] && ok "order created ($ORDER_ID)" || fail "order id missing" "${BODY:0:120}"

req GET "/orders/$ORDER_ID" "$AV_TOKEN" ""
expect "get order by id" 200 "$STATUS"
req GET /orders "$AV_TOKEN" ""
expect "list orders" 200 "$STATUS"

req POST "/quotations/$QUOTE_ID/convert-to-order" "$AV_TOKEN" ''
expect "re-converting same quotation rejected (422)" 422 "$STATUS"

# ═══════════════════════════ 4. PURCHASING (SMB) ═══════════════════════════
section "Purchasing — suppliers & supplier POs"

req POST /suppliers "$SMB_TOKEN" '{"name":"Test Parts Co","contactPerson":"Supplier Rep","email":"sales@testparts.example","phone":"+263771000111","paymentTerms":30,"currency":"USD"}'
expect "create supplier" 200 "$STATUS" "${BODY:0:120}"
SUPPLIER_ID=$(json .id "$BODY")

req GET /suppliers "$SMB_TOKEN" ""
expect "list suppliers" 200 "$STATUS"

req POST /supplier-pos "$SMB_TOKEN" '{"supplierId":"'"$SUPPLIER_ID"'","avOrderId":"'"$ORDER_ID"'","expectedDelivery":"2026-10-01","items":[{"description":"Front left door skin","quantity":1,"unitCost":440.00,"freightAlloc":15.00},{"description":"Pearl white paint 5L","quantity":2,"unitCost":110.00,"freightAlloc":5.00}]}'
expect "create supplier PO" 200 "$STATUS" "${BODY:0:120}"
PO_ID=$(json .id "$BODY")

req GET /supplier-pos "$SMB_TOKEN" ""
expect "list supplier POs" 200 "$STATUS"

# Cross-workspace negative: AV user cannot manage suppliers
req POST /suppliers "$AV_TOKEN" '{"name":"AV should fail"}'
expect "AV user denied SMB purchasing (403)" 403 "$STATUS"

# ═══════════════════════════ 5. INVENTORY & FULFILMENT (SMB) ═══════════════════════════
section "Inventory — stock, deliveries, goods receipts"

req POST /inventory "$SMB_TOKEN" '{"partNumber":"DOOR-LC-'"$RUN_TAG"'","description":"Front left door skin","quantityOnHand":5,"reorderLevel":2,"unitCost":440.00,"sellingPrice":465.00,"category":"BODY"}'
expect "create inventory item" 200 "$STATUS" "${BODY:0:120}"
ITEM_ID=$(json .id "$BODY")

req GET /inventory "$SMB_TOKEN" ""
expect "list inventory" 200 "$STATUS"

req POST /inventory "$SMB_TOKEN" '{"partNumber":"","description":"bad item"}'
expect "inventory validation (blank part number → 400)" 400 "$STATUS"

req POST /delivery-notes "$SMB_TOKEN" '{"avOrderId":"'"$ORDER_ID"'","deliveryDate":"2026-09-10","deliveredBy":"Driver T","vehicleReg":"SMB TRK 1","notes":"Delivered to AV Motors"}'
expect "create delivery note against AV order" 200 "$STATUS" "${BODY:0:120}"

req GET /delivery-notes "$SMB_TOKEN" ""
expect "list delivery notes" 200 "$STATUS"

req GET /goods-receipts "$SMB_TOKEN" ""
expect "list goods receipts" 200 "$STATUS"

# ═══════════════════════════ 6. FINANCE (SMB) ═══════════════════════════
section "Finance — invoices, payments, cashbook, journals, GL"

req GET /chart-of-accounts "$SMB_TOKEN" ""
expect "chart of accounts seeded" 200 "$STATUS"
ACCOUNTS="$BODY"
ACC1=$(json '.[0].id' "$ACCOUNTS"); ACC2=$(json '.[1].id' "$ACCOUNTS")
[ "$(json 'length' "$ACCOUNTS")" -ge 5 ] 2>/dev/null && ok "seeded GL accounts ($(json 'length' "$ACCOUNTS"))" || fail "seeded GL accounts" "${ACCOUNTS:0:120}"

req GET /cash-accounts "$SMB_TOKEN" ""
expect "cash accounts list" 200 "$STATUS"
CASH_ACCOUNT=$(json '.[0].id' "$BODY")

req POST /invoices "$SMB_TOKEN" '{"avOrderId":"'"$ORDER_ID"'","fromEntity":"SMB Procurement","toEntity":"AV Motors","taxRate":15.0,"dueDate":"2026-10-15","items":[{"orderItemId":null,"description":"Front left door skin","quantity":1,"unitPrice":465.00},{"description":"Pearl white paint 5L","quantity":2,"unitPrice":118.50}]}'
expect "create invoice with items" 200 "$STATUS" "${BODY:0:120}"
INVOICE_ID=$(json .id "$BODY")

req GET /invoices "$SMB_TOKEN" ""
expect "list invoices" 200 "$STATUS"

req POST /payments "$SMB_TOKEN" '{"invoiceId":"'"$INVOICE_ID"'","paymentType":"SUPPLIER_INVOICE","amount":702.00,"paymentMethod":"BANK_TRANSFER","reference":"PAY-0001","cashAccountId":"'"$CASH_ACCOUNT"'"}'
expect "create payment against invoice" 200 "$STATUS" "${BODY:0:120}"

req GET /payments "$SMB_TOKEN" ""
expect "list payments" 200 "$STATUS"

req POST /cashbook "$SMB_TOKEN" '{"cashAccountId":"'"$CASH_ACCOUNT"'","entryType":"PAYMENT","amount":702.00,"counterparty":"AV Motors","narration":"Invoice settlement","reference":"PAY-0001"}'
expect "create cashbook entry" 200 "$STATUS" "${BODY:0:120}"

req GET /cashbook "$SMB_TOKEN" ""
expect "list cashbook entries" 200 "$STATUS"

req POST /journals "$SMB_TOKEN" '{"journalType":"MANUAL","description":"Test balanced journal","lines":[{"accountId":"'"$ACC1"'","debitAmount":100.00,"creditAmount":0},{"accountId":"'"$ACC2"'","debitAmount":0,"creditAmount":100.00}]}'
expect "create balanced journal" 200 "$STATUS" "${BODY:0:120}"
JOURNAL_ID=$(json .id "$BODY")

req POST "/journals/$JOURNAL_ID/post" "$SMB_TOKEN" ''
expect "post journal" 200 "$STATUS"

req POST "/journals/$JOURNAL_ID/post" "$SMB_TOKEN" ''
expect "double-post journal rejected (422)" 422 "$STATUS"

req POST /journals "$SMB_TOKEN" '{"journalType":"MANUAL","description":"Unbalanced","lines":[{"accountId":"'"$ACC1"'","debitAmount":100.00,"creditAmount":0},{"accountId":"'"$ACC2"'","debitAmount":0,"creditAmount":90.00}]}'
expect "unbalanced journal rejected (422)" 422 "$STATUS"

req GET /journals "$SMB_TOKEN" ""
expect "list journals" 200 "$STATUS"

# RBAC: AV admin has no finance-posting permission
req POST /journals "$AV_TOKEN" '{"journalType":"MANUAL","description":"AV should not post","lines":[{"accountId":"'"$ACC1"'","debitAmount":1,"creditAmount":0},{"accountId":"'"$ACC2"'","debitAmount":0,"creditAmount":1}]}'
expect "AV user denied journal posting (403)" 403 "$STATUS"

# ═══════════════════════════ 7. PLATFORM ═══════════════════════════
section "Platform — dashboard, reports, audit, notifications, files, compliance"

req GET /dashboard/stats "$AV_TOKEN" ""
expect "dashboard stats" 200 "$STATUS" "${BODY:0:120}"
req GET /dashboard/finance-summary "$SMB_TOKEN" ""
expect "finance summary" 200 "$STATUS" "${BODY:0:120}"

for r in trial-balance debtor-ageing job-cost inventory-valuation procurement-pipeline; do
  req GET "/reports/$r" "$SMB_TOKEN" ""
  expect "report: $r" 200 "$STATUS" "${BODY:0:120}"
done
req GET "/reports/csv/invoices" "$SMB_TOKEN" ""
expect "report: CSV export (invoices)" 200 "$STATUS"
req GET "/reports/csv/not-a-report" "$SMB_TOKEN" ""
expect "report: unknown CSV type rejected (400)" 400 "$STATUS"

req GET "/audit?limit=10" "$AV_TOKEN" ""
expect "audit trail (AV)" 200 "$STATUS"
AUDIT_COUNT=$(json 'length' "$BODY")
[ "${AUDIT_COUNT:-0}" -gt 0 ] 2>/dev/null && ok "audit trail populated ($AUDIT_COUNT entries)" || fail "audit trail populated" "${BODY:0:120}"

req GET /notifications "$AV_TOKEN" ""
expect "list notifications" 200 "$STATUS"
req GET /notifications/count "$AV_TOKEN" ""
expect "unread notification count" 200 "$STATUS"

# File upload / download / cross-tenant isolation
printf 'part,qty\nDOOR-LC-001,1\n' > av-smb-upload-test.csv
UP_RESPONSE="$(curl -s -w "\n%{http_code}" -X POST "$API/files/upload" \
  -H "Authorization: Bearer $AV_TOKEN" \
  -F "file=@av-smb-upload-test.csv;type=text/csv" \
  -F "entityType=REQUISITION" -F "entityId=$REQ_ID")"
UP_STATUS="${UP_RESPONSE##*$'\n'}"; UP_BODY="${UP_RESPONSE%$'\n'*}"
expect "file upload (text/csv)" 200 "$UP_STATUS" "${UP_BODY:0:120}"
FILE_ID=$(json .id "$UP_BODY")

req GET "/files/$FILE_ID" "$AV_TOKEN" ""
expect "download own-org file" 200 "$STATUS"

req GET "/files/$FILE_ID" "$SMB_TOKEN" ""
expect "cross-tenant file download denied (404)" 404 "$STATUS"

req GET "/files/entity/REQUISITION/$REQ_ID" "$AV_TOKEN" ""
expect "list files by entity" 200 "$STATUS"

# File-type allowlist
echo "<script>alert(1)</script>" > av-smb-upload-test.html
HTML_RESPONSE="$(curl -s -w "\n%{http_code}" -X POST "$API/files/upload" \
  -H "Authorization: Bearer $AV_TOKEN" \
  -F "file=@av-smb-upload-test.html;type=text/html")"
expect "HTML upload rejected (422)" 422 "${HTML_RESPONSE##*$'\n'}"

req GET /compliance/privacy-policy "$AV_TOKEN" ""
expect "compliance: privacy policy" 200 "$STATUS"
req GET /compliance/audit-retention "$AV_TOKEN" ""
expect "compliance: audit retention" 200 "$STATUS"
req GET /compliance/data-export "$AV_TOKEN" ""
expect "compliance: data export" 200 "$STATUS"

# ═══════════════════════════ 8. EDGE OF STACK ═══════════════════════════
section "Stack edge — nginx, health, unknown routes"

curl -sf "$BASE_URL/" >/dev/null && ok "frontend served by nginx (GET /)" || fail "frontend served by nginx"
curl -sf "$BASE_URL/api/actuator/health" | grep -q '"UP"' 2>/dev/null && ok "health through nginx proxy" || fail "health through nginx proxy"
req GET "/nonexistent-endpoint" "$AV_TOKEN" ""
expect "unknown API route → 404 JSON" 404 "$STATUS"
req POST /auth/login "" '{"email":"not-an-email","password":"x"}'
expect "login validation (bad email → 400)" 400 "$STATUS"

# ═══════════════════════════ SUMMARY ═══════════════════════════
echo
echo "══════════════════════════════════════════"
echo "  PASSED: $PASS    FAILED: $FAIL"
if [ "$FAIL" -gt 0 ]; then
  echo "  Failed tests:"; printf '    - %s\n' "${FAILED_TESTS[@]}"
  exit 1
fi
echo "  ALL USE-CASE TESTS PASSED ✔"
rm -f av-smb-upload-test.csv av-smb-upload-test.html
