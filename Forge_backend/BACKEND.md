# RMS — Job Posting Service

Backend for the Recruitment Management System (RMS). Built by Team 3.

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka |
| Auth | JWT (shared secret with auth-service) |
| Cache | Redis (auth-service only) |
| Container | Docker + Docker Compose |

---

## Services & Ports

| Service | Host Port | Description |
|---|---|---|
| Kafka UI | 8080 | Visual Kafka browser |
| user-auth-service | 8081 | JWT issuer, user management |
| job-posting-service | 8082 | Team 3 — this service |
| PostgreSQL | 5432 | Two DBs: `auth_db`, `job_posting_db` |
| Redis | 6379 | JWT blacklist for auth-service |
| Kafka | 9092 | External listener (host) |

---

## Project Structure

```
backend/
├── user-auth-service/          # Team 1 — JWT issuer
│   ├── Dockerfile
│   └── src/
├── job-posting-service/        # Team 3 — this service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/talentgrid/jobposting/
│       ├── config/             # Security, CORS
│       ├── controller/         # REST endpoints
│       ├── dto/                # Request / Response shapes
│       ├── entity/             # JPA entities
│       ├── enums/              # JobStatus, ApprovalAction
│       ├── event/              # Kafka event POJOs
│       ├── exception/          # GlobalExceptionHandler
│       ├── filter/             # JwtAuthFilter
│       ├── kafka/              # DemandEventConsumer
│       ├── repository/         # Spring Data JPA
│       ├── security/           # JwtTokenUtil, AuthenticatedUser
│       └── service/            # Business logic
scripts/
└── init-multiple-dbs.sh        # Creates auth_db + job_posting_db on first boot
docker-compose.yml
.env.example
```

---

## Quick Start

```bash
# 1. Clone and enter the project
cd "forge-rms 4"

# 2. Copy env file
cp .env.example .env

# 3. Build and start all services
docker compose up --build -d

# 4. Watch logs
docker compose logs -f
```

First build takes ~2–3 minutes. Services are ready when health checks pass.

---

## Seeded Users

| Email | Password | Role |
|---|---|---|
| admin@griddynamics.com | admin@123 | ADMIN |
| recruiter@talentgrid.com | recruiter@123 | RECRUITER |
| hm@talentgrid.com | hm@123 | HIRING_MANAGER |

---

## Authentication

All `/api/**` endpoints on port 8082 require a Bearer token issued by the auth-service.

**Login:**
```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "recruiter@talentgrid.com",
  "password": "recruiter@123"
}
```

Use the returned `accessToken` as:
```
Authorization: Bearer <accessToken>
```

---

## Status Flow

```
DRAFT
  │
  ▼  (submit-for-approval)
PENDING_APPROVAL
  │
  ├──▶ READY_TO_PUBLISH  (approve)
  │         │
  │         ▼  (publish)
  │        LIVE
  │
  └──▶ DECLINED  (decline — mandatory reason required)
```

---

## API Reference

### Job Postings

| Method | Endpoint | Description | Who |
|---|---|---|---|
| POST | `/api/job-postings` | Create new posting | Recruiter |
| GET | `/api/job-postings` | List all postings | Any |
| GET | `/api/job-postings?status=DRAFT` | Filter by status | Any |
| GET | `/api/job-postings/{id}` | Get single posting | Any |
| PUT | `/api/job-postings/{id}` | Update posting | Recruiter |
| POST | `/api/job-postings/{id}/save-draft` | Save as draft | Recruiter |
| POST | `/api/job-postings/{id}/submit-for-approval` | Send to HM | Recruiter |
| POST | `/api/job-postings/{id}/approve` | Approve posting | Hiring Manager |
| POST | `/api/job-postings/{id}/decline` | Decline with reason | Hiring Manager |
| POST | `/api/job-postings/{id}/publish` | Go live | Recruiter |
| GET | `/api/job-postings/{id}/approvals` | Approval audit trail | Any |

### Demands (from Kafka)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/demands` | All demands received from Kafka |
| GET | `/api/demands/{id}` | By internal DB id |
| GET | `/api/demands/by-demand-id/{demandId}` | By original demand id |

