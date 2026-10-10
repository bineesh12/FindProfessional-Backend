# Find Professional Backend

## Authentication configuration

Set these environment variables before starting the backend:

```text
JWT_SECRET=<at-least-32-random-bytes>
GOOGLE_CLIENT_IDS=<comma-separated-allowed-google-oauth-client-ids>
FIREBASE_PROJECT_ID=<firebase-project-id>
DATABASE_URL=jdbc:postgresql://localhost:5432/find_professional
DATABASE_USERNAME=find_professional
DATABASE_PASSWORD=<local-or-managed-database-password>
SERVER_PORT=8082
PORTFOLIO_UPLOAD_DIRECTORY=/absolute/writable/path/to/uploads
```

Use the OAuth client IDs whose ID tokens the backend should accept. Android, iOS, and web/server client IDs can be supplied as a comma-separated list. The mobile app sends the Google ID token to `POST /api/auth/google`; it must never send a Google client secret.

Firebase ID tokens are verified against Firebase's public signing keys. The configured project ID is enforced as both the token issuer and audience.

## Token flow

- Login endpoints return a 15-minute JWT access token and a 30-day opaque refresh token.
- Send the access token as `Authorization: Bearer <accessToken>` to protected APIs.
- Exchange the refresh token at `POST /api/auth/refresh`. Refresh tokens rotate on every use.
- Reusing an old refresh token revokes its entire token family.
- `POST /api/auth/logout` revokes the token family and returns `204 No Content`.

Refresh tokens are stored as SHA-256 hashes in the backend database. The mobile app persists token metadata and authenticated-user UI data in separate typed DataStores. Move client tokens to platform-protected storage before a production release if the threat model requires protection at rest.

The local backend listens on port `8082` by default. A physical Android device can reach it over USB with:

```shell
adb reverse tcp:8082 tcp:8082
```

Portfolio images use local file storage during development. If
`PORTFOLIO_UPLOAD_DIRECTORY` is omitted, files are stored under
`~/.findprofessional/uploads`. Production deployments should provide an
absolute writable directory or replace the local storage implementation with
object storage.

## Railway deployment

Deploy this repository as one Railway service and add a Railway PostgreSQL
service in the same project. The checked-in `Dockerfile` builds and runs the
Spring Boot application on Railway's assigned `PORT`. Flyway applies pending
migrations when the application starts.

Configure these backend service variables using references to the PostgreSQL
service where shown:

```text
DATABASE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DATABASE_USERNAME=${{Postgres.PGUSER}}
DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
JWT_SECRET=<at-least-32-random-bytes>
FIREBASE_PROJECT_ID=<firebase-project-id>
FIREBASE_MESSAGING_ENABLED=true
FIREBASE_SERVICE_ACCOUNT_JSON=<complete-service-account-json>
GOOGLE_CLIENT_IDS=<comma-separated-allowed-google-oauth-client-ids>
OPENAI_API_KEY=<openai-api-key>
EXPOSE_PHONE_CODE=false
PORTFOLIO_UPLOAD_DIRECTORY=/data/uploads
REQUIRE_PERSISTENT_UPLOAD_STORAGE=true
PERSISTENT_UPLOAD_MOUNT_PATH=/data
```

Paste the Firebase service account JSON into Railway as a secret variable; do
not commit the file. `FIREBASE_SERVICE_ACCOUNT_JSON` is only needed for push
delivery. Firebase login token verification only requires
`FIREBASE_PROJECT_ID`.

Create a persistent volume mounted at `/data` so profile and portfolio images
survive deployments. Run one backend replica while local file storage is in
use. Production startup fails when the configured upload directory is outside
the required mount or is not writable, preventing a deployment from silently
falling back to ephemeral storage. Configure `/actuator/health` as the service
health-check path, then create
the custom domain `api.getarbio.com`. Railway provisions TLS after the DNS
record displayed by the dashboard is added to the domain provider.

After the public endpoint is healthy, build the mobile application with:

```text
BACKEND_BASE_URL=https://api.getarbio.com
```

Keep Railway's generated domain available until authentication, uploads,
WebSocket messaging, and push notifications have been verified through the
custom domain.

## Authentication endpoints

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/google
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/phone/request-code
POST /api/auth/phone/verify
PUT /api/users/me/role
```

`PUT /api/users/me/role` requires the backend access token and accepts `CUSTOMER`, `PROFESSIONAL`, or `BOTH`. The authenticated JWT subject identifies the user, and the response contains the updated user without rotating either token. JWTs contain stable identity claims rather than user roles. A null `role_selected_at` marks an account that still needs the first-time role screen.

## Service catalog endpoints

These endpoints require a backend access token and a user with the `CUSTOMER` role:

```text
GET /api/categories
GET /api/categories/{categoryId}/services
GET /api/services/search?query=<2-to-120-characters>
GET /api/services/nearby?latitude=<latitude>&longitude=<longitude>&radiusKm=<optional-radius>
```

The nearby endpoint calculates distance from active service availability areas and ranks results by local popularity, distance, and service name. Its default, minimum, maximum, and result limit are configured through `app.catalog` environment-backed settings. The optional radius allows the client to apply a user's future distance preference without changing the API. Customer coordinates are used only for the request and are not persisted. A location without matching availability returns an empty JSON array.
