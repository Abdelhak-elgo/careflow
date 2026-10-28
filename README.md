# CareFlow

**Healthcare claims platform** — a patient submits a reimbursement claim, a deterministic rules engine decides (`APPROVED` / `REJECTED` / `PENDING`), and an admin can review borderline cases. Built to show production-minded backend engineering: clean architecture, real robustness, and honest scope.

**Stack:** Spring Boot 4 · Java 21 · Angular 17 · PostgreSQL 16 · Keycloak (OIDC) · MinIO (S3) · Flyway · MapStruct · Testcontainers · Docker Compose

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-brightgreen) ![Angular](https://img.shields.io/badge/Angular-17-red) ![Keycloak](https://img.shields.io/badge/Keycloak-OIDC-blue) ![Architecture](https://img.shields.io/badge/architecture-hexagonal-informational)

---

## Screenshots

<!-- Add 2–3 screenshots of the Angular UI here. Example:
![Patient — submit a claim](docs/screenshot-submit.png)
![Admin — review queue](docs/screenshot-admin.png)
-->
_Screenshots coming soon._

---

## What it does

A patient submits a `Claim` for a type of care (`DENTAL` · `OPTICAL` · `GENERAL`) with an amount and a date. An **ordered, deterministic** policy decides — the first matching rule wins:

| Priority | Condition | Decision |
|----------|-----------|----------|
| 1 | `amount < €100` | `APPROVED` |
| 2 | `careType = OPTICAL` and `amount > €500` | `REJECTED` |
| 3 | otherwise | `PENDING` (human review) |

Admins can override decisions, manage file attachments, and read an append-only audit log.

---

## Architecture — Hexagonal (Ports & Adapters)

The domain depends on no framework; adapters (REST, persistence) plug Spring into it.

```
backend/src/main/java/com/elgourmat/careflow/
├── domain/                 # entities, value objects, business rules — 0 Spring deps
├── application/
│   ├── port/in/            # use-case interfaces (driving)
│   ├── port/out/           # repository interfaces (driven)
│   └── service/            # use-case implementations
├── adapter/
│   ├── in/rest/            # controllers, DTOs, RFC 7807 exception handler
│   └── out/persistence/    # JPA entities, repositories, mappers
└── config/                 # Spring wiring
```

**Why hexagonal:** use cases are tested without Spring or a database (far faster tests), new adapters (Kafka consumer, batch, gRPC) can be added without touching the domain, and no Hibernate or Jackson leaks into business logic.

Request flow for `POST /api/claims`:

```
HTTP POST
  └─> ClaimRestController          (adapter in)
        └─> SubmitClaimUseCase     (port in)
              └─> SubmitClaimService (application)
                    ├─ ClaimRulesEngine (domain)
                    └─ ClaimRepository  (port out)
                          └─> ClaimJpaAdapter (adapter out) → PostgreSQL
```

---

## Security & robustness

Delivered in the project, not just documented:

- **OIDC auth** — Spring Security Resource Server validates JWTs issued by Keycloak (realm `careflow`). The `ADMIN` role is required for decisions, attachment rename/delete, and the audit log.
- **Idempotency keys** — opaque header (≤128 chars), server-side dedup with a 24h TTL cleanup job, on `POST /api/claims`, attachment upload, and manual decisions. Replaying a key returns the original resource with no side effect.
- **Rate limiting** — bucket4j (token bucket) on expensive write endpoints, keyed by user (or client IP). `429` responses as RFC 7807 `ProblemDetail` with `Retry-After` and `X-RateLimit-Remaining`.
- **Append-only audit trail** — one row per business action (submit / decide / update / upload / rename / delete) with actor, resource, and a JSON detail payload.
- **S3 storage** — MinIO (S3-compatible). Bucket auto-created on startup; binaries never pass through the database.
- **RFC 7807 errors** — a single `GlobalExceptionHandler` maps validation (400), not-found (404), illegal state (409), rate-limit (429), and fallback (500).
- **Immutable domain** — Java records with `withXxx`, money as `BigDecimal` (never `double`).

---

## API

OpenAPI contract at `/v3/api-docs` · Swagger UI at `/swagger-ui.html`.

### Claims
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/claims` | patient | Submit a claim — supports `Idempotency-Key`, rate-limited |
| `GET` | `/api/claims?status=PENDING` | patient | List (optional status filter) |
| `GET` | `/api/claims/{id}` | patient | Get a claim |
| `PATCH` | `/api/claims/{id}` | patient | Correct `patientId`/`careDate` while `PENDING` |
| `PATCH` | `/api/claims/{id}/decision` | **admin** | Decide manually — supports `Idempotency-Key` |

### Attachments (MinIO / S3)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/claims/{claimId}/attachments` | patient | Upload (`multipart/form-data`), idempotent, rate-limited |
| `GET` | `/api/claims/{claimId}/attachments` | patient | List a claim's attachments |
| `GET` | `/api/attachments/{id}` | patient | Download the binary (streamed from MinIO) |
| `PATCH` | `/api/attachments/{id}` | **admin** | Rename |
| `DELETE` | `/api/attachments/{id}` | **admin** | Delete (DB + S3 object) |

### Audit
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/admin/audit` | **admin** | Audit log (append-only) |

---

## Run it locally

Full stack via Docker Compose:

```bash
# Starts Postgres + Keycloak + MinIO + backend + frontend
docker compose up -d --build
```

| Service | URL |
|---------|-----|
| Frontend (Angular) | http://localhost:4200 |
| Swagger UI (backend) | http://localhost:8187/swagger-ui.html |
| MinIO console | http://localhost:9001 |
| Keycloak | http://localhost:8080 |

Backend only (dev): `docker compose up -d postgres keycloak minio minio-init` then `cd backend && mvn spring-boot:run`.

---

## Tests

```bash
cd backend
./mvnw test     # unit + web (@WebMvcTest) + persistence (@DataJpaTest)
./mvnw verify   # + integration tests (Testcontainers, needs Docker)
```

- **Unit** — rules engine + application services, no Spring.
- **REST adapter** — `@WebMvcTest` per controller.
- **Persistence adapter** — `@DataJpaTest` + Testcontainers Postgres.
- **E2E** — `@SpringBootTest` + Testcontainers, full HTTP → DB path.

Coverage target: **80%+ on `domain/` and `application/`**.

---

## What I'd add for production

Deliberately out of scope here, and what I'd build before a real deployment:

- **Content-hashed idempotency on upload** — hash the file (SHA-256) and compose with the key so replaying a key with different content is rejected.
- **Async processing** — publish `ClaimSubmittedEvent` to Kafka to decouple manual review (dedicated worker, retry policy, DLQ).
- **Observability** — Micrometer + OpenTelemetry → Grafana/Tempo, with business metrics.
- **API versioning** — `/api/v1/…` and versioned payloads.
- **Scalability** — Postgres read replica, Redis cache, distributed rate-limit (bucket4j-redis).
- **Resilience** — Resilience4j circuit breakers and exponential retry on external calls.
- **Storage security** — pre-signed MinIO URLs for downloads, antivirus scan (ClamAV) before persistence.
- **CI/CD** — GitHub Actions: build → test → sonar → staging → e2e → prod.

---

## Known limitations (by design, for this build)

- No content-hash on upload idempotency yet (key maps to attachment id).
- No patient notifications (email/SMS) on status change.
- No CI/CD pipeline delivered — local commands documented above.
- No pre-signed URLs or antivirus scan on storage.
- No business metrics / dashboards.

---

**Author:** Abdelhak El Gourmat · [LinkedIn](https://www.linkedin.com/in/abdelhak-el-gourmat-2675881a5)
