# API Reference Index

> Base URL (via Nginx gateway): `http://localhost:8084/api`
> Auth service routes: `/api/auth/**` → port 8081
> Job-posting service routes: `/api/**` → port 8082

---

## Auth Service (`/api/auth`)

| Method | Endpoint | Description | Frontend Call |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | Authenticate user, returns JWT | `auth.service.ts` |
| POST | `/api/auth/register` | Register new user | — |
| GET | `/api/auth/me` | Get current user from token | `auth.service.ts` |
| POST | `/api/auth/refresh` | Refresh access token | `auth.service.ts` |
| POST | `/api/auth/logout` | Invalidate session | `auth.service.ts` |
| GET | `/api/auth/oauth-success` | OAuth redirect handler | — |

---

## Demands (`/api/demands`)

| Method | Endpoint | Description | Frontend Call |
|--------|----------|-------------|---------------|
| GET | `/api/demands` | List all demands | `job-posting.service.ts` |
| GET | `/api/demands/{id}` | Get demand by DB id | — |
| GET | `/api/demands/by-demand-id/{demandId}` | Get demand by external demand id | — |

---

## Job Postings (`/api/job-postings`)

| Method | Endpoint | Description | Frontend Call |
|--------|----------|-------------|---------------|
| POST | `/api/job-postings` | Create new job posting | `job-posting.service.ts` |
| GET | `/api/job-postings` | List all job postings | `job-posting.service.ts` |
| GET | `/api/job-postings/{id}` | Get single job posting | `job-posting.service.ts` |
| PUT | `/api/job-postings/{id}` | Update job posting fields | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/save-draft` | Save as draft | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/submit-for-approval` | Submit to HM for approval | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/approve` | HM approves posting | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/decline` | HM declines posting | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/publish` | Mark posting as published | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/close` | Close / unpublish posting | `job-posting.service.ts` |
| POST | `/api/job-postings/{id}/channels/{channel}/publish` | Publish to specific channel (linkedin / indeed / portal) | `job-posting.service.ts` |
| POST | `/api/job-postings/generate-jd` | AI-generate job description (via Team 4 ai-service) | `job-posting-api.service.ts` |
| GET | `/api/job-postings/{id}/approvals` | Get approval audit trail for a posting | — |

---

## Recruiter AI (`/api/recruiter-ai`)

| Method | Endpoint | Auth | Description | Frontend Call |
|--------|----------|------|-------------|---------------|
| POST | `/api/recruiter-ai/chat` | None (public) | Career chatbot — proxied to Team 4 ai-service after guardrail check | `chat.service.ts` (careers app) |

---

## Notifications (`/api/notifications`)

| Method | Endpoint | Description | Frontend Call |
|--------|----------|-------------|---------------|
| GET | `/api/notifications` | List all notifications for current user | `notification.service.ts` |
| GET | `/api/notifications/unread` | List unread notifications | — |
| GET | `/api/notifications/unread/count` | Count of unread notifications | — |
| PUT | `/api/notifications/read-all` | Mark all notifications as read | — |
| PUT | `/api/notifications/{id}/read` | Mark single notification as read | — |

---

## Test (internal only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/test/secure` | Verify JWT auth is working |
| GET | `/api/test/admin` | Verify admin role access |

---

## Gap Analysis

Endpoints the **backend exposes but frontend does not call**:

| Endpoint | Notes |
|----------|-------|
| `POST /api/auth/register` | No registration UI yet |
| `GET /api/auth/oauth-success` | OAuth flow not wired in frontend |
| `GET /api/demands/{id}` | Frontend uses list only |
| `GET /api/demands/by-demand-id/{demandId}` | Unused by frontend |
| `GET /api/job-postings/{id}/approvals` | Approval history not shown in UI |
| `GET /api/notifications/unread` | Frontend fetches all, not unread-only |
| `GET /api/notifications/unread/count` | Frontend derives count from loaded list |
| `PUT /api/notifications/read-all` | Frontend marks read locally, no backend call |
| `PUT /api/notifications/{id}/read` | Same — local-only |
| `GET /api/test/secure` | Dev/test only |
| `GET /api/test/admin` | Dev/test only |

---

## Kafka Events (not HTTP)

| Topic | Direction | Producer | Consumer |
|-------|-----------|----------|----------|
| `demand-events` | inbound | External resourcing system | `job-posting-service` |
| `portal-job-events` | outbound | `job-posting-service` | Career portal |
| `portal-confirmations` | inbound | Career portal | `job-posting-service` |
