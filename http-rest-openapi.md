# 01 - HTTP, REST Principles, and OpenAPI Specification

## Module 7 Requirement Covered

This README covers **Part 1: HTTP + REST** from Module 7 Spring Web. The module asks the project to expose functionality to clients through HTTP, follow REST principles, and document the API using an OpenAPI specification.

Module 7 acceptance expectations for this part:

- API should follow REST principles.
- API should include user-story functionality.
- API should include pagination, filtering, and sorting capabilities where applicable.
- OpenAPI documentation should describe the REST contract.
- The OpenAPI/API documentation should be committed in the project root or documentation folder.

## Project Context

Project name: **Forge FullStack / Forge Recruitment Management System**.

Forge is an internal recruitment management system. It supports the flow from an external hiring demand to a recruiter-authored job posting, hiring-manager approval, publishing, notifications, public career portal listing, and AI-assisted job-description generation.

Main users:

- Recruiter: creates and manages job postings.
- Hiring Manager: approves or declines postings.
- Candidate: views public live jobs and can use the careers chatbot.
- Admin: user and role management through the auth domain.

Main backend services:

- `user-auth-service`: authentication, registration, refresh token, logout, JWT profile endpoints.
- `job-posting-service`: demands, job postings, workflow transitions, notifications, public job listings, SSE events, AI JD generation, recruiter chatbot.
- `nginx-gateway`: single HTTP API entrypoint at `http://localhost:8084`.

## HTTP and Client-Server Communication

Forge exposes server functionality to clients through HTTP APIs. The frontend Angular application, Postman, curl, and browser EventSource clients communicate with the backend through the Nginx API gateway.

Typical request flow:

```text
Angular / Postman / Browser
        |
        v
Nginx Gateway
http://localhost:8084/api
        |
        +-- /api/auth/**              -> user-auth-service:8081
        +-- /api/job-postings/**      -> job-posting-service:8082
        +-- /api/demands/**           -> job-posting-service:8082
        +-- /api/notifications/**     -> job-posting-service:8082
        +-- /api/recruiter-ai/chat    -> job-posting-service:8082
```

Most endpoints require a JWT access token in this header:

```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

Public endpoints do not require authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/oauth-success`
- `GET /api/auth/actuator/health`
- `GET /api/job-postings/public/live`
- `GET /api/job-postings/public/events`
- `POST /api/recruiter-ai/chat`

## REST Design Principles Used

### Resource-Based URLs

Forge API paths are organized around resources:

| Resource | Base Path | Purpose |
|---|---|---|
| Auth | `/api/auth` | Login, register, refresh, logout, current user profile |
| Job Postings | `/api/job-postings` | Job posting CRUD and workflow transitions |
| Demands | `/api/demands` | Demand lookup and available demands for posting creation |
| Notifications | `/api/notifications` | User notification listing and read actions |
| Recruiter AI | `/api/recruiter-ai` | Candidate-facing chatbot |
| Guardrail | `/api/guardrail` | Guardrail processing/evaluation contract endpoints |

### HTTP Methods

Forge uses HTTP methods according to action type:

| Method | Usage in Forge | Example |
|---|---|---|
| `GET` | Read resources | `GET /api/job-postings`, `GET /api/demands/{id}` |
| `POST` | Create resource or run business command | `POST /api/job-postings`, `POST /api/job-postings/{id}/approve` |
| `PUT` | Full update of an existing resource | `PUT /api/job-postings/{id}` |
| `DELETE` | Not currently used in documented job-posting endpoints | TODO if future hard-delete endpoints are added |

Workflow transitions are modeled as command-style subresources:

```text
POST /api/job-postings/{id}/submit-for-approval
POST /api/job-postings/{id}/approve
POST /api/job-postings/{id}/decline
POST /api/job-postings/{id}/publish
POST /api/job-postings/{id}/close
```

This is acceptable for domain workflows because these operations are not simple field updates; they trigger state transitions, audit records, notifications, Kafka events, and SSE events.

### HTTP Status Codes

| Status | Used For |
|---|---|
| `200 OK` | Successful read, update, workflow transition, login, refresh |
| `201 Created` | Successful job posting creation |
| `400 Bad Request` | Validation failures, invalid state, guardrail block |
| `401 Unauthorized` | Missing or invalid JWT |
| `403 Forbidden` | Authenticated user lacks required role/authority |
| `404 Not Found` | Resource ID does not exist |
| `502 Bad Gateway` | AI service unavailable or returned incomplete data |

## Base URLs

| Environment Component | URL |
|---|---|
| Gateway | `http://localhost:8084` |
| API base | `http://localhost:8084/api` |
| Frontend | `http://localhost:4200` |
| Auth service direct | `http://localhost:8085` |
| Job posting service direct | `http://localhost:8082` |
| Kafka UI | `http://localhost:9095` if enabled by compose |

## API Authentication Summary