### Notifications

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/notifications` | All notifications for current user |
| GET | `/api/notifications/unread` | Unread only |
| GET | `/api/notifications/unread/count` | Unread badge count |
| PUT | `/api/notifications/read-all` | Mark all read |
| PUT | `/api/notifications/{id}/read` | Mark one read |

---

## Request & Response Shapes

### Create / Update Job Posting

```json
{
  "demandId": 101,
  "title": "Senior Java Developer",
  "description": "Full job description here...",
  "skillsRequired": ["Java", "Spring Boot", "Kafka"],
  "employmentType": "FULL_TIME",
  "experienceLevel": "Senior",
  "experienceYears": 5.0,
  "workMode": "HYBRID",
  "locationCity": "Bangalore",
  "locationState": "Karnataka",
  "locationCountry": "India",
  "department": "Engineering",
  "jobCategory": "Backend",
  "salaryMin": 180000,
  "salaryMax": 240000,
  "currency": "INR",
  "showSalary": true,
  "requiredCount": 1
}
```

### Decline Request

```json
{
  "comments": "Job description needs more detail on required skills."
}
```

### Job Posting Response

```json
{
  "success": true,
  "message": "Job posting created",
  "data": {
    "id": 1,
    "demandId": 101,
    "title": "Senior Java Developer",
    "status": "DRAFT",
    "createdBy": 3,
    "createdByEmail": "recruiter@talentgrid.com",
    "createdAt": "2026-06-14T10:00:00",
    "updatedAt": "2026-06-14T10:00:00"
  }
}
```

---

## Kafka — Demand Events

The service consumes the `demand-events` topic. Only events with `eventType: DEMAND_OPEN_EXTERNAL` are processed. Duplicates (same `demandId`) are silently skipped.

### Event Schema

```json
{
  "eventId": "evt-001",
  "eventType": "DEMAND_OPEN_EXTERNAL",
  "timestamp": "2026-06-14T10:00:00Z",
  "source": "demand-service",
  "payload": {
    "demandId": 101,
    "roleTitle": "Senior Java Developer",
    "description": "...",
    "skillsRequired": ["Java", "Spring Boot"],
    "employmentType": "FULL_TIME",
    "experienceLevel": "Senior",
    "experienceYears": 5.0,
    "workMode": "HYBRID",
    "locationCity": "Bangalore",
    "locationState": "Karnataka",
    "locationCountry": "India",
    "department": "Engineering",
    "jobCategory": "Backend",
    "salaryMin": 180000,
    "salaryMax": 240000,
    "currency": "INR",
    "showSalary": true,
    "demandStatus": "OPEN_EXTERNAL",
    "requiredCount": 1,
    "targetDate": "2026-07-14T10:00:00Z"
  }
}
```

### Publish a test event manually

```bash
# Enter Kafka container
docker exec -it rms-kafka bash

# Open producer
kafka-console-producer \
  --bootstrap-server localhost:29092 \
  --topic demand-events \
  --property "parse.key=true" \
  --property "key.separator=:"
```

Paste (as a single line):
```
101:{"eventId":"evt-001","eventType":"DEMAND_OPEN_EXTERNAL","payload":{"demandId":101,"roleTitle":"Senior Java Developer","description":"Backend role","skillsRequired":["Java","Spring Boot"],"employmentType":"FULL_TIME","experienceLevel":"Senior","experienceYears":5.0,"workMode":"HYBRID","locationCity":"Bangalore","locationState":"Karnataka","locationCountry":"India","department":"Engineering","jobCategory":"Backend","salaryMin":180000,"salaryMax":240000,"currency":"INR","showSalary":true,"demandStatus":"OPEN_EXTERNAL","requiredCount":1}}
```

### Publish 5 samples via mock producer (when available)

```http
POST http://localhost:8083/api/mock/publish-samples
```

### Verify in Kafka UI

```
http://localhost:8080 → Topics → demand-events → Messages
```

---

## Full End-to-End Test Flow

### Step 1 — Get tokens

```bash
# Recruiter token
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"recruiter@talentgrid.com","password":"recruiter@123"}' \
  | jq -r '.accessToken'

