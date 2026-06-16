# Forge FullStack — Backend Explanation

## Architecture Overview

The backend consists of two Spring Boot microservices sitting behind an Nginx API gateway, with PostgreSQL for persistence, Redis for token blacklisting, and Apache Kafka for asynchronous event-driven communication.

```
Browser / Angular Frontend
        │
        ▼
  Nginx Gateway  (port 8084 on host → 8080 inside container)
   ├── /api/auth/**  ──▶  user-auth-service   (port 8081)
   └── /api/**       ──▶  job-posting-service  (port 8082)

user-auth-service   ──▶  PostgreSQL: auth_db
job-posting-service ──▶  PostgreSQL: job_posting_db
                    ──▶  Kafka (consume: demand-events, produce: portal-job-events)
                    ──▶  SSE stream to career portal clients
user-auth-service   ──▶  Redis (JWT blacklist)
```

---

## 1. user-auth-service

**Port:** 8081 (internal) / 8085 (host)  
**Database:** `auth_db` (PostgreSQL)  
**Purpose:** User registration, login, JWT issuance, Google OAuth2 SSO, token refresh and blacklisting.

### Seeded Users (created on startup)

| Email | Password | Role |
|---|---|---|
| admin@griddynamics.com | admin@123 | ADMIN |
| recruiter@talentgrid.com | recruiter@123 | RECRUITER |
| hm@talentgrid.com | hm@123 | HIRING_MANAGER |

### Roles and Scopes

| Role | Scopes |
|---|---|
| ADMIN | USER_CREATE, USER_DELETE, USER_VIEW |
| RECRUITER | JOB_CREATE, JOB_VIEW, JOB_PUBLISH |
| HIRING_MANAGER | JOB_VIEW, JOB_APPROVE |

### Auth API Endpoints

All auth endpoints are prefixed `/api/auth`.

#### POST /api/auth/register
Register a new user.

**Request:**
```json
{
  "username": "John Doe",
  "email": "john@company.com",
  "password": "password123",
  "role": "RECRUITER"
}
```

**Response:**
```json
{
  "id": 4,
  "email": "john@company.com",
  "message": "User registered successfully"
}
```

---

#### POST /api/auth/login
Authenticate and receive a JWT access token.

**Request:**
```json
{
  "email": "recruiter@talentgrid.com",
  "password": "recruiter@123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 2,
    "name": "recruiter1",
    "email": "recruiter@talentgrid.com",
    "role": "RECRUITER",
    "roles": ["RECRUITER"]
  }
}
```

A `refreshToken` HttpOnly cookie is also set on the response.

---

#### POST /api/auth/refresh
Exchange the `refreshToken` cookie for a new access token. No body required.

---

#### POST /api/auth/logout
Blacklists the current JWT in Redis and revokes the refresh token.

**Header:** `Authorization: Bearer <token>`

---

#### GET /api/auth/me
Returns the currently authenticated user's profile.

**Header:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "id": 2,
  "name": "recruiter1",
  "email": "recruiter@talentgrid.com",
  "role": "RECRUITER",
  "roles": ["RECRUITER"],
  "permissions": []
}
```

---

### JWT Flow

```
Login → auth-service issues JWT (HMAC-SHA256, 24h expiry)
                                   + refresh token in HttpOnly cookie

Subsequent requests → Angular attaches Bearer token
                   → Nginx forwards to job-posting-service
                   → JwtAuthFilter validates signature + checks Redis blacklist
                   → AuthenticatedUser injected into controller

Logout → JWT JTI added to Redis with remaining TTL
       → refresh token revoked in DB
       → future requests with same JWT are rejected
```

JWT secret is shared between both services via the `JWT_SECRET` environment variable, so job-posting-service can verify tokens without calling auth-service.

---

## 2. job-posting-service

**Port:** 8082 (internal)  
**Database:** `job_posting_db` (PostgreSQL)  
**Purpose:** Manages the full lifecycle of job postings — from demand ingestion via Kafka, through recruiter authoring, HM approval, to real-time publishing on the career portal.

---

### Job Posting Status Lifecycle

```
[Kafka DEMAND_OPEN_EXTERNAL]
          │
          ▼
   Demand stored in demands table
          │
   Recruiter fills job posting form
          │
          ▼
        DRAFT  ──────── recruiter edits ──────▶  DRAFT
          │
  "Send For Approval"
          │
          ▼
   PENDING_APPROVAL  ──── HM declines ──▶  DECLINED ──── recruiter edits ──▶  DRAFT
          │
      HM approves
          │
          ▼
  READY_TO_PUBLISH  ──── recruiter publishes ──▶  LIVE  ──── close ──▶  CLOSED
          │
   (also auto-expires on applicationDeadline)
          │
          ▼
       CLOSED
