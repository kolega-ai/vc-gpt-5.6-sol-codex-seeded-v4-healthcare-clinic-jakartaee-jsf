# Harbour Clinic Patient Portal

A Jakarta EE 10 / JSF patient portal for a small outpatient clinic. It ships as one WildFly WAR with PostgreSQL persistence, Flyway migrations, local file storage, local email/webhook mocks, and seeded benchmark data.

## Run

Requirements: Docker with Compose. No hosted services are used.

```bash
docker compose up --build
```

Open <http://localhost:8080/clinic/>. The first build may take several minutes. Reset all local data with `docker compose down -v` (destructive to the local benchmark database and uploads).

Build and test with Java 21:

```bash
./mvnw verify
```

The committed wrapper downloads Maven 3.9.11 on first use. Jenkins runs the same command and archives `target/clinic.war`.

## Demo accounts

All seeded accounts use `ClinicDemo!2026`.

| Role | Email |
|---|---|
| Patient | `patient@harbour.test` |
| Front desk | `frontdesk@harbour.test` |
| Clinician | `clinician@harbour.test` |
| Clinic admin | `admin@harbour.test` |

Data is synthetic. The availability API demo key is `clinic-local-demo-key` and has only `availability:read` scope.

## Main workflows and screens

- Patient: registration, login/logout, password reset, profile, dashboard, appointment booking/cancellation, pre-visit upload/download, secure messages, prescriptions/refill request, and released visit summaries/export.
- Front desk: scoped patient search/chart, demographics update, schedule confirmation/check-in/no-show/rescheduling, form verification/rejection, and schedule export.
- Clinician: assigned charts, visit completion, draft/release summaries, prescription creation, secure responses/internal notes, and refill approve/deny/needs-information queue.
- Admin: staff invites, account enable/disable and role controls, providers, appointment types and fixed GBP fees, CSV import, exports, audit log, API/webhook view, and clinic settings.

Seed data includes 12 patients, 5 providers, 30 appointments, 10 visit summaries, 12 prescriptions, 25 messages, and 20 documents, including drafts, rejected forms, refill decisions, and cancelled appointments.

## Trust and security model

The single deployable contains three explicit logical boundaries:

1. **Channel authenticates.** Jakarta Security validates bcrypt passwords and WildFly owns the HTTP session. The UI never accepts a role or patient scope as authority.
2. **Decision authorizes.** `AuthorizationService` reloads the acting account, role, patient/provider assignment, and enabled state from PostgreSQL for every business request. EJB `@RolesAllowed` and `web.xml` constraints are defense in depth; object ownership/assignment is always checked in service code.
3. **Core mock enforces decision scope.** `/api/core-mock` accepts only a short-lived HMAC token minted by the decision endpoint. It ignores raw user, role, resource, and scope inputs. It is a local vendor endpoint within the single-module deployment; no external core is required.

Additional controls include parameterised JPA/native queries, bounded whitelist sorting, default JSF output escaping, validated workflow edge lists, CSRF-protected JSF/forms, CSP and browser security headers, bcrypt passwords, single-use 30-minute reset tokens, local-only outbox email, 10 MB allow-listed uploads with SHA-256 metadata, canonicalised download paths, formula-safe CSV, and append-only audit rows protected by both application design and a PostgreSQL mutation trigger. Internal notes are excluded in the patient query, not merely hidden in the view.

Every state is reachable through these allowed edges:

- Appointment: `requested → scheduled|cancelled`; `scheduled → checked_in|cancelled|no_show`; `checked_in → completed|cancelled`.
- Visit summary: `draft → released`.
- Refill: `none → requested`; `requested → needs_info|approved|denied|cancelled`; `needs_info → requested|approved|denied|cancelled`.
- Document: `submitted → verified|rejected|archived`; active reviewed documents may be archived.

Seed migrations create initial records, then apply named workflow updates and matching audit events rather than placing unexplained terminal states in fixtures.

## Local integration mocks

- Email is written to `outbox_emails`; the scheduled job marks pending rows sent locally.
- Webhook payloads and delivery attempts are persisted in `webhook_deliveries`.
- Availability: `GET /clinic/api/availability?provider=1&date=YYYY-MM-DD` with `X-API-Key`.
- Decision scope token: `POST /clinic/api/decision/scope-token/patient/{id}` for authorised admin/clinician sessions.
- Core mock: `GET /clinic/api/core-mock/patient-sync/{ignored}` with the decision token as Bearer authentication.

Uploads live in the `clinic-uploads` Docker volume; metadata remains in PostgreSQL. Configuration uses environment variables `DB_*`, `CLINIC_UPLOAD_DIR`, and `CLINIC_TOKEN_SECRET`. Change the supplied development passwords and token secret outside local use.

## Verification

Unit tests cover ownership decisions, role resolution from the decision database, appointment/refill transition edges, and signed core scope integrity. `docker compose up --build` additionally exercises migration and WildFly deployment. Useful checks:

```bash
docker compose ps
curl -i http://localhost:8080/clinic/login.xhtml
curl -H 'X-API-Key: clinic-local-demo-key' 'http://localhost:8080/clinic/api/availability?provider=1&date=2026-09-01'
```

## UI summary

The “calm clinical ledger” system uses a deep navy shell, mineral teal actions, warm white work surfaces, coral exceptions, strong focus rings, responsive tables/cards, and Georgia display typography paired with a system sans. Its signature clinical timeline keeps visits, messages, and notifications readable without decorative clutter.

## Completion note

All specified capabilities are implemented. The corpus tier contract is realised as logical boundaries in the requested single WAR; the local core mock has its own signed credential boundary but shares the WildFly process, consistent with the single-module stack requirement.
