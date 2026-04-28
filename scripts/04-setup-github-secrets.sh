#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
#  CHAGHAF — Phase 4 : Service Principal Azure pour GitHub Actions
# ═══════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../infra/.env.azure"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Fichier $ENV_FILE introuvable. Lancez d'abord 01."
  exit 1
fi
source "$ENV_FILE"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   CHAGHAF AZURE — PHASE 4               ║"
echo "║   Service Principal + GitHub Secrets    ║"
echo "╚══════════════════════════════════════════╝"
echo ""

SUBSCRIPTION_ID=$(az account show --query id -o tsv)
echo "📋 Subscription ID : $SUBSCRIPTION_ID"
echo ""

# ── Créer le Service Principal ───────────────────────────────
# MSYS_NO_PATHCONV=1 empêche Git Bash de convertir /subscriptions/...
# en C:/Program Files/Git/subscriptions/... sur Windows
echo "🔵 Création du Service Principal pour GitHub Actions..."
SP_JSON=$(MSYS_NO_PATHCONV=1 az ad sp create-for-rbac \
  --name "sp-chaghaf-github" \
  --role contributor \
  --scopes "/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP" \
  --json-auth)

# Sauvegarder dans .env.azure (format brut, sans newline)
SP_FILE="$SCRIPT_DIR/../infra/azure-credentials.json"
echo "$SP_JSON" > "$SP_FILE"
chmod 600 "$SP_FILE"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║    📋 SECRETS À AJOUTER DANS GITHUB                         ║"
echo "║    Repo → Settings → Secrets → Actions → New secret         ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

echo "┌─────────────────────────────────────────────────────────────┐"
echo "│  Secret name : AZURE_CREDENTIALS                           │"
echo "│  Value (TOUT le bloc JSON) :                               │"
echo "└─────────────────────────────────────────────────────────────┘"
echo "$SP_JSON"
echo ""

echo "┌─────────────────────────────────────────────────────────────┐"
echo "│  Secret name : ACR_LOGIN_SERVER                            │"
echo "└─────────────────────────────────────────────────────────────┘"
echo "$ACR_LOGIN_SERVER"
echo ""

echo "┌─────────────────────────────────────────────────────────────┐"
echo "│  Secret name : ACR_USERNAME                                │"
echo "└─────────────────────────────────────────────────────────────┘"
echo "$ACR_USERNAME"
echo ""

echo "┌─────────────────────────────────────────────────────────────┐"
echo "│  Secret name : ACR_PASSWORD                                │"
echo "└─────────────────────────────────────────────────────────────┘"
echo "$ACR_PASSWORD"
echo ""

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ⚠️  Ces valeurs sont SENSIBLES — ne les partagez jamais    ║"
echo "║                                                              ║"
echo "║  Les credentials JSON sont aussi sauvés dans :              ║"
echo "║    infra/azure-credentials.json (gitignored)                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "✅ Une fois les 4 secrets ajoutés sur GitHub :"
echo "   git push → le pipeline Azure se déclenche automatiquement !"
echo ""