Login request:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "recruiter@talentgrid.com",
  "password": "recruiter@123"
}
```

Login response includes:

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
    "roles": ["RECRUITER"],
    "permissions": [],
    "businessUnit": null
  }
}
```

The access token is attached by the Angular `authInterceptor` to protected requests.

## OpenAPI Specification Status

### Current Status

The supplied project documentation describes a complete API contract, but no committed `openapi.yaml`, `openapi.yml`, `swagger.yaml`, or Swagger UI configuration was confirmed in the provided files.

Status:

```text
OpenAPI file: TODO / not confirmed
Swagger UI endpoint: TODO / not confirmed
SpringDoc dependency: TODO / not confirmed
```

### Required Action for Module 7

Add one of the following:

```text
openapi.yaml
```

or:

```text
docs/openapi/openapi.yaml
```

Recommended root-level location for Module 7 review:

```text
/openapi.yaml
```

## Proposed OpenAPI Skeleton

Use this skeleton as the starting point for the project OpenAPI contract:

```yaml
openapi: 3.0.3
info:
  title: Forge Recruitment Management API
  version: 1.0.0
  description: REST API for authentication, demands, job postings, notifications, public career portal events, and AI-assisted JD generation.
servers:
  - url: http://localhost:8084/api
    description: Local Nginx gateway
security:
  - bearerAuth: []
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
paths: {}
```

## Endpoint Summary for OpenAPI Documentation

### Auth Service

| Method | Path | Auth | Summary | Request Body | Response |
|---|---|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register user | Register request | Register response |
| `POST` | `/api/auth/login` | No | Login and issue JWT | Login request | Login response + refresh cookie |
| `POST` | `/api/auth/refresh` | Refresh cookie | Issue new access token | None | Login-shaped response |
| `POST` | `/api/auth/logout` | Yes | Blacklist token and revoke refresh token | None | Message response |
| `GET` | `/api/auth/me` | Yes | Current user profile | None | User profile |
| `GET` | `/api/auth/oauth-success` | No | OAuth success callback | Query params | Login-shaped response |
| `GET` | `/api/auth/actuator/health` | No | Health check | None | `{ "status": "UP" }` |

### Diagnostics

| Method | Path | Auth | Summary |
|---|---|---|---|
| `GET` | `/api/test/secure` | Yes | Verify JWT and authorities |
| `GET` | `/api/test/admin` | Admin authority | Verify admin RBAC |

### Job Postings

| Method | Path | Auth | Summary | Status |
|---|---|---|---|---|
| `POST` | `/api/job-postings` | Recruiter | Create DRAFT posting | `201` |
| `GET` | `/api/job-postings` | Yes | List postings, optional status filter | `200` |
| `GET` | `/api/job-postings/{id}` | Yes | Get posting by ID | `200` |
| `PUT` | `/api/job-postings/{id}` | Recruiter | Update DRAFT/DECLINED posting | `200` |
| `POST` | `/api/job-postings/{id}/save-draft` | Recruiter | Save partial draft changes | `200` |
| `POST` | `/api/job-postings/{id}/submit-for-approval` | Recruiter | Move to PENDING_APPROVAL | `200` |
| `POST` | `/api/job-postings/{id}/approve` | Hiring Manager | Move to READY_TO_PUBLISH | `200` |
| `POST` | `/api/job-postings/{id}/decline` | Hiring Manager | Move to DECLINED | `200` |
| `POST` | `/api/job-postings/{id}/publish` | Recruiter | Move to LIVE | `200` |
| `POST` | `/api/job-postings/{id}/close` | Yes | Move to CLOSED | `200` |
| `POST` | `/api/job-postings/{id}/channels/{channel}/publish` | Yes | Mark channel live | `200` |
| `GET` | `/api/job-postings/{id}/approvals` | Yes | Approval audit trail | `200` |
| `GET` | `/api/job-postings/stats` | Yes | Dashboard status counts | `200` |
| `POST` | `/api/job-postings/generate-jd` | Yes | Generate AI JD sections | `200` / `400` / `502` |
| `GET` | `/api/job-postings/public/live` | No | Public careers listing data | `200` |
| `GET` | `/api/job-postings/public/events` | No | SSE public job updates | `text/event-stream` |

### Demands

| Method | Path | Auth | Summary |
|---|---|---|---|
| `GET` | `/api/demands` | Yes | List demands available for job posting creation |
| `GET` | `/api/demands/{id}` | Yes | Get demand by database ID |
| `GET` | `/api/demands/by-demand-id/{demandId}` | Yes | Get demand by external demand ID |

### Notifications

| Method | Path | Auth | Summary |
|---|---|---|---|
| `GET` | `/api/notifications` | Yes | List current user's notifications |
| `GET` | `/api/notifications/unread` | Yes | List unread notifications |
| `GET` | `/api/notifications/unread/count` | Yes | Count unread notifications |
| `PUT` | `/api/notifications/read-all` | Yes | Mark all notifications read |
| `PUT` | `/api/notifications/{id}/read` | Yes | Mark one notification read |

### Recruiter AI Chatbot