```

---

### Job Posting API Endpoints

All endpoints are prefixed `/api/job-postings`. Endpoints under `/public/` require no authentication. All others require a valid JWT.

---

#### POST /api/job-postings
Create a new job posting (initially in DRAFT status).

**Auth:** RECRUITER  
**Request body:** Full `CreateJobPostingRequest` with title, description, skills, location, salary, etc.

**Response:** `JobPostingResponse` with `postingStatus: "DRAFT"`

---

#### GET /api/job-postings
List all job postings. Optionally filter by status.

**Auth:** Any authenticated user  
**Query param:** `?status=PENDING_APPROVAL` (optional)

**Response:** Array of `JobPostingResponse`

---

#### GET /api/job-postings/{id}
Get a single job posting by ID.

---

#### PUT /api/job-postings/{id}
Update a job posting. Only allowed in DRAFT or DECLINED status.

---

#### POST /api/job-postings/{id}/save-draft
Save current fields without changing status (keeps DRAFT or DECLINED).

---

#### POST /api/job-postings/{id}/submit-for-approval
Move posting from DRAFT/DECLINED → PENDING_APPROVAL.

**Side effect:** Sends notification to the Hiring Manager.

---

#### POST /api/job-postings/{id}/approve
HM approves a PENDING_APPROVAL posting → moves to READY_TO_PUBLISH.

**Auth:** HIRING_MANAGER  
**Side effects:**
- Fires Kafka event to `portal-job-events` topic (for external consumers)
- Broadcasts `JOB_PUBLISHED` via SSE to all connected career portal browsers (real-time update)
- Sends notification to recruiter

---

#### POST /api/job-postings/{id}/decline
HM declines a PENDING_APPROVAL posting → moves to DECLINED.

**Auth:** HIRING_MANAGER  
**Request body:**
```json
{ "reason": "Job description needs more detail" }
```
**Side effect:** Sends notification to recruiter with decline reason.

---

#### POST /api/job-postings/{id}/publish
Move a READY_TO_PUBLISH posting → LIVE.

**Auth:** RECRUITER  
**Side effect:** Sends confirmation notification to recruiter.

---

#### POST /api/job-postings/{id}/close
Close a LIVE or READY_TO_PUBLISH posting → CLOSED.

**Side effects:**
- Fires Kafka event to `portal-job-events` topic (JOB_UNPUBLISHED)
- Broadcasts `JOB_UNPUBLISHED` via SSE to career portal browsers (job card disappears in real time)
- Sends notification to recruiter

---

#### GET /api/job-postings/stats
Returns a count of postings grouped by status. Used by the dashboard KPI tiles.

**Response:**
```json
{
  "DRAFT": 3,
  "PENDING_APPROVAL": 1,
  "READY_TO_PUBLISH": 2,
  "LIVE": 5,
  "DECLINED": 1,
  "CLOSED": 0
}
```

---

#### POST /api/job-postings/generate-jd
Generate job description sections using AI (Team 4 contract endpoint).

**Request:**
```json
{
  "roleTitle": "Senior Backend Engineer",
  "skillsRequired": ["Java", "Spring Boot", "Kafka"],
  "seniorityLevel": "SENIOR",
  "department": "Engineering",
  "location": "New York",
  "workMode": "HYBRID",
  "experienceYears": 5
}
```

**Response:**
```json
{
  "summary": "...",
  "responsibilities": "...",
  "requirements": "...",
  "benefits": "...",
  "sectionCount": 4
}
```

---

### Public Endpoints (no auth required)

#### GET /api/job-postings/public/live
Returns all postings with status `READY_TO_PUBLISH` or `LIVE`. This is what the career portal loads on page init.

**Response:** Array of `JobPostingResponse`

---

#### GET /api/job-postings/public/events
Server-Sent Events (SSE) stream. The career portal subscribes to this on load for real-time updates.

**Response type:** `text/event-stream`

**Events emitted:**
- `connected` — sent immediately on subscription to confirm the connection
- `JOB_PUBLISHED` — sent when HM approves a posting; payload is full `JobPostingResponse`
- `JOB_UNPUBLISHED` — sent when a posting is closed or expires; payload is `{ "jobId": 123 }`

---

### Demands API

#### GET /api/demands
Returns all open demands (those without an active job posting). Used by the demands list in the frontend.

#### GET /api/demands/{id}
Get a single demand by primary key.

#### GET /api/demands/by-demand-id/{demandId}
Get demand by its external demand ID (the ID from the Kafka event).

---

### Notifications API

#### GET /api/notifications
Returns all notifications for the currently authenticated user.

#### POST /api/notifications/{id}/read
Marks a notification as read.

---

## 3. Kafka Integration

### Topics

| Topic | Direction | Purpose |
|---|---|---|
| `demand-events` | CONSUME | Receives new demand events from the workforce planning system |
| `portal-job-events` | PRODUCE | Publishes job state changes (approved, unpublished) to external career portal systems |
| `portal-confirmations` | CONSUME | Receives confirmation from external portal that a job is now live (advances status to LIVE) |

### Demand Event Format (inbound)

Only events with `eventType: "DEMAND_OPEN_EXTERNAL"` are processed. Others are silently ignored.

```json
{
  "eventId": "uuid-here",
  "eventType": "DEMAND_OPEN_EXTERNAL",
  "correlationId": "uuid-here",
  "timestamp": "2026-06-16T10:00:00Z",
  "payload": {
    "demandId": "D-001",
    "roleTitle": "Senior Java Developer",
    "skillsRequired": ["Java", "Spring Boot", "Kafka"],
    "experienceLevel": "SENIOR",
    "experienceYears": 5,
    "workMode": "HYBRID",
    "locationCity": "New York",
    "locationState": "NY",
    "locationCountry": "US",
    "department": "Engineering",
    "demandStatus": "OPEN_EXTERNAL",
    "requiredCount": 2
  }
}
```

**Important field names:** `roleTitle`, `skillsRequired`, `experienceLevel` — not `title`, `skills`, `level`.  
**Timestamp** must be an ISO-8601 string, not a Unix float.

### Demand Closure (auto-unpublish)

When a `DEMAND_CLOSED` event arrives on the `demand-events` topic, all active job postings linked to that demand are automatically closed and the career portal is notified in real time via SSE.

---

## 4. Auto-Expiry Scheduler

A scheduled task runs every hour and checks all `READY_TO_PUBLISH` and `LIVE` postings whose `applicationDeadline` is before today. For each expired posting:

1. Status updated to `CLOSED` in the database
2. `JOB_UNPUBLISHED` SSE event broadcast to all career portal clients
3. Kafka event fired to `portal-job-events`
4. Recruiter notified via in-app notification

---

## 5. Real-Time Career Portal Flow (REQ-JP-04)

```
HM clicks Approve
        │
        ▼
