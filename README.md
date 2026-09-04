# CareFlow

Plateforme de remboursement de soins — un patient soumet une demande, un moteur de règles décide (`APPROVED` / `REJECTED` / `PENDING`).

**Stack** : Spring Boot 4 · Java 21 · Angular 17 · PostgreSQL 16 · Keycloak (OIDC) · MinIO (S3) · Flyway · MapStruct · Testcontainers · Docker Compose.

---

## Sommaire

- [Domaine métier](#domaine-métier)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Démarrage local](#démarrage-local)
- [API](#api)
- [Tests](#tests)
- [Sécurité & robustesse](#sécurité--robustesse)
- [Production-ready](#production-ready)
- [Dette technique avouée](#dette-technique-avouée)

---

## Domaine métier

Un patient soumet un `Claim` (demande de remboursement) portant sur un soin (`CareType` = `DENTAL` · `OPTICAL` · `GENERAL`), avec un montant et une date. Le moteur applique une politique **ordonnée et déterministe** — la première règle qui matche décide :

| Priorité | Condition | Décision |
|---|---|---|
| 1 | `amount < 100 €` | `APPROVED` |
| 2 | `careType = OPTICAL` ET `amount > 500 €` | `REJECTED` |
| 3 | sinon | `PENDING` (revue humaine) |

---

## Architecture

**Hexagonal (Ports & Adapters)** — le domaine ne dépend d'aucun framework ; les adapters (REST, persistence) branchent Spring dessus.

```text
backend/src/main/java/com/elgourmat/careflow/
├── domain/                    # entités, value objects, exceptions métier — 0 dep Spring
├── application/
│   ├── port/in/               # use case interfaces (driving)
│   ├── port/out/              # repository interfaces (driven)
│   └── service/               # implémentations use case
├── adapter/
│   ├── in/rest/               # controllers, DTOs, exception handler RFC 7807
│   └── out/persistence/       # JPA entities, repositories, mappers
└── config/                    # wiring Spring
```

### Pourquoi hexagonal

- **Testable** : les use cases se testent sans Spring et sans DB (temps de test ×10 plus rapide).
- **Évolutif** : brancher un nouvel adapter (consumer Kafka, batch, gRPC) ne touche pas le domaine.
- **Explicite** : aucun leak Hibernate ou Jackson dans la couche métier.

### Flux `POST /api/claims`

```text
HTTP POST
  └─> ClaimRestController                (adapter in)
        └─> SubmitClaimUseCase           (port in)
              └─> SubmitClaimService     (application)
                    ├─ ClaimRulesEngine  (domain)
                    └─ ClaimRepository   (port out)
                          └─> ClaimJpaAdapter  (adapter out) → PostgreSQL
```

---

## Stack technique

| Couche | Techno |
|---|---|
| Backend | Spring Boot 4 · Spring Web MVC · Spring Security (OAuth2 Resource Server) · Spring Data JPA · Flyway · MapStruct · Lombok · Springdoc OpenAPI · bucket4j (rate-limit) · MinIO SDK |
| Tests | JUnit 5 · Mockito · Testcontainers (PostgreSQL) · `@WebMvcTest` · `@DataJpaTest` |
| Frontend | Angular 17 (parcours patient + admin) |
| Infra locale | Docker Compose : Postgres 16 · Keycloak 25 · MinIO (S3) · backend · frontend |

---

## Démarrage local

### Full-stack via Docker Compose

```bash
# Démarre Postgres + Keycloak + MinIO + backend + frontend
docker compose up -d --build

# UIs :
open http://localhost:4200                       # Frontend Angular
open http://localhost:8187/swagger-ui.html       # Swagger UI (backend)
open http://localhost:9001                       # Console MinIO (minioadmin/minioadmin)
open http://localhost:8080                       # Keycloak (admin/admin)
```

### Backend seul (dev)

```bash
# 1. Dépendances externes (Postgres port 5444, Keycloak 8080, MinIO 9000)
docker compose up -d postgres keycloak minio minio-init

# 2. Lancer l'app
cd backend
mvn spring-boot:run
```

### Frontend seul (dev)

```bash
cd frontend/careflow-web
npm install
npm start   # http://localhost:4200
```

---

## API

Contrat OpenAPI : `http://localhost:8187/v3/api-docs` · UI : `/swagger-ui.html`.

### Claims

| Verbe | Path | Auth | Description |
|---|---|---|---|
| `POST`  | `/api/claims` | patient | Soumettre une demande — supporte `Idempotency-Key`, rate-limité |
| `GET`   | `/api/claims?status=PENDING` | patient | Lister (filtre optionnel par statut) |
| `GET`   | `/api/claims/{id}` | patient | Récupérer une demande |
| `PATCH` | `/api/claims/{id}` | patient | Corriger `patientId`/`careDate` si `PENDING` |
| `PATCH` | `/api/claims/{id}/decision` | **admin** | Trancher manuellement — supporte `Idempotency-Key` |

### Attachments (MinIO / S3)

| Verbe | Path | Auth | Description |
|---|---|---|---|
| `POST`   | `/api/claims/{claimId}/attachments` | patient | Uploader (`multipart/form-data`) — supporte `Idempotency-Key`, rate-limité |
| `GET`    | `/api/claims/{claimId}/attachments` | patient | Lister les pièces d'une demande |
| `GET`    | `/api/attachments/{id}` | patient | Télécharger le binaire (stream depuis MinIO) |
| `PATCH`  | `/api/attachments/{id}` | **admin** | Renommer |
| `DELETE` | `/api/attachments/{id}` | **admin** | Supprimer (DB + objet S3) |
| `GET`    | `/api/admin/attachments` | **admin** | Toutes les pièces jointes |

### Audit

| Verbe | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/admin/audit` | **admin** | Journal d'audit (append-only) |

### Convention headers

- `Authorization: Bearer <JWT>` — token OIDC émis par Keycloak (realm `careflow`).
- `X-User-Id: <string>` — identité simulée (fallback MVP quand pas de token).
- `Idempotency-Key: <opaque ≤128 chars>` — dedup serveur, TTL 24 h, sur `POST /api/claims`, `POST /api/claims/{id}/attachments`, `PATCH /api/claims/{id}/decision`.

---

## Tests

```bash
cd backend
./mvnw test        # unit + web + persistence
./mvnw verify      # + tests d'intégration (Testcontainers, nécessite Docker)
```

- **Unit** : moteur de règles + services applicatifs — aucune dep Spring.
- **Adapter REST** : `@WebMvcTest` par controller.
- **Adapter persistence** : `@DataJpaTest` + Testcontainers Postgres.
- **E2E** : `@SpringBootTest` + Testcontainers, parcours complet HTTP → DB.

Cible de couverture : **80%+ sur `domain/` et `application/`**.

---

## Sécurité & robustesse

Livré dans le POC (pas seulement documenté) :

- **Auth OIDC** — Spring Security Resource Server valide les JWT émis par Keycloak (`realm careflow`). Rôle `ADMIN` requis sur les routes de décision, de rename/delete de pièces jointes et sur le journal d'audit.
- **Idempotency-Key** — header opaque ≤ 128 chars, dedup serveur (TTL 24 h via job de nettoyage `IdempotencyCleanupJob`). Table `idempotency_key(resource_type, resource_id)` généralisée en V5 pour couvrir `CLAIM` et `ATTACHMENT` ; rejouer la même clé renvoie la ressource initiale sans effet de bord.
- **Rate limiting** — bucket4j (token bucket) sur les endpoints d'écriture coûteux : `POST /api/claims`, `POST /api/claims/{id}/attachments`. Clé de bucket = `X-User-Id` sinon IP client. Réponses 429 en ProblemDetail RFC 7807 + `Retry-After` + `X-RateLimit-Remaining`.
- **Audit trail append-only** — table `audit_log`, une ligne par action métier (submit / decide / update / upload / rename / delete) avec l'acteur, le type de ressource, et un JSON de détails.
- **Storage S3** — MinIO en local (Docker), API S3-compatible. Le bucket `careflow-attachments` est créé automatiquement au démarrage (`MinioFileStorageAdapter#ensureBucket`) et pré-provisionné par `minio-init` en compose. Les binaires ne transitent jamais par la DB.
- **Erreurs RFC 7807** — `GlobalExceptionHandler` : 400 (validation), 404 (`ClaimNotFoundException`, `AttachmentNotFoundException`), 409 (`IllegalClaimStateException`), 429 (rate-limit), 500 (fallback).
- **Immutabilité domaine** — records + `withXxx` sur `ClaimAttachment`, `Money` en `BigDecimal`, jamais `double`.

## Production-ready

Ce que je livrerais en plus si le projet allait en prod :

- **Idempotency sur upload avec hash** — actuellement la clé mappe → attachment id. Idéal : hasher SHA-256 le fichier + composer avec la clé pour rejeter un même `Idempotency-Key` renvoyé avec un contenu différent.
- **Traitement async** — publier `ClaimSubmittedEvent` sur Kafka pour découpler la validation manuelle des `PENDING` (worker dédié, retry policy, DLQ).
- **Observability** — Micrometer + OpenTelemetry → Grafana/Tempo ; metrics métier `claim.submitted{status=…}`, `claim.decision.duration`, `attachment.upload.bytes`.
- **Versionning API** — préfixe `/api/v1/…` ; payload versionné en base pour tolérer l'évolution du domaine.
- **Scalabilité** — read replica Postgres pour les `GET`, cache Redis sur listings filtrés, partitioning `claim` par `submitted_at` quand le volume explose. Rate-limit distribué via Redis (bucket4j-redis).
- **Résilience** — Resilience4j circuit breaker sur futurs appels externes (anti-fraude, scoring IA, MinIO), retry exponentiel sur les uploads S3.
- **Sécurité storage** — URLs pré-signées MinIO pour le download (offload la bande passante), scan antivirus (ClamAV) avant persistence en DB.
- **CI/CD** — GitHub Actions : `build → test → sonar → deploy staging → e2e → deploy prod (manuel)`.

---

## Dette technique avouée

Éléments **explicitement hors scope** de ce POC — à traiter avant tout déploiement en production :

- **Idempotency sur upload avec hash de contenu** — la clé mappe actuellement vers l'attachment id sans lier le contenu ; un rejeu avec un fichier différent renverrait l'ancien.
- **Notifications patient** (email, SMS) — l'utilisateur n'est pas notifié lors d'un changement de statut.
- **CI/CD** — pas de pipeline livré ; commandes locales documentées ci-dessus.
- **Sécurité storage** — pas d'URLs pré-signées MinIO ni de scan antivirus (ClamAV) sur les uploads.
- **Observability** — pas de metrics métier (Micrometer + OTel) ni de dashboards.

---

**Auteur** : Abdelhak El Gourmat.
