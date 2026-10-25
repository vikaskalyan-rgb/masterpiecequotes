# Masterpiece Interiors — Quote Builder Backend

Spring Boot 4 (Java 21) REST API for the quote builder app. Stores customer quotes (rooms, items,
material spec, terms), every version shared with a customer, default templates and the business
profile printed on the PDF. PDFs are generated in the frontend from the data this API returns.

## What the API guarantees

- **Every number is computed here.** SQFT quantity = length × width, amount = quantity × rate,
  subtotal, and the final total (rounded by the rule in the business profile, unless the total was
  typed by hand). The frontend shows a live preview with the same rules; the saved value comes from
  the server.
- **Shared quotes are never lost.** Sharing a quote stores an immutable copy of exactly what the
  customer received. Later edits create a new version on the next share; old versions stay viewable.
- **No silent overwrites.** Saves carry the version they started from; a save from a stale copy
  (another phone) is rejected with `409` instead of overwriting newer work.
- **Retries never duplicate.** Creates carry a client-generated id; repeating the request returns
  the same quote.
- **Deletes are reversible.** Deleted quotes are hidden and can be restored.

## Security

A single owner signs in with one password (`APP_PASSWORD`), sent as HTTP Basic over HTTPS on every
request. Without `APP_PASSWORD` (at least 4 characters; 12+ recommended) the app **refuses to start**:
it fails closed, never open. Brute-force protection: 5 wrong passwords lock an IP out for 15 minutes,
and 30 wrong passwords from anywhere within an hour pause all sign-ins for the rest of that hour.
Failed sign-ins are logged.
`/api/health` and `/api/health/ready` are the only public endpoints.

## Local setup

Requirements: Java 21, Maven 3.9+, a Postgres database (local, or a Neon dev branch).

```
export DB_URL=jdbc:postgresql://localhost:5432/masterpiece_quotes
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export APP_PASSWORD=choose-a-long-password
export CORS_ORIGINS=http://localhost:5173
mvn spring-boot:run
```

The server starts on `http://localhost:8080`. Flyway creates or upgrades the schema on startup.

## Tests

The integration tests run the real application against a real Postgres:

```
export TEST_DB_URL=jdbc:postgresql://localhost:5432/quotes_test   # an empty database
mvn verify
```

They cover the money math, auth and lockout, validation, version conflicts, sharing history,
backup and restore, and an upgrade of a database created by the pre-migration version of the app.
GitHub Actions runs them on every push and pull request.

## Database migrations

The schema is owned by Flyway (`src/main/resources/db/migration`). Hibernate only validates it and
refuses to start if entities and tables disagree, so a bad deploy fails before serving traffic.

- `V1` is the schema the app had before migrations existed. On the existing production database
  Flyway records it as the baseline and does not run it.
- `V2` adds quote numbers, versions, shared-version history, soft delete and the business profile,
  normalizes phone numbers and removes dimensions that were duplicated inside item descriptions.

Migrations are **additive only** (nothing dropped or renamed), so the previous release keeps working
against the upgraded database if a deploy has to be rolled back. Keep it that way: drop old columns
in a later release, once nothing reads them.

## API overview

| Endpoint | Purpose |
|---|---|
| `GET /api/quotes?status=&search=&deleted=` | List quotes; search matches name, phone digits or quote number (`MP-0042`) |
| `GET /api/quotes/{id}` | Full quote |
| `POST /api/quotes` | Create (send `clientRequestId` to make retries safe), returns `201` |
| `PUT /api/quotes/{id}` | Replace the quote; requires `version`, returns `409` if stale |
| `PATCH /api/quotes/{id}/status` | `DRAFT` / `SENT` / `ACCEPTED` / `REJECTED` |
| `POST /api/quotes/{id}/share` | Record the version being sent; marks the quote `SENT` |
| `GET /api/quotes/{id}/snapshots[/{snapshotId}]` | Shared versions |
| `DELETE /api/quotes/{id}`, `POST /api/quotes/{id}/restore` | Soft delete and undo |
| `GET /api/items/suggestions?q=` | Past item descriptions with their unit (rates are never suggested) |
| `GET /api/customers/suggestions?q=` | Repeat customers by name or phone |
| `GET/PUT /api/settings/defaults` | Default material spec and terms |
| `GET/PUT /api/settings/business` | Name, address, phones, GSTIN, signatory, rounding rule, validity |
| `GET /api/analytics` | Business numbers (drafts never count as quoted) |
| `GET /api/backup`, `POST /api/backup/restore` | Complete export and additive restore |
| `GET /api/auth/check` | Password check for the login screen |
| `POST /api/client-errors` | Frontend crash reports, written to the server log |
| `GET /api/health`, `GET /api/health/ready` | Liveness (no database) and readiness (checks database) |

## Deploying (Render + Neon)

1. **Back up first**: in Neon, create a branch from `main` before any deploy that adds a migration.
2. Render → the web service → Environment:
   - `DB_URL`: `jdbc:postgresql://<host>/<db>?sslmode=require&channel_binding=require`
   - `DB_USERNAME`, `DB_PASSWORD`
   - `APP_PASSWORD`: the app's sign-in password (4+ characters; longer is safer)
   - `CORS_ORIGINS`: the Vercel URL, e.g. `https://masterpiece-quotes.vercel.app`
3. Deploy (Docker, using the included `Dockerfile`).
4. Optional: set Render's health check path to `/api/health/ready`, so a deploy that can't reach the
   database never takes traffic.

UptimeRobot should ping `/api/health` every 5 minutes. That endpoint never touches the database, so
it keeps Render awake while letting Neon suspend when idle. Render's free tier gives 750 instance
hours per workspace per month: enough for one always-on service, not several.