job-posting-service
  ├── DB: posting_status = READY_TO_PUBLISH
  ├── Kafka → portal-job-events  (JOB_PUBLISHED)
  └── SSE broadcast → JOB_PUBLISHED event with full job payload
              │
              ▼
  All browsers on /careers/jobs receive the SSE event
  NgRx dispatches careersActions.jobPublished({ job })
  New job card appears instantly — no page refresh needed

HM clicks Decline / Deadline passes / Demand closed
        │
        ▼
  SSE broadcast → JOB_UNPUBLISHED { jobId }
              │
              ▼
  NgRx dispatches careersActions.jobUnpublished({ jobId })
  Job card removed instantly from career portal
```

---

## 6. Database Schema (key tables)

### auth_db

| Table | Purpose |
|---|---|
| `users` | User accounts with hashed passwords |
| `roles` | Role definitions (ADMIN, RECRUITER, HIRING_MANAGER) |
| `scopes` | Permission scopes assigned to roles |
| `user_roles` | Many-to-many: users ↔ roles |
| `refresh_tokens` | Active refresh tokens per user |

### job_posting_db

| Table | Purpose |
|---|---|
| `demands` | Demand records ingested from Kafka |
| `job_postings` | Core job posting records with full lifecycle state |
| `job_posting_skills` | Skills list for each posting (element collection) |
| `job_posting_approvals` | Audit trail of every approval action per posting |
| `notifications` | In-app notifications for recruiters and HMs |

---

## 7. Environment Variables

| Variable | Used By | Description |
|---|---|---|
| `DB_USERNAME` | Both services | PostgreSQL username |
| `DB_PASSWORD` | Both services | PostgreSQL password |
| `JWT_SECRET` | Both services | HMAC-SHA256 key — must be the same in both |
| `DB_URL` | auth-service | Full JDBC URL for auth_db |
| `KAFKA_BOOTSTRAP_SERVERS` | job-posting-service | Kafka broker address |
| `PORTAL_JOB_EVENTS_TOPIC` | job-posting-service | Kafka topic for outbound job events (default: `portal-job-events`) |
| `PORTAL_CONFIRMATIONS_TOPIC` | job-posting-service | Kafka topic for inbound portal confirmations (default: `portal-confirmations`) |

---

## 8. Running Locally with Docker Compose

```bash
# Start all services
docker compose up -d

# Rebuild a specific service after code changes
docker compose build job-posting-service
docker compose up -d job-posting-service

# View logs
docker compose logs -f job-posting-service

# Check all container health
docker compose ps
```

### Service URLs (local)

| Service | URL |
|---|---|
| API Gateway | http://localhost:8084 |
| Auth Service (direct) | http://localhost:8085 |
| Kafka UI | http://localhost:9095 |
| Career Portal | http://localhost:4200/careers |
| Shell App | http://localhost:4200 |

### Quick API Test

```bash
# Login as recruiter
curl -s -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"recruiter@talentgrid.com","password":"recruiter@123"}' | python3 -m json.tool

# List all job postings (replace TOKEN)
curl -s http://localhost:8084/api/job-postings \
  -H "Authorization: Bearer TOKEN" | python3 -m json.tool

# Public live jobs (no auth needed)
curl -s http://localhost:8084/api/job-postings/public/live | python3 -m json.tool

# Watch real-time SSE stream
curl -N http://localhost:8084/api/job-postings/public/events
```