# HM token
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hm@talentgrid.com","password":"hm@123"}' \
  | jq -r '.accessToken'
```

### Step 2 — Publish a demand event

See Kafka section above.

### Step 3 — Verify demand was consumed

```bash
curl -s http://localhost:8082/api/demands \
  -H "Authorization: Bearer <recruiter-token>" | jq
```

### Step 4 — Create job posting from demand

```bash
curl -s -X POST http://localhost:8082/api/job-postings \
  -H "Authorization: Bearer <recruiter-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "demandId": 101,
    "title": "Senior Java Developer",
    "description": "Backend role in Engineering team",
    "skillsRequired": ["Java", "Spring Boot"],
    "employmentType": "FULL_TIME",
    "workMode": "HYBRID",
    "locationCity": "Bangalore",
    "currency": "INR",
    "salaryMin": 180000,
    "salaryMax": 240000
  }' | jq
```

Note the `id` from the response.

### Step 5 — Submit for approval

```bash
curl -s -X POST http://localhost:8082/api/job-postings/1/submit-for-approval \
  -H "Authorization: Bearer <recruiter-token>" | jq
```

### Step 6 — HM checks notifications

```bash
curl -s http://localhost:8082/api/notifications \
  -H "Authorization: Bearer <hm-token>" | jq
```

### Step 7 — HM approves

```bash
curl -s -X POST http://localhost:8082/api/job-postings/1/approve \
  -H "Authorization: Bearer <hm-token>" | jq
```

### Step 8 — Recruiter publishes

```bash
curl -s -X POST http://localhost:8082/api/job-postings/1/publish \
  -H "Authorization: Bearer <recruiter-token>" | jq
```

### Step 9 — Confirm LIVE

```bash
curl -s "http://localhost:8082/api/job-postings?status=LIVE" \
  -H "Authorization: Bearer <recruiter-token>" | jq
```

---

## Database Tables

| Table | Description |
|---|---|
| `demands` | Demand records consumed from Kafka |
| `demand_skills` | Skills collection for demands |
| `job_postings` | Core job posting records |
| `job_posting_skills` | Skills collection for postings |
| `job_posting_approvals` | Audit trail of every status action |
| `notifications` | In-app notifications per user |

**Inspect via psql:**
```bash
docker exec -it rms-postgres psql -U postgres -d job_posting_db

# List tables
\dt

# Check postings
SELECT id, title, status, created_by_email FROM job_postings;

# Check approvals
SELECT * FROM job_posting_approvals ORDER BY action_at DESC;
```

---

## Health Checks

```bash
curl http://localhost:8081/actuator/health   # auth-service
curl http://localhost:8082/actuator/health   # job-posting-service
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `401 Unauthorized` on port 8082 | JWT_SECRET in `.env` must match across both services. Restart both after changing. |
| Demand not appearing after Kafka publish | `docker compose logs job-posting-service --tail=50` — check for consumer errors |
| Auth service fails to start | Postgres may not be ready. Run `docker compose restart user-auth-service` |
| `demand-events` topic missing | Kafka auto-creates it. Or manually: `docker exec rms-kafka kafka-topics --create --topic demand-events --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1` |
| Port 8080 already in use | Kafka UI is on 8080. Stop local processes using that port. |
| Build fails — Maven not found | The Dockerfile installs Maven inside the build stage. Rebuild with `docker compose build --no-cache` |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (set in `.env`) | Shared HMAC key — must be identical for auth and job-posting services |
| `DB_URL` | `jdbc:postgresql://postgres:5432/job_posting_db` | Overridden by docker-compose |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Internal Kafka address inside Docker network |

---

## Frontend Integration Notes

- **Recruiter app** runs on `http://localhost:4200` — already in CORS allowlist
- **Hiring Manager app** runs on `http://localhost:4201` — already in CORS allowlist
- All responses are wrapped in `{ success, message, data }` envelope
- Notifications endpoint is per-user — user id is extracted from the JWT automatically
- Use `GET /api/notifications/unread/count` for the badge counter (polls every 30s recommended)
