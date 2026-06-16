cat > /mnt/user-data/outputs/README.md << 'EOF'
# user-auth-service

Authentication and Authorization microservice for the **TalentGrid** platform.

This service is the security foundation of the platform — it handles all identity, access, and token management. Every other service in the platform validates tokens issued by this service.

---

## What this service does

- Registers users and assigns roles
- Authenticates users with email and password, returns a signed JWT access token
- Issues refresh tokens via HttpOnly cookies and supports automatic token rotation on refresh
- Blacklists JWT access tokens on logout using Redis (identified by JTI)
- Supports Google OAuth2 login — automatically creates an account on first login
- Enforces account lockout after 5 consecutive failed login attempts (15-minute lock)
- Exposes JWT validation consumed by all other platform services via the shared auth-client library

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| Security | Spring Security + OAuth2 Client |
| Token | JJWT 0.12.5 (HS256) |
| Database | PostgreSQL 15 (via Spring Data JPA) |
| Token blacklist | Redis (Spring Data Redis) |
| Password hashing | BCrypt |
| Boilerplate | Lombok |

---

## Project structure

```
src/main/java/com/talentgrid/auth/
├── UserAuthServiceApplication.java    Entry point
├── config/
│   ├── SecurityConfig.java            Spring Security filter chain, OAuth2, RBAC
│   ├── RedisConfig.java               RedisTemplate bean configuration
│   └── DataInitializer.java           Seeds default roles, scopes, and admin user on startup
├── constants/
│   └── PermissionConstants.java       Scope name constants (USER_CREATE, USER_VIEW, USER_DELETE)
├── controller/
│   ├── AuthController.java            /api/auth endpoints (register, login, refresh, logout)
│   ├── OAuthController.java           /api/auth/oauth-success — handles post-OAuth2 redirect
│   └── TestController.java            /api/test endpoints for auth and RBAC verification
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   └── response/
│       ├── LoginResponse.java
│       └── RegisterResponse.java
├── entity/
│   ├── User.java                      users table
│   ├── Role.java                      roles table
│   ├── Scope.java                     scopes table
│   └── RefreshToken.java              refresh_tokens table
├── exception/
│   └── GlobalExceptionHandler.java    Centralised error responses
├── filter/
│   └── JwtAuthenticationFilter.java   Validates Bearer token on every request
├── jwt/
│   ├── JwtService.java                Token generation, parsing, validation
│   └── JwtBlacklistService.java       Redis-backed blacklist check by JTI
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── ScopeRepository.java
│   └── RefreshTokenRepository.java
├── security/
│   └── CustomUserDetailsService.java  Loads UserDetails by email for Spring Security
└── service/
    ├── RefreshTokenService.java       Create, validate, rotate, and revoke refresh tokens
    ├── interfaces/
    │   └── AuthService.java
    └── impl/
        └── AuthServiceImpl.java       Core register, login, refresh logic + account lockout
```

---

## Roles

The platform supports the following roles. Each role is stored in the `roles` table and assigned to users at registration or via OAuth2 login.

| Role | Description |
|---|---|
| `ADMIN` | Full platform access — manages users, roles, and system configuration |
| `HIRING_MANAGER` | Creates demands and oversees the status and progress of those demands || `RESOURCE_MANAGER` | Manages bench engineers and handles internal allocation (RMG) |
| `RECRUITER` | Manages candidate pipeline, interviews, and offers |
| `CANDIDATE` | External applicant — can apply for jobs and track their own application |
| `EMPLOYEE` | Internal employee — can update their own profile and availability |

Roles are seeded into the database via `DataInitializer` on startup. Each role can have multiple scopes (fine-grained permissions) attached to it via the `role_scopes` join table.

---

## Database schema

```
users
  id, username, email, password, enabled,
  failed_attempts, account_locked, lock_time,
  created_at, updated_at

roles
  id, name

scopes
  id, name, description

refresh_tokens
  id (UUID), token, user_id (FK → users), expiry_date, revoked

user_roles         (join table)
  user_id, role_id

role_scopes        (join table)
  role_id, scope_id
```

Schema is auto-managed by Hibernate (`ddl-auto=update`). Flyway migrations will be added in a future iteration.

---

## API endpoints

### `POST /api/auth/register`

Registers a new user. Role defaults to `EMPLOYEE` if not provided.

