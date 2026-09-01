# CareFlow

Plateforme de remboursement de soins — un patient soumet une demande, un moteur de règles décide (`APPROVED` / `REJECTED` / `PENDING`).

**Stack** : Spring Boot 3 · Java 21 · Angular 17 *(planifié)* · PostgreSQL 16 · Flyway · Testcontainers · Docker Compose.

**Contexte** : exercice de 3 jours pour entretien Senior @ Theodo. Voir [Dette technique avouée](#dette-technique-avouée) pour le scope explicitement non couvert.

---

## Sommaire

- [Domaine métier](#domaine-métier)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Démarrage local](#démarrage-local)
- [API](#api)
- [Tests](#tests)
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
| Backend | Spring Boot 3 · Spring Web MVC · Spring Data JPA · Flyway · MapStruct · Lombok · Springdoc OpenAPI |
| Tests | JUnit 5 · Mockito · Testcontainers (PostgreSQL) · `@WebMvcTest` · `@DataJpaTest` |
| Frontend | Angular 17 · TailwindCSS · Jest *(arrive en Jour 3)* |
| Infra locale | Docker Compose (Postgres 16.2) |

---

## Démarrage local

### Backend

```bash
# 1. Démarrer Postgres (port 5434)
docker compose up -d

# 2. Lancer l'app
cd backend
./mvnw spring-boot:run

# 3. Swagger UI
open http://localhost:8187/swagger-ui.html
```

### Frontend

*À venir — l'app Angular sera initialisée en Jour 3 dans `frontend/`.*

---

## API

Contrat OpenAPI : `http://localhost:8187/v3/api-docs` · UI : `/swagger-ui.html`.

| Verbe | Path | Description |
|---|---|---|
| `POST` | `/api/claims` | Soumettre une demande |
| `GET`  | `/api/claims?status=PENDING` | Lister les demandes (filtre optionnel par statut) |
| `GET`  | `/api/claims/{id}` | Récupérer une demande |

**Convention MVP** : l'identité du patient est passée via le header `X-User-Id` (voir [Dette technique](#dette-technique-avouée)).

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

## Production-ready

Ce que je livrerais en plus si le projet allait en prod :

- **Auth** — OIDC (Keycloak / Auth0), scopes `patient:read`, `claim:submit`, `claim:review`.
- **Traitement async** — publier `ClaimSubmittedEvent` sur Kafka pour découpler la validation manuelle des `PENDING` (worker dédié, retry policy, DLQ).
- **Observability** — Micrometer + OpenTelemetry → Grafana/Tempo ; metrics métier `claim.submitted{status=…}`, `claim.decision.duration`.
- **Idempotency** — header `Idempotency-Key` sur `POST /api/claims`, dedup serveur TTL 24 h.
- **Versionning API** — préfixe `/api/v1/…` ; payload versionné en base pour tolérer l'évolution du domaine.
- **Scalabilité** — read replica Postgres pour les `GET`, cache Redis sur listings filtrés, partitioning `claim` par `submitted_at` quand le volume explose.
- **Résilience** — Resilience4j circuit breaker sur futurs appels externes (anti-fraude, scoring IA).
- **CI/CD** — GitHub Actions : `build → test → sonar → deploy staging → e2e → deploy prod (manuel)`.

---

## Dette technique avouée

Faute de temps sur les 3 jours, les éléments suivants sont **explicitement hors scope MVP** :

- **Authentification / autorisation** — simulée via header `X-User-Id`. En prod → OIDC.
- **Rate limiting** — non implémenté. En prod → gateway (Kong / Spring Cloud Gateway) + bucket4j.
- **Audit trail** — seulement `submitted_at` / `decided_at`. En prod → table `claim_event` append-only, ou Spring Data Envers.
- **Notifications patient** (email, SMS) — hors scope. En prod → event → worker dédié.
- **Front admin** (traitement des `PENDING`) — le front livré ne couvre que le parcours patient.
- **CI/CD** — pas de pipeline livré ; commandes locales documentées ci-dessus.

---

**Auteur** : Abdelhak El Gourmat · Exercice pour entretien Senior @ Theodo.