| Method | Path | Auth | Summary |
|---|---|---|---|
| `POST` | `/api/recruiter-ai/chat` | No | Candidate-facing AI chatbot |

### Guardrail

| Method | Path | Auth | Summary |
|---|---|---|---|
| `POST` | `/api/guardrail/evaluate` | Yes | Evaluate prompt safety |
| `POST` | `/api/guardrail/process` | Yes | Process/sanitize prompt |

## Main Schemas to Document

### `JobPostingResponse`

Important fields:

| Field | Type | Description |
|---|---|---|
| `id` | number | Database ID |
| `demandId` | number/string | Linked demand ID |
| `title` | string | Job title |
| `description` | string | Main JD description |
| `responsibilities` | string | Responsibilities section |
| `requirements` | string | Requirements section |
| `benefits` | string | Benefits section |
| `employmentType` | string | Full-time, part-time, etc. |
| `level` | string | Seniority such as SENIOR |
| `experienceYears` | number | Required experience |
| `workMode` | string | REMOTE, HYBRID, ONSITE |
| `locationCity` | string | City |
| `locationState` | string | State |
| `locationCountry` | string | Country |
| `department` | string | Department |
| `jobCategory` | string | Category |
| `skills` | array<string> | Required skills |
| `salaryMin` | number | Minimum salary |
| `salaryMax` | number | Maximum salary |
| `currency` | string | Currency |
| `showSalary` | boolean | Whether salary is public |
| `applicationDeadline` | date | Application deadline |
| `postingStatus` | string | DRAFT, PENDING_APPROVAL, READY_TO_PUBLISH, LIVE, DECLINED, CLOSED |
| `approvalStatus` | string | Approval status |
| `channels` | array | Portal/linkedin/indeed channel state |
| `analytics` | object | Views/clicks metrics |

### `GenerateJdRequest`

| Field | Type | Required | Description |
|---|---|---|---|
| `demandId` | string | No | Demand ID for traceability |
| `roleTitle` | string | Intended yes | Role name |
| `department` | string | No | Department |
| `location` | string | No | Location |
| `workMode` | string | No | REMOTE/HYBRID/ONSITE |
| `experienceYears` | string | No | Experience requirement |
| `skillsRequired` | array<string> | No | Skill list |
| `seniorityLevel` | string | No | Seniority |
| `employmentType` | string | No | Employment type |
| `additionalContext` | string | No | Free text scanned by guardrail |

Note: project notes state that this endpoint currently does not enforce `@Valid` on the request object, so Bean Validation annotations on `GenerateJdRequest` are not active until `@Valid` is added.

### `JdSectionsResponse`

| Field | Type | Description |
|---|---|---|
| `summary` | string | AI-generated summary |
| `responsibilities` | string | AI-generated responsibilities |
| `requirements` | string | AI-generated requirements |
| `benefits` | string | AI-generated benefits |
| `sectionCount` | number | Number of generated sections |

## Pagination, Filtering, Sorting in OpenAPI

Current documented job posting list endpoint supports:

```text
GET /api/job-postings?status=PENDING_APPROVAL
```

Confirmed filter:

| Parameter | Type | Description |
|---|---|---|
| `status` | string | Optional filter by posting status |

Module 7 still expects page/size/sort support. Document these as TODO unless implemented in code:

```text
GET /api/job-postings?page=0&size=10&sort=createdAt,desc&status=LIVE
```

Required OpenAPI parameters to add when implemented:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Zero-based page number |
| `size` | integer | No | Items per page |
| `sort` | string | No | `field,direction` format |
| `status` | string | No | Exact status filter |

## Error Response Documentation

Current documented errors are custom JSON maps, not confirmed RFC 7807 ProblemDetail.

Example AI service unavailable:

```json
{
  "error": "AI service unavailable",
  "message": "Team4 AI service did not respond. Please try again later."
}
```

Example guardrail block:

```json
{
  "message": "Prompt was blocked by security guardrail"
}
```

Module 7 TODO:

- Standardize all `400+` errors using RFC 7807 / Spring `ProblemDetail`.
- Include all field validation errors in one response.
- Document error schemas in OpenAPI.

## Demo Checklist for This Part

1. Start services with Docker Compose.
2. Open Postman.
3. Login using recruiter credentials.
4. Copy JWT access token.
5. Call `GET /api/job-postings` through the gateway.
6. Call `GET /api/job-postings?status=PENDING_APPROVAL` to demonstrate filtering.
7. Open `openapi.yaml` when added and show endpoint documentation.
8. Explain TODOs clearly if OpenAPI file, pagination, or RFC 7807 are still pending.

## TODO for Module 7 Completion

- Add committed `openapi.yaml` or generated OpenAPI docs.
- Add schemas for auth, job posting, demand, notification, AI JD, chatbot, and error responses.
- Add pagination parameters to get-all endpoints.
- Add filtering and sorting beyond `status`.
- Standardize error responses using RFC 7807 `ProblemDetail`.
