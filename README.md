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
