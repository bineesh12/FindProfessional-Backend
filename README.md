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