**Request body:**
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "John@1234",
  "role": "RECRUITER"
}
```

Password rules: minimum 8 characters, at least one uppercase letter, one lowercase letter, one digit, and one special character (`@$!%*?&`).

**Response:**
```json
{
  "message": "User registered successfully",
  "email": "john@example.com",
  "role": "RECRUITER"
}
```

---

### `POST /api/auth/login`

Authenticates a user. Returns JWT access token in the response body and sets a refresh token as an HttpOnly cookie.

**Request body:**
```json
{
  "email": "john@example.com",
  "password": "John@1234"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "email": "john@example.com",
  "roles": ["RECRUITER"]
}
```

**Cookie set automatically (HttpOnly, Secure, SameSite=Strict):**
```
refreshToken=<uuid>
```

Account locks for 15 minutes after 5 consecutive failed login attempts.

---

### `POST /api/auth/refresh`

Issues a new access token and rotates the refresh token. Reads the refresh token from the cookie — no request body required.

**Response:** Same shape as the login response. A new refresh token cookie is set automatically.

---

### `POST /api/auth/logout`

Revokes the refresh token and blacklists the current access token in Redis by its JTI so it cannot be reused.

**Headers required:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

---

### `GET /api/auth/oauth-success?email={email}`

Called internally after Google OAuth2 redirect completes. If no account exists for the email, one is created automatically with the `CANDIDATE` role. Returns a JWT access token.

---

### `GET /api/test/secure`

Returns the authenticated user's email and granted authorities. Use this to verify JWT authentication is working end-to-end. Requires a valid Bearer token.

### `GET /api/test/admin`

Requires the `USER_DELETE` scope. Returns an RBAC confirmation response. Use this to verify scope-based access control is working correctly.

---

## JWT token structure

Every access token issued by this service contains the following claims:

| Claim | Value |
|---|---|
| `sub` | User ID |
| `email` | User email address |
| `roles` | List of role names assigned to the user |
| `scopes` | Flat list of all scope names across all roles |
| `token_type` | `access` |
| `iss` | `auth-service` |
| `aud` | `api-gateway` |
| `jti` | UUID — used for Redis blacklist lookup on logout |
| `iat` | Issued at timestamp |
| `exp` | Expiry timestamp |

Signing algorithm: **HS256**. Key is derived from `JWT_SECRET`.

---

## Security rules

| Endpoint pattern | Auth required |
|---|---|
| `POST /api/auth/**` | No |
| `GET /oauth2/**` | No |
| `GET /login/**` | No |
| `GET /swagger-ui/**` | No |
| `GET /v3/api-docs/**` | No |
| Everything else | Yes — valid Bearer JWT |

The `JwtAuthenticationFilter` intercepts every request and performs these checks in order:

1. Bearer token is present in the `Authorization` header
2. JTI is not in the Redis blacklist (token not already logged out)
3. Token signature is valid and not expired
4. Email extracted from token matches an active user in the database

Session management is fully stateless — no HTTP session is ever created.

---

## Seeded data on startup

`DataInitializer` runs every time the service starts and creates the following records if they do not already exist:

**Scopes:** `USER_CREATE`, `USER_DELETE`, `USER_VIEW`

**Roles seeded:** `ADMIN` (with all three scopes attached)

All other roles must be inserted into the `roles` table before users can register with them.

**Default admin account:**
```
email:    admin@griddynamics.com
password: admin@123
role:     ADMIN
```

Change the default admin credentials before deploying to any shared environment.

---

## Local setup

### Prerequisites

- Java 17
- PostgreSQL 15 running locally (or via Docker Compose at monorepo root)
- Redis running on `localhost:6379`

### 1. Create `.env` file

Create a file at `Forge_Java_Capstone_Project_Q1_2026_Services/.env` with your local values:

```properties
DB_URL=jdbc:postgresql://localhost:5432/talentgrid_auth
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_secret_key_minimum_32_characters
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

This file is git-ignored and must never be committed to the repository.

### 2. Run the service

From the monorepo root:
```bash
mvn spring-boot:run -pl services/user-auth-service
```

From IntelliJ — set the working directory in the run configuration to `$MODULE_WORKING_DIR$` so the `.env` file is resolved from the correct path.

The service starts on **port 8081**.

### 3. Verify startup

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"Test@1234","role":"EMPLOYEE"}'
```

---

## Environment variables

| Variable | Description | Required |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL | Yes |
| `DB_USERNAME` | Database username | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | HMAC signing key — minimum 32 characters | Yes |
| `jwt.expiration` | Access token TTL in milliseconds (default: `900000` = 15 min) | No |
| `jwt.refresh-expiration` | Refresh token TTL in milliseconds (default: `604800000` = 7 days) | No |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | Only if using OAuth2 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | Only if using OAuth2 |

---