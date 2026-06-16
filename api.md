# Forge API Documentation

**Base URL:** `http://localhost:8084`  
**Auth header:** `Authorization: Bearer <access_token>`  
**Content-Type:** `application/json`

---

## Authentication

All endpoints except `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/job-postings/public/*` require a valid JWT in the `Authorization` header.

---

## Table of Contents

- [Auth Service](#auth-service)
  - [POST /api/auth/register](#post-apiauthregister)
  - [POST /api/auth/login](#post-apiauthlogin)
  - [POST /api/auth/refresh](#post-apiauthrefresh)
  - [POST /api/auth/logout](#post-apiauthlogout)
  - [GET /api/auth/me](#get-apiauthme)
  - [GET /api/auth/oauth-success](#get-apiauthoauth-success)
- [Job Postings](#job-postings)
  - [POST /api/job-postings](#post-apijob-postings)
  - [GET /api/job-postings](#get-apijob-postings)
  - [GET /api/job-postings/:id](#get-apijob-postingsid)
  - [PUT /api/job-postings/:id](#put-apijob-postingsid)
  - [POST /api/job-postings/:id/save-draft](#post-apijob-postingsidsave-draft)
  - [POST /api/job-postings/:id/submit-for-approval](#post-apijob-postingsidsubmit-for-approval)
  - [POST /api/job-postings/:id/approve](#post-apijob-postingsidapprove)
  - [POST /api/job-postings/:id/decline](#post-apijob-postingsiddecline)
  - [POST /api/job-postings/:id/publish](#post-apijob-postingsidpublish)
  - [POST /api/job-postings/:id/close](#post-apijob-postingsidclose)
  - [POST /api/job-postings/:id/channels/:channel/publish](#post-apijob-postingsidchannelschannelpublish)
  - [GET /api/job-postings/:id/approvals](#get-apijob-postingsidapprovals)
  - [GET /api/job-postings/stats](#get-apijob-postingsstats)
  - [POST /api/job-postings/generate-jd](#post-apijob-postingsgenerate-jd)
  - [GET /api/job-postings/public/live](#get-apijob-postingspubliclive)
  - [GET /api/job-postings/public/events](#get-apijob-postingspublicevents)
- [Demands](#demands)
  - [GET /api/demands](#get-apidemands)
  - [GET /api/demands/:id](#get-apidemandsid)
  - [GET /api/demands/by-demand-id/:demandId](#get-apidemandsbydemandiddemandid)
- [Notifications](#notifications)
  - [GET /api/notifications](#get-apinotifications)
  - [GET /api/notifications/unread](#get-apinotificationsunread)
  - [GET /api/notifications/unread/count](#get-apinotificationsunreadcount)
  - [PUT /api/notifications/read-all](#put-apinotificationsread-all)
  - [PUT /api/notifications/:id/read](#put-apinotificationsidread)

---

## Auth Service

Base path: `/api/auth`  
Routed to: `user-auth-service:8081`

---

### POST /api/auth/register

Register a new user account.

**Auth required:** No

**Request body:**

| Field | Type | Required | Description |
|---|---|---|---|
| username | string | Yes | Display name |
| email | string | Yes | Unique email address |
| password | string | Yes | Plain text (hashed server-side) |
| role | string | No | `RECRUITER`, `HIRING_MANAGER`, `ADMIN` (defaults to seeded role) |

```json
{
  "username": "Jane Smith",
  "email": "jane@company.com",
  "password": "password123",
  "role": "RECRUITER"
}
```

**Response `200 OK`:**

```json
{
  "id": 5,
  "email": "jane@company.com",
  "message": "User registered successfully"
}
```

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Validation failed (missing fields) |
| 409 | Email already registered |

---

### POST /api/auth/login

Authenticate and receive a JWT access token.

**Auth required:** No

**Request body:**

| Field | Type | Required |
|---|---|---|
| email | string | Yes |
| password | string | Yes |

```json
{
  "email": "recruiter@talentgrid.com",
  "password": "recruiter@123"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyZWNydWl0...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 2,
    "name": "recruiter1",
    "email": "recruiter@talentgrid.com",
    "role": "RECRUITER",
    "roles": ["RECRUITER"],
    "permissions": [],
    "businessUnit": null
  }
}
```

A `refreshToken` HttpOnly cookie (`SameSite=Lax`) is also set on the response and is used by `/api/auth/refresh`.

**Error responses:**

| Status | Reason |
|---|---|
| 401 | Invalid email or password |

---

### POST /api/auth/refresh

Issue a new access token using the refresh token cookie. No request body needed.

**Auth required:** No (uses `refreshToken` cookie)

**Response `200 OK`:** Same shape as login response with a new `accessToken`.

**Error responses:**

| Status | Reason |
|---|---|
| 400 | `refreshToken` cookie missing or expired |

---

### POST /api/auth/logout

Blacklist the current access token in Redis and revoke the refresh token.

**Auth required:** Yes

**Response `200 OK`:**

```json
{
  "message": "Logged out successfully"
}
```

The `refreshToken` cookie is cleared (maxAge=0).

---

### GET /api/auth/me

Return the currently authenticated user's profile.

**Auth required:** Yes

**Response `200 OK`:**

```json
{
  "id": 3,
  "name": "hiringmanager1",
  "email": "hm@talentgrid.com",
  "role": "HIRING_MANAGER",
  "roles": ["HIRING_MANAGER"],
  "permissions": [],
  "businessUnit": null
}
```

**Error responses:**

| Status | Reason |
|---|---|
| 401 | Missing or invalid token |

---

### GET /api/auth/oauth-success

Callback endpoint after Google OAuth2 SSO. Called internally by the OAuth flow — not called directly by the frontend.

**Auth required:** No

**Query params:**

| Param | Type | Description |
|---|---|---|
| email | string | Email returned from Google |

**Response `200 OK`:** Same shape as login response. Creates the user with `CANDIDATE` role if first sign-in.

---

## Job Postings

Base path: `/api/job-postings`  
Routed to: `job-posting-service:8082`

---

### Job Posting Object

All endpoints that return a job posting use this shape:

```json
{
  "id": 1,
  "demandId": 42,
  "title": "Senior Java Developer",
  "description": "Full description...",
  "responsibilities": "Lead design and delivery...",
  "requirements": "5+ years Java experience...",
  "benefits": "Competitive salary, remote work...",
  "employmentType": "Full-time",
  "level": "SENIOR",
  "experienceYears": 5,
  "workMode": "REMOTE",
  "locationCity": "New York",
  "locationState": "NY",
  "locationCountry": "US",
  "department": "Engineering",
  "jobCategory": "Backend",
  "skills": ["Java", "Spring Boot", "Kafka"],
  "budget": null,
  "requiredCount": 2,
  "currency": "USD",
  "salaryMin": 120000,
  "salaryMax": 160000,
  "showSalary": true,
  "applicationDeadline": "2026-09-01",
  "previousStatus": "PENDING_APPROVAL",
  "postingStatus": "READY_TO_PUBLISH",
  "approvalStatus": "APPROVED",
  "declineReason": null,
  "recruiterId": 2,
  "approvedBy": 3,
  "approvedAt": "2026-06-15T19:32:00.128",
  "publishedAt": null,
  "createdAt": "2026-06-15T19:31:22.328",
  "updatedAt": "2026-06-15T19:32:00.310",
  "channels": [
    { "key": "linkedin", "label": "LinkedIn", "state": "idle" },
    { "key": "indeed", "label": "Indeed", "state": "idle" },
    { "key": "portal", "label": "Careers Portal", "state": "pending" }
  ],
  "analytics": {
    "views": 0,
    "clicks": 0
  }
}
```

**`postingStatus` values:**

| Value | Meaning |
|---|---|
| `DRAFT` | Created by recruiter, not yet submitted |
| `PENDING_APPROVAL` | Submitted to HM for review |
| `READY_TO_PUBLISH` | Approved by HM, visible on career portal |
| `LIVE` | Manually published by recruiter |
| `DECLINED` | Rejected by HM |
| `CLOSED` | Manually closed or auto-expired |

---

### POST /api/job-postings

Create a new job posting. Starts in `DRAFT` status.

**Auth required:** Yes (RECRUITER)

**Request body:**

| Field | Type | Required | Description |
|---|---|---|---|
| title | string | **Yes** | Job title |
| demandId | number | No | Links to a demand record |
| description | string | No | Full job description |
| responsibilities | string | No | What the candidate will do |
| requirements | string | No | Required qualifications |
| benefits | string | No | Perks and compensation |
| employmentType | string | No | `Full-time`, `Part-time`, `Contract` |
| level | string | No | `JUNIOR`, `MID`, `SENIOR`, `LEAD` |
| experienceYears | number | No | Minimum years required |
| workMode | string | No | `REMOTE`, `HYBRID`, `ON_SITE` |
| locationCity | string | No | |
| locationState | string | No | |
| locationCountry | string | No | |
| department | string | No | |
| jobCategory | string | No | |
| skills | string[] | No | List of required skills |
| salaryMin | number | No | |
| salaryMax | number | No | |
| currency | string | No | Default: `USD` |
| showSalary | boolean | No | Whether to show salary on portal |
| requiredCount | number | No | Number of open seats |
| applicationDeadline | string | No | `YYYY-MM-DD` format |

```json
{
  "demandId": 42,
  "title": "Senior Java Developer",
  "description": "We are hiring...",
  "responsibilities": "Lead backend systems...",
  "requirements": "5+ years Java...",
  "benefits": "Remote, equity, health...",
  "employmentType": "Full-time",
  "level": "SENIOR",
  "experienceYears": 5,
  "workMode": "REMOTE",
  "locationCity": "New York",
  "locationState": "NY",
  "locationCountry": "US",
  "department": "Engineering",
  "jobCategory": "Backend",
  "skills": ["Java", "Spring Boot", "Kafka"],
  "salaryMin": 120000,
  "salaryMax": 160000,
  "currency": "USD",
  "showSalary": true,
  "requiredCount": 2,
  "applicationDeadline": "2026-09-01"
}
```

**Response `201 Created`:** [Job Posting Object](#job-posting-object)

---

### GET /api/job-postings

List all job postings. Optionally filter by status.

**Auth required:** Yes

**Query params:**

| Param | Type | Description |
|---|---|---|
| status | string | Filter by `postingStatus` value. Omit for all. |

**Examples:**
```
GET /api/job-postings
GET /api/job-postings?status=PENDING_APPROVAL
GET /api/job-postings?status=READY_TO_PUBLISH
```

**Response `200 OK`:** Array of [Job Posting Objects](#job-posting-object)

---

### GET /api/job-postings/:id

Get a single job posting by its database ID.

**Auth required:** Yes

**Response `200 OK`:** [Job Posting Object](#job-posting-object)

**Error responses:**

| Status | Reason |
|---|---|
| 404 | Posting not found |

---

### PUT /api/job-postings/:id

Update a job posting. Only allowed when `postingStatus` is `DRAFT` or `DECLINED`.

**Auth required:** Yes (RECRUITER)

**Request body:** Same fields as [POST /api/job-postings](#post-apijob-postings)

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object)

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Posting is not in DRAFT or DECLINED state |
| 404 | Posting not found |

---

### POST /api/job-postings/:id/save-draft

Save changes to a DRAFT or DECLINED posting without changing its status.

**Auth required:** Yes (RECRUITER)

**Request body:** Any subset of [CreateJobPostingRequest](#post-apijob-postings) fields (all optional)

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object)

---

### POST /api/job-postings/:id/submit-for-approval

Transition a `DRAFT` or `DECLINED` posting to `PENDING_APPROVAL`. Sends an in-app notification to the Hiring Manager.

**Auth required:** Yes (RECRUITER)

**Request body:** None

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object) with `postingStatus: "PENDING_APPROVAL"`

**Side effects:**
- Notification sent to HM: `"New Job Posting Requires Approval"`

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Posting is not in DRAFT or DECLINED state |
| 404 | Posting not found |

---

### POST /api/job-postings/:id/approve

HM approves a `PENDING_APPROVAL` posting. Transitions to `READY_TO_PUBLISH`.

**Auth required:** Yes (HIRING_MANAGER)

**Request body:** None

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object) with `postingStatus: "READY_TO_PUBLISH"`

**Side effects:**
- Kafka event produced to `portal-job-events` topic (`JOB_PUBLISHED`)
- SSE broadcast `JOB_PUBLISHED` to all connected career portal clients (real-time update)
- Notification sent to recruiter: `"Job Posting Approved"`

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Posting is not in PENDING_APPROVAL state |
| 404 | Posting not found |

---

### POST /api/job-postings/:id/decline

HM declines a `PENDING_APPROVAL` posting. Transitions to `DECLINED`.

**Auth required:** Yes (HIRING_MANAGER)

**Request body:**

| Field | Type | Required |
|---|---|---|
| reason | string | Yes |

```json
{
  "reason": "Job description is incomplete. Please add responsibilities."
}
```

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object) with `postingStatus: "DECLINED"`

**Side effects:**
- Notification sent to recruiter with decline reason

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Posting is not in PENDING_APPROVAL state |
| 404 | Posting not found |

---

### POST /api/job-postings/:id/publish

Manually transition a `READY_TO_PUBLISH` posting to `LIVE`.

**Auth required:** Yes (RECRUITER)

**Request body:** None

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object) with `postingStatus: "LIVE"`

**Side effects:**
- Notification sent to recruiter: `"Job Posting is Now Live"`

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Posting is not in READY_TO_PUBLISH state |
| 404 | Posting not found |

---

### POST /api/job-postings/:id/close

Close a `LIVE` or `READY_TO_PUBLISH` posting. Transitions to `CLOSED`.

**Auth required:** Yes

**Request body:** None

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object) with `postingStatus: "CLOSED"`

**Side effects:**
- Kafka event produced to `portal-job-events` topic (`JOB_UNPUBLISHED`)
- SSE broadcast `JOB_UNPUBLISHED` to all connected career portal clients (job card removed in real time)
- Notification sent to recruiter: `"Job Posting Closed"`

**Error responses:**

| Status | Reason |
|---|---|
| 400 | Posting is not in LIVE or READY_TO_PUBLISH state |
| 404 | Posting not found |

---

### POST /api/job-postings/:id/channels/:channel/publish

Mark a specific distribution channel as `live` for a posting.

**Auth required:** Yes

**Path params:**

| Param | Description |
|---|---|
| id | Job posting ID |
| channel | Channel key: `linkedin`, `indeed`, `portal` |

**Request body:** None

**Response `200 OK`:** Updated [Job Posting Object](#job-posting-object) with the channel `state` set to `"live"`

---

### GET /api/job-postings/:id/approvals

Get the full approval audit trail for a posting.

**Auth required:** Yes

**Response `200 OK`:**

```json
[
  {
    "id": 1,
    "jobPostingId": 42,
    "action": "SUBMITTED",
    "comments": "",
    "actionByEmail": "recruiter@talentgrid.com",
    "actionAt": "2026-06-15T19:31:50"
  },
  {
    "id": 2,
    "jobPostingId": 42,
    "action": "APPROVED",
    "comments": "",
    "actionByEmail": "hm@talentgrid.com",
    "actionAt": "2026-06-15T19:32:00"
  }
]
```

**`action` values:** `SUBMITTED`, `APPROVED`, `DECLINED`, `PUBLISHED`

---

### GET /api/job-postings/stats

Return counts of job postings grouped by status. Used by the dashboard KPI tiles.

**Auth required:** Yes

**Response `200 OK`:**

```json
{
  "DRAFT": 3,
  "PENDING_APPROVAL": 1,
  "READY_TO_PUBLISH": 2,
  "LIVE": 5,
  "DECLINED": 1,
  "CLOSED": 8
}
```

---

### POST /api/job-postings/generate-jd

Generate AI-written job description sections (Team 4 contract endpoint).

**Auth required:** Yes

**Request body:**

| Field | Type | Required | Description |
|---|---|---|---|
| roleTitle | string | No | Job title |
| skillsRequired | string[] | No | List of skills |
| seniorityLevel | string | No | `JUNIOR`, `MID`, `SENIOR`, `LEAD` |
| department | string | No | Department name |
| location | string | No | City / location string |
| workMode | string | No | `REMOTE`, `HYBRID`, `ON_SITE` |
| experienceYears | number | No | Years of experience |

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

**Response `200 OK`:**

```json
{
  "summary": "We are looking for a SENIOR Senior Backend Engineer to join our Engineering team...",
  "responsibilities": "• Lead design and delivery of Senior Backend Engineer solutions.\n• Collaborate with product...",
  "requirements": "• 5 years of experience as Senior Backend Engineer or equivalent.\n• Strong proficiency in: Java, Spring Boot, Kafka...",
  "benefits": "• Competitive compensation and equity package.\n• Flexible work arrangements...",
  "sectionCount": 4
}
```

---

### GET /api/job-postings/public/live

Return all postings with status `READY_TO_PUBLISH` or `LIVE`. This is the data source for the career portal job listings page.

**Auth required:** No

**Response `200 OK`:** Array of [Job Posting Objects](#job-posting-object)

```bash
curl http://localhost:8084/api/job-postings/public/live
```

---

### GET /api/job-postings/public/events

Server-Sent Events stream. The career portal subscribes to this endpoint on page load to receive real-time job updates without polling.

**Auth required:** No  
**Content-Type:** `text/event-stream`  
**Connection:** Long-lived (nginx timeout set to 3600s)

**Events:**

| Event name | When fired | Payload |
|---|---|---|
| `connected` | Immediately on subscribe | `{ "subscribers": 3 }` |
| `JOB_PUBLISHED` | When HM approves a posting | Full [Job Posting Object](#job-posting-object) |
| `JOB_UNPUBLISHED` | When posting is closed or expires | `{ "jobId": 123 }` |

**Example stream:**
```
event:connected
data:{"subscribers":1}

event:JOB_PUBLISHED
data:{"id":5,"title":"Frontend Engineer","postingStatus":"READY_TO_PUBLISH",...}

event:JOB_UNPUBLISHED
data:{"jobId":3}
```

**Test with curl:**
```bash
curl -N http://localhost:8084/api/job-postings/public/events
```

---

## Demands

Base path: `/api/demands`  
Routed to: `job-posting-service:8082`

Demands are created automatically when a `DEMAND_OPEN_EXTERNAL` Kafka event is consumed. They cannot be created via REST.

---

### Demand Object

```json
{
  "id": 1,
  "demandId": 1001,
  "ref": "DEM-1001",
  "title": "Senior Java Developer",
  "description": null,
  "level": "SENIOR",
  "employmentType": "Full-time",
  "experienceYears": 5.0,
  "workMode": "REMOTE",
  "locationCity": "New York",
  "locationState": "NY",
  "locationCountry": "US",
  "department": "Engineering",
  "jobCategory": null,
  "skills": ["Java", "Spring Boot", "Kafka"],
  "requiredCount": 2,
  "filledCount": 0,
  "priority": "MEDIUM",
  "budgetMin": 0.0,
  "budgetMax": 0.0,
  "currency": "USD",
  "showSalary": false,
  "demandStatus": "OPEN_EXTERNAL",
  "targetDate": "2026-09-01",
  "aiRecommended": false,
  "receivedAt": "2026-06-15T10:00:00"
}
```

---

### GET /api/demands

List all demands that are available for job posting creation (no active linked posting).

**Auth required:** Yes

**Response `200 OK`:** Array of [Demand Objects](#demand-object)

---

### GET /api/demands/:id

Get a single demand by its database primary key.

**Auth required:** Yes

**Response `200 OK`:** [Demand Object](#demand-object)

**Error responses:**

| Status | Reason |
|---|---|
| 404 | Demand not found |

---

### GET /api/demands/by-demand-id/:demandId

Get a demand by the external `demandId` field (the ID from the Kafka event payload).

**Auth required:** Yes

**Response `200 OK`:** [Demand Object](#demand-object)

**Error responses:**

| Status | Reason |
|---|---|
| 404 | Demand not found |

---

## Notifications

Base path: `/api/notifications`  
Routed to: `job-posting-service:8082`

Notifications are created automatically by the system on workflow transitions (submit, approve, decline, publish, close, expire). They cannot be created via REST.

---

### Notification Object

```json
{
  "id": 7,
  "userId": 2,
  "jobPostingId": 1,
  "title": "Job Posting Approved",
  "message": "'Senior Java Developer' has been approved and is being sent to the career portal.",
  "isRead": false,
  "createdAt": "2026-06-15T19:32:00"
}
```

---

### GET /api/notifications

Get all notifications for the current user, newest first.

**Auth required:** Yes

**Response `200 OK`:** Array of [Notification Objects](#notification-object)

---

### GET /api/notifications/unread

Get only unread notifications for the current user.

**Auth required:** Yes

**Response `200 OK`:** Array of [Notification Objects](#notification-object) where `isRead: false`

---

### GET /api/notifications/unread/count

Get the count of unread notifications. Used by the notification badge in the header.

**Auth required:** Yes

**Response `200 OK`:**

```json
{
  "count": 3
}
```

---

### PUT /api/notifications/read-all

Mark all notifications for the current user as read.

**Auth required:** Yes

**Response `204 No Content`**

---

### PUT /api/notifications/:id/read

Mark a single notification as read.

**Auth required:** Yes

**Response `204 No Content`**

**Error responses:**

| Status | Reason |
|---|---|
| 404 | Notification not found or does not belong to this user |

---

## Error Response Format

All error responses follow this shape:

```json
{
  "error": "Description of what went wrong"
}
```

**Common status codes:**

| Status | Meaning |
|---|---|
| 400 | Bad request — invalid state transition or missing required field |
| 401 | Unauthorized — missing, expired, or blacklisted JWT |
| 403 | Forbidden — authenticated but insufficient role |
| 404 | Resource not found |
| 500 | Internal server error |

---

## Seeded Credentials

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@griddynamics.com | admin@123 |
| RECRUITER | recruiter@talentgrid.com | recruiter@123 |
| HIRING_MANAGER | hm@talentgrid.com | hm@123 |

---

## Quick Reference — curl Examples

```bash
# Login
curl -sc cookies.txt -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"recruiter@talentgrid.com","password":"recruiter@123"}'

# Export token (paste from login response)
TOKEN=eyJhbGciOiJIUzI1NiJ9...

# List all job postings
curl http://localhost:8084/api/job-postings \
  -H "Authorization: Bearer $TOKEN"

# List pending approvals
curl "http://localhost:8084/api/job-postings?status=PENDING_APPROVAL" \
  -H "Authorization: Bearer $TOKEN"

# Create a job posting
curl -X POST http://localhost:8084/api/job-postings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Backend Engineer","demandId":1,"skills":["Java"],"level":"SENIOR"}'

# Submit for approval
curl -X POST http://localhost:8084/api/job-postings/1/submit-for-approval \
  -H "Authorization: Bearer $TOKEN"

# Approve (login as HM first)
HM_TOKEN=eyJhbGciOiJIUzI1NiJ9...
curl -X POST http://localhost:8084/api/job-postings/1/approve \
  -H "Authorization: Bearer $HM_TOKEN"

# Career portal — public live jobs (no auth)
curl http://localhost:8084/api/job-postings/public/live

# Career portal — SSE stream (no auth, keep connection open)
curl -N http://localhost:8084/api/job-postings/public/events

# Get demands
curl http://localhost:8084/api/demands \
  -H "Authorization: Bearer $TOKEN"

# Generate JD
curl -X POST http://localhost:8084/api/job-postings/generate-jd \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleTitle":"Backend Engineer","skillsRequired":["Java","Kafka"],"seniorityLevel":"SENIOR"}'

# Dashboard stats
curl http://localhost:8084/api/job-postings/stats \
  -H "Authorization: Bearer $TOKEN"

# Notifications
curl http://localhost:8084/api/notifications \
  -H "Authorization: Bearer $TOKEN"

# Mark all notifications read
curl -X PUT http://localhost:8084/api/notifications/read-all \
  -H "Authorization: Bearer $TOKEN"
```
