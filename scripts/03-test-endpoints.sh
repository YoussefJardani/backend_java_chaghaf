#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
#  CHAGHAF — Phase 3 : Tests des endpoints après déploiement
# ═══════════════════════════════════════════════════════════════════

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../infra/.env.azure"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Fichier $ENV_FILE introuvable. Lancez d'abord 01 et 02."
  exit 1
fi
source "$ENV_FILE"

# Normaliser BASE_URL (avec ou sans https://)
BASE_URL="${APP_URL}"
[[ "$BASE_URL" != https://* ]] && BASE_URL="https://$BASE_URL"

echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║   CHAGHAF — Tests POST-DÉPLOIEMENT          ║"
echo "║   URL : $BASE_URL"
echo "╚══════════════════════════════════════════════╝"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'
pass() { echo -e "${GREEN}✅ PASS${NC} — $1"; }
fail() { echo -e "${RED}❌ FAIL${NC} — $1"; }
warn() { echo -e "${YELLOW}⚠️  WARN${NC} — $1"; }

# ── Test 1 : Health ──────────────────────────────────────────
echo "🧪 Test 1 : /actuator/health"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
[ "$STATUS" = "200" ] && pass "GET /actuator/health → $STATUS" || fail "GET /actuator/health → $STATUS"

# ── Test 2 : Auth Health ─────────────────────────────────────
echo ""
echo "🧪 Test 2 : /api/auth/health"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/auth/health")
[ "$STATUS" = "200" ] && pass "GET /api/auth/health → $STATUS" || fail "GET /api/auth/health → $STATUS"

# ── Test 3 : Catalogue public ─────────────────────────────────
echo ""
echo "🧪 Test 3 : /api/catalog (public)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/catalog")
[ "$STATUS" = "200" ] && pass "GET /api/catalog → $STATUS" || fail "GET /api/catalog → $STATUS"

# ── Test 4 : Packs abonnements (public) ──────────────────────
echo ""
echo "🧪 Test 4 : /api/subscriptions/packs (public)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/subscriptions/packs")
[ "$STATUS" = "200" ] && pass "GET /api/subscriptions/packs → $STATUS" || fail "GET /api/subscriptions/packs → $STATUS"

# ── Test 5 : Login admin (créé auto par DataInitializer) ─────
echo ""
echo "🧪 Test 5 : Login admin (admin@chaghaf.ma / admin123)"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@chaghaf.ma","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
  pass "POST /api/auth/login → Token obtenu"
  echo "   Token (50 premiers chars): ${TOKEN:0:50}..."
else
  fail "POST /api/auth/login → Pas de token"
  echo "   Réponse complète: $LOGIN_RESPONSE"
fi

# ── Test 6 : Endpoint protégé avec token ─────────────────────
if [ -n "$TOKEN" ]; then
  echo ""
  echo "🧪 Test 6 : /api/auth/me (avec token)"
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/auth/me")
  [ "$STATUS" = "200" ] && pass "GET /api/auth/me → $STATUS" || fail "GET /api/auth/me → $STATUS"
fi

# ── Test 7 : Boissons et Snacks (public) ─────────────────────
echo ""
echo "🧪 Test 7 : /api/boissons et /api/snacks/catalog (public)"
S1=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/boissons")
S2=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/snacks/catalog")
[ "$S1" = "200" ] && pass "GET /api/boissons → $S1" || fail "GET /api/boissons → $S1"
[ "$S2" = "200" ] && pass "GET /api/snacks/catalog → $S2" || fail "GET /api/snacks/catalog → $S2"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    RÉSUMÉ DES TESTS                         ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  App URL    : $BASE_URL"
echo "  Admin auto : admin@chaghaf.ma / admin123 (créé par DataInitializer)"
echo "  User auto  : user@chaghaf.ma  / user123"
echo ""
echo "  ➡️  Phase 4 : ./scripts/04-setup-github-secrets.sh"
echo ""
