# Guide Déploiement Chaghaf sur Azure Container Apps

> Stack : Spring Boot 3 + PostgreSQL 16 + Docker + GitHub Actions + Azure Container Apps

## Vue d'ensemble

```
git push (chaghaf-monolith/**)
      ↓
GitHub Actions (azure-deploy.yml)
      ↓
Build JAR → Build Docker → Push ACR → Update Container App
      ↓
Azure Container Apps (scale-to-zero)
      ↓
Azure Database for PostgreSQL Flexible (Burstable B1ms)
```

## Prérequis (à faire une fois)

### 1. Installer Azure CLI
- Windows: https://aka.ms/installazurecliwindows
- Vérifier: `az --version` doit afficher 2.xx.x

### 2. Installer Docker Desktop
- https://www.docker.com/products/docker-desktop/
- Démarrer Docker Desktop, attendre "Running"

### 3. Se connecter à Azure
```bash
az login
az account show --query "{name:name, id:id}" -o table
```

### 4. Installer extension Container Apps
```bash
az extension add --name containerapp --upgrade
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
```

## Phase 1 — Créer les ressources Azure (10 min)

```bash
cd /path/to/chaghaf-monolith-complete
chmod +x scripts/*.sh
./scripts/01-create-resources.sh
```

Le script demande le **mot de passe PostgreSQL** interactivement (pas hardcodé).

**Variables à modifier dans le script** si vous voulez d'autres noms:
- `RESOURCE_GROUP="rg-chaghaf"`
- `REGISTRY_NAME="crchaghafreg"` ← **doit être unique mondial** (sinon erreur "already exists")
- `LOCATION="francecentral"`

**Ce que ça crée**:
| Ressource | Type | Coût |
|---|---|---|
| `rg-chaghaf` | Resource Group | gratuit |
| `crchaghafreg` | Container Registry Basic | ~$5/mois |
| `pg-chaghaf` | PostgreSQL Flexible B1ms | **gratuit 12 mois** |
| `chaghaf_bd` | Database | inclus |
| `env-chaghaf` | Container Apps Environment | gratuit |

Variables sauvegardées dans `infra/.env.azure` (gitignored).

## Phase 2 — Build & Deploy initial (15 min)

```bash
./scripts/02-build-and-deploy.sh
```

**Ce que ça fait**:
1. Login Azure Container Registry
2. Build l'image Docker depuis `chaghaf-monolith/Dockerfile`
3. Push l'image vers ACR avec 2 tags (`:latest` + timestamp)
4. Crée (ou met à jour) la Container App avec:
   - 0.5 vCPU, 1 GB RAM
   - Min 0 / Max 2 replicas (scale-to-zero)
   - Variables d'env: `DB_HOST`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`, `DB_SSL_PARAMS=?sslmode=require`
5. Affiche l'URL publique HTTPS

À la fin vous obtenez:
```
URL: https://app-chaghaf.xxx.francecentral.azurecontainerapps.io
```

## Phase 3 — Tester (1 min)

```bash
./scripts/03-test-endpoints.sh
```

Tests:
- `/actuator/health`
- `/api/auth/health`
- `/api/catalog`, `/api/subscriptions/packs`, `/api/boissons`, `/api/snacks/catalog`
- Login `admin@chaghaf.ma / admin123` (créé automatiquement par DataInitializer)
- Endpoint protégé `/api/auth/me` avec token

## Phase 4 — CI/CD GitHub Actions

```bash
./scripts/04-setup-github-secrets.sh
```

Le script affiche **4 secrets** à ajouter sur GitHub:
- `AZURE_CREDENTIALS` (JSON complet)
- `ACR_LOGIN_SERVER`
- `ACR_USERNAME`
- `ACR_PASSWORD`

À ajouter dans:
```
GitHub repo → Settings → Secrets and variables → Actions → New repository secret
```

Ensuite, à chaque push sur `main` qui touche `chaghaf-monolith/**`, le workflow `azure-deploy.yml` se lance automatiquement:
1. Build JAR via Maven
2. Build & push image Docker vers ACR
3. Update Container App avec la nouvelle image
4. Health check

## Monitoring & Debug

### Voir les logs en temps réel
```bash
az containerapp logs show \
  --name app-chaghaf \
  --resource-group rg-chaghaf \
  --follow
```

### Voir les révisions déployées
```bash
az containerapp revision list \
  --name app-chaghaf \
  --resource-group rg-chaghaf \
  --output table
```

### Update une variable d'env
```bash
az containerapp update \
  --name app-chaghaf \
  --resource-group rg-chaghaf \
  --set-env-vars "JWT_SECRET=nouvellecle"
```

### Forcer 1 replica minimum (éviter cold start)
```bash
az containerapp update \
  --name app-chaghaf \
  --resource-group rg-chaghaf \
  --min-replicas 1
```
> Coût ~$5-8/mois supplémentaire mais aucun cold start

## Coût estimé

| Ressource | Plan | Prix/mois |
|---|---|---|
| Container Apps | Consumption (scale-to-zero) | ~$0-5 |
| PostgreSQL Flexible | B1ms Burstable | **gratuit 12 mois**, ensuite ~$13 |
| Container Registry | Basic | ~$5 |
| **Total** | | **~$5-10/mois** |

Avec un crédit Azure for Students ($100/an) → ~10-20 mois.

## FAQ

### "REGISTRY_NAME already exists"
Le nom doit être unique mondial. Modifiez `REGISTRY_NAME` dans `01-create-resources.sh` (ex: `crchaghafjardani2025`).

### Cold start (30s première requête)
Normal avec `min-replicas=0`. Solution payante: passer à `min-replicas=1`.

### "SSL connection required"
Les variables d'env Azure ont `DB_SSL_PARAMS=?sslmode=require`. Si l'app crash avec cette erreur, vérifier que la variable est bien définie:
```bash
az containerapp show --name app-chaghaf --resource-group rg-chaghaf --query "properties.template.containers[0].env"
```

### Le workflow Render tourne encore en parallèle
Le workflow `render-deploy.yml` a été désactivé en auto (mode `workflow_dispatch` uniquement). Render auto-deploy peut être désactivé dans le dashboard Render → Settings → Auto-Deploy: No.

## Nettoyer toutes les ressources Azure

⚠️ Supprime TOUT, irréversible:
```bash
az group delete --name rg-chaghaf --yes --no-wait
```
