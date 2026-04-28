#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
#  CHAGHAF — Phase 1 : Création des ressources Azure
#  À exécuter UNE SEULE FOIS depuis Git Bash / Terminal
# ═══════════════════════════════════════════════════════════════════

set -e  # Arrêt automatique si une commande échoue

# ── Variables — MODIFIEZ CES VALEURS AVANT EXÉCUTION ─────────────
RESOURCE_GROUP="rg-chaghaf"
LOCATION="francecentral"          # France = latence Maroc OK
POSTGRES_SERVER="pg-chaghaf"      # Nom unique global PostgreSQL
POSTGRES_ADMIN="chaghafadmin"
POSTGRES_DB="chaghaf_bd"
CONTAINER_ENV="env-chaghaf"
CONTAINER_APP="app-chaghaf"
REGISTRY_NAME="crchaghafreg"      # Nom unique global (minuscules, pas de tirets)
IMAGE_NAME="chaghaf-monolith"
# ────────────────────────────────────────────────────────────────

if [ -z "$POSTGRES_PASSWORD" ]; then
  echo ""
  read -s -p "🔐 Mot de passe PostgreSQL (min 8 chars, 1 maj, 1 chiffre, 1 spécial): " POSTGRES_PASSWORD
  echo ""
  if [ ${#POSTGRES_PASSWORD} -lt 8 ]; then
    echo "❌ Mot de passe trop court (min 8)"
    exit 1
  fi
fi

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   CHAGHAF AZURE DEPLOYMENT — PHASE 1    ║"
echo "╚══════════════════════════════════════════╝"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/../infra"
mkdir -p "$INFRA_DIR"

# ── 0. Enregistrement des providers Azure ───────────────────────
echo "🔵 [0/5] Enregistrement des providers Azure (one-time)..."
for NAMESPACE in Microsoft.DBforPostgreSQL Microsoft.App Microsoft.ContainerRegistry; do
  STATE=$(az provider show --namespace "$NAMESPACE" --query registrationState -o tsv 2>/dev/null || echo "NotFound")
  if [ "$STATE" != "Registered" ]; then
    echo "   📦 Enregistrement de $NAMESPACE..."
    az provider register --namespace "$NAMESPACE" --wait
    echo "   ✅ $NAMESPACE enregistré"
  else
    echo "   ✔️  $NAMESPACE déjà enregistré — skip"
  fi
done
echo ""

# ── 1. Resource Group ────────────────────────────────────────────
echo "🔵 [1/5] Création du Resource Group..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output table
echo "✅ Resource Group créé : $RESOURCE_GROUP"
echo ""

# ── 2. Azure Container Registry ─────────────────────────────────
echo "🔵 [2/5] Création du Container Registry (ACR)..."
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$REGISTRY_NAME" \
  --sku Basic \
  --admin-enabled true \
  --output table

ACR_LOGIN_SERVER=$(az acr show --name "$REGISTRY_NAME" --query loginServer -o tsv)
ACR_USERNAME=$(az acr credential show --name "$REGISTRY_NAME" --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$REGISTRY_NAME" --query "passwords[0].value" -o tsv)
echo "✅ Container Registry créé : $ACR_LOGIN_SERVER"
echo ""

# ── 3. PostgreSQL Flexible Server ───────────────────────────────
echo "🔵 [3/5] Création PostgreSQL Flexible Server (Burstable B1ms)..."
echo "   ⏳ Cette étape prend 5-8 minutes, soyez patient..."
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER" \
  --location "$LOCATION" \
  --admin-user "$POSTGRES_ADMIN" \
  --admin-password "$POSTGRES_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 16 \
  --public-access 0.0.0.0 \
  --yes \
  --output table

POSTGRES_FQDN=$(az postgres flexible-server show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER" \
  --query fullyQualifiedDomainName -o tsv)

az postgres flexible-server db create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$POSTGRES_SERVER" \
  --database-name "$POSTGRES_DB" \
  --output table

echo "✅ PostgreSQL créé : $POSTGRES_FQDN (DB: $POSTGRES_DB)"
echo ""

# ── 4. Container Apps Environment ───────────────────────────────
echo "🔵 [4/5] Création Container Apps Environment..."
az containerapp env create \
  --name "$CONTAINER_ENV" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output table
echo "✅ Container Apps Environment créé : $CONTAINER_ENV"
echo ""

# ── 5. Sauvegarde des variables pour les autres scripts ─────────
echo "🔵 [5/5] Sauvegarde des variables dans infra/.env.azure..."
cat > "$INFRA_DIR/.env.azure" << EOF
# Généré automatiquement par 01-create-resources.sh
# ⚠️  NE JAMAIS committer ce fichier (déjà ignoré via .gitignore)
RESOURCE_GROUP=$RESOURCE_GROUP
LOCATION=$LOCATION
POSTGRES_SERVER=$POSTGRES_SERVER
POSTGRES_ADMIN=$POSTGRES_ADMIN
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
POSTGRES_DB=$POSTGRES_DB
POSTGRES_FQDN=$POSTGRES_FQDN
CONTAINER_ENV=$CONTAINER_ENV
CONTAINER_APP=$CONTAINER_APP
REGISTRY_NAME=$REGISTRY_NAME
ACR_LOGIN_SERVER=$ACR_LOGIN_SERVER
ACR_USERNAME=$ACR_USERNAME
ACR_PASSWORD=$ACR_PASSWORD
IMAGE_NAME=$IMAGE_NAME
EOF
chmod 600 "$INFRA_DIR/.env.azure"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              ✅ PHASE 1 TERMINÉE — RÉSUMÉ                  ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Resource Group  : $RESOURCE_GROUP"
echo "║  ACR Server      : $ACR_LOGIN_SERVER"
echo "║  PostgreSQL Host : $POSTGRES_FQDN"
echo "║  PostgreSQL DB   : $POSTGRES_DB"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  📄 Variables sauvées dans : infra/.env.azure (gitignored) ║"
echo "║  ➡️  Prochaine étape : ./scripts/02-build-and-deploy.sh    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""