# Chaghaf Monolith — Version Complète

Projet Spring Boot monolithique unifié, prêt pour Render.

## Structure

```
chaghaf-monolith/
├── pom.xml              ← Dépendances Maven
├── Dockerfile           ← Build Docker pour Render
└── src/main/
    ├── java/ma/chaghaf/
    │   ├── ChaghafApplication.java    ← Point d'entrée
    │   ├── config/                    ← Sécurité, JWT, SSE, Exceptions
    │   ├── auth/                      ← Login, Register, JWT
    │   ├── subscription/              ← Abonnements, DayAccess
    │   ├── reservation/               ← Réservations de salles
    │   ├── notification/              ← Notifications
    │   ├── social/                    ← Posts sociaux
    │   ├── catalog/                   ← Catalogue boissons/snacks
    │   └── admin/                     ← Endpoints admin + SSE
    └── resources/
        └── application.yml
```

## Endpoints disponibles

### Auth (public)
- POST /api/auth/login
- POST /api/auth/register
- GET  /api/auth/me                   (auth)
- GET  /api/auth/health

### Réservations
- GET  /api/reservations              (auth)
- GET  /api/reservations/salles       (public)

### Abonnements
- GET  /api/subscriptions/active      (auth)
- GET  /api/subscriptions/packs       (public)

### Catalogue (boissons/snacks)
- GET  /api/catalog                   (public, available only)
- GET  /api/catalog/all               (auth)
- GET  /api/catalog/type/{BOISSON|SNACK}
- POST /api/catalog                   (auth)
- PUT  /api/catalog/{id}              (auth)
- DELETE /api/catalog/{id}            (auth)

### Social
- GET  /api/posts                     (auth)
- POST /api/posts                     (auth)

### Notifications
- GET  /api/notifications             (auth)

### Admin
- GET  /api/admin/stream              (SSE)
- GET  /api/admin/occupation
- GET  /api/admin/live-stats
- GET  /api/admin/reservations
- GET  /api/admin/clients
- POST /api/admin/qr/validate
- POST /api/admin/message/send
- POST /api/admin/social/post
- POST /api/admin/broadcast

### Health
- GET /actuator/health

## Déploiement Render

### 1. Push le code sur GitHub

```bash
cd Chaghaf-back-main
# (supprime tout le contenu actuel sauf .git)
# (copie ce projet)
git add .
git commit -m "Complete monolith"
git push
```

### 2. Configure Render Settings

| Champ | Valeur |
|-------|--------|
| Root Directory | `chaghaf-monolith` |
| Dockerfile Path | `Dockerfile` |
| Docker Build Context Directory | (VIDE) |

### 3. Variables d'environnement

```
DB_HOST=dpg-d7f7dcdckfvc73dopob0-a
DB_PORT=5432
DB_NAME=chaghaf_bd
DB_USER=chaghaf_bd_user
DB_PASS=ton_password
JWT_SECRET=chaghaf-super-secret-key-2025-agadir-morocco-production
```

### 4. Manual Deploy → Clear cache & deploy

## Tests rapides après déploiement

```bash
# Health
curl https://chaghaf-back-5t0i.onrender.com/

# Register
curl -X POST https://chaghaf-back-5t0i.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test","email":"test@test.com","password":"123456"}'

# Login
curl -X POST https://chaghaf-back-5t0i.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}'

# Catalogue (public)
curl https://chaghaf-back-5t0i.onrender.com/api/catalog
```

## Notes importantes

- `ddl-auto: update` → Hibernate crée/met à jour les tables automatiquement
- Premier démarrage : Hibernate va créer toutes les tables dans `chaghaf_bd`
- Si tu as déjà des tables avec un schéma différent, il faut les drop d'abord
- Le JWT secret doit faire au moins 256 bits (32+ caractères)
- CORS est activé pour tous les origins (à restreindre en production)
# backend_java_chaghaf
