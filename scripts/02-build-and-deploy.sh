#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
#  CHAGHAF — Phase 2 : Build Docker + Push ACR + Deploy Container App
#  Exécuter depuis la racine du projet (où se trouve chaghaf-monolith/)
# ═══════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../infra/.env.azure"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Fichier $ENV_FILE introuvable."
  echo "   Lancez d'abord ./scripts/01-create-resources.sh"
  exit 1
fi
source "$ENV_FILE"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   CHAGHAF AZURE DEPLOYMENT — PHASE 2    ║"
echo "║   Build → Push ACR → Deploy             ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── Vérifier qu'on est dans le bon dossier ────────────────────
if [ ! -f "chaghaf-monolith/pom.xml" ]; then
  echo "❌ Erreur : lancez ce script depuis la racine du projet"
  echo "   (le dossier qui contient chaghaf-monolith/)"
  exit 1
fi

# ── Vérifier Docker ───────────────────────────────────────────
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker Desktop n'est pas démarré. Lancez-le et réessayez."
  exit 1
fi

# ── 1. Login ACR ─────────────────────────────────────────────
echo "🔵 [1/4] Login Azure Container Registry..."
az acr login --name "$REGISTRY_NAME"
echo "✅ Login ACR OK"
echo ""

# ── 2. Build Docker image ────────────────────────────────────
FULL_IMAGE_TAG="$ACR_LOGIN_SERVER/$IMAGE_NAME:latest"
BUILD_TAG="$ACR_LOGIN_SERVER/$IMAGE_NAME:$(date +%Y%m%d%H%M)"

echo "🔵 [2/4] Build de l'image Docker..."
echo "   ⏳ ~5-10 minutes (Maven downloads + compile JAR)..."
echo ""

docker build \
  -t "$FULL_IMAGE_TAG" \
  -t "$BUILD_TAG" \
  -f chaghaf-monolith/Dockerfile \
  chaghaf-monolith/

echo "✅ Build terminé"
echo ""

# ── 3. Push vers ACR ─────────────────────────────────────────
echo "🔵 [3/4] Push vers Azure Container Registry..."
docker push "$FULL_IMAGE_TAG"
docker push "$BUILD_TAG"
echo "✅ Push terminé"
echo ""

# ── 4. Deploy Container App ──────────────────────────────────
echo "🔵 [4/4] Déploiement sur Azure Container Apps..."

# Récupérer ou générer le JWT_SECRET
if [ -z "$JWT_SECRET" ]; then
  JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n/+=' | head -c 64)
  echo "🔐 JWT_SECRET généré (64 chars)"
fi

# Vérifier si l'app existe déjà → update, sinon create
APP_EXISTS=$(az containerapp show --name "$CONTAINER_APP" --resource-group "$RESOURCE_GROUP" --query name -o tsv 2>/dev/null || echo "")

if [ -n "$APP_EXISTS" ]; then
  echo "ℹ️  Container App existe déjà → update de l'image"
  az containerapp update \
    --name "$CONTAINER_APP" \
    --resource-group "$RESOURCE_GROUP" \
    --image "$FULL_IMAGE_TAG" \
    --output table
else
  az containerapp create \
    --name "$CONTAINER_APP" \
    --resource-group "$RESOURCE_GROUP" \
    --environment "$CONTAINER_ENV" \
    --image "$FULL_IMAGE_TAG" \
    --registry-server "$ACR_LOGIN_SERVER" \
    --registry-username "$ACR_USERNAME" \
    --registry-password "$ACR_PASSWORD" \
    --target-port 8080 \
    --ingress external \
    --min-replicas 0 \
    --max-replicas 2 \
    --cpu 0.5 \
    --memory 1.0Gi \
    --env-vars \
      "DB_HOST=$POSTGRES_FQDN" \
      "DB_PORT=5432" \
      "DB_NAME=$POSTGRES_DB" \
      "DB_USER=$POSTGRES_ADMIN" \
      "DB_PASS=$POSTGRES_PASSWORD" \
      "DB_SSL_PARAMS=?sslmode=require" \
      "JWT_SECRET=$JWT_SECRET" \
      "SPRING_PROFILES_ACTIVE=prod" \
    --output table
fi

# Récupérer l'URL publique
APP_URL=$(az containerapp show \
  --name "$CONTAINER_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --query "properties.configuration.ingress.fqdn" -o tsv)

# Sauvegarder JWT_SECRET et APP_URL dans .env.azure
grep -v '^JWT_SECRET=' "$ENV_FILE" | grep -v '^APP_URL=' > "$ENV_FILE.tmp" && mv "$ENV_FILE.tmp" "$ENV_FILE"
echo "JWT_SECRET=$JWT_SECRET" >> "$ENV_FILE"
echo "APP_URL=https://$APP_URL" >> "$ENV_FILE"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              ✅ PHASE 2 TERMINÉE — APP DÉPLOYÉE             ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  🌍 URL: https://$APP_URL"
echo "║  🧪 Test: curl https://$APP_URL/actuator/health"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  ➡️  Prochaine étape : ./scripts/03-test-endpoints.sh       ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
