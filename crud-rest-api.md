# 04 - CRUD REST APIs

## Module 7 Requirement Covered

This README covers **Part 4: CRUDs everywhere** from Module 7 Spring Web. It documents implemented create, read, update, and delete-style REST functionality in Forge.

Module 7 CRUD checklist:

- Endpoints work according to user stories.
- Controller layer should use DTOs instead of exposing entities.
- Endpoints return valid HTTP status codes.
- Read operation should support find by ID and find all.
- Integration tests should cover endpoints using MockMvc.
- Postman collection should be prepared for demo.

## CRUD Meaning in Forge

CRUD means:

| Operation | Meaning | Forge Examples |
|---|---|---|
| Create | Add a new resource | Create job posting, register user |
| Read | Fetch one or many resources | Get job posting, list demands, list notifications |
| Update | Modify existing resource | Update posting, save draft, mark notification read |
| Delete | Remove resource | Hard delete is not exposed in documented endpoints; workflow close is used for postings |

Forge also has workflow operations that are not pure CRUD but are important domain commands:

- Submit posting for approval.
- Approve posting.
- Decline posting.
- Publish posting.
- Close posting.
- Generate JD using AI.

## Domain Resources

## Resource: Auth User

| Item | Value |
|---|---|
| Service | `user-auth-service` |
| Controller | `AuthController` |
| Entity/Table | `users`, `roles`, `scopes`, `refresh_tokens` |
| Repository | User/role/refresh token repositories |
| Request DTOs | Register request, login request |
| Response DTOs | Register response, login response, user profile response |
| CRUD Scope | Create/register and read current user profile; update/delete not exposed as public documented CRUD |

## Resource: Job Posting

| Item | Value |
|---|---|
| Service | `job-posting-service` |
| Controller | `JobPostingController` |
| Entity/Table | `JobPosting` / `job_postings` |
| Repository | `JobPostingRepository` |
| Request DTOs | `CreateJobPostingRequest`, update/save-draft payloads |
| Response DTO | `JobPostingResponse` |
| CRUD Scope | Create, read all, read by ID, update, save draft; hard delete not exposed |

## Resource: Demand

| Item | Value |
|---|---|
| Service | `job-posting-service` |
| Controller | `DemandController` |
| Entity/Table | `Demand` / `demands` |
| Repository | `DemandRepository` |
| Create Source | Kafka `DEMAND_OPEN_EXTERNAL` event, not REST |
| REST Scope | Read all available, read by DB ID, read by external demand ID |

## Resource: Notification

| Item | Value |
|---|---|
| Service | `job-posting-service` |
| Controller | `NotificationController` |
| Entity/Table | `Notification` / `notifications` |
| Repository | `NotificationRepository` |
| Create Source | System workflow side effects, not REST |
| REST Scope | Read notifications, mark as read, mark all read |

## CRUD Endpoint Tables

## Auth User

| Operation | Method | Endpoint | Description | Success Status |
|---|---|---|---|---|
| Create | `POST` | `/api/auth/register` | Register user account | `200 OK` |
| Read current | `GET` | `/api/auth/me` | Return authenticated user profile | `200 OK` |
| Session create | `POST` | `/api/auth/login` | Authenticate and return JWT | `200 OK` |
| Session refresh | `POST` | `/api/auth/refresh` | Refresh access token | `200 OK` |
| Session revoke | `POST` | `/api/auth/logout` | Revoke refresh token and blacklist access token | `200 OK` |

## Job Posting

| Operation | Method | Endpoint | Description | Success Status |
|---|---|---|---|---|
| Create | `POST` | `/api/job-postings` | Create DRAFT job posting | `201 Created` |
| Read all | `GET` | `/api/job-postings` | List all postings, optional status filter | `200 OK` |
| Read by ID | `GET` | `/api/job-postings/{id}` | Get one posting | `200 OK` |
| Update | `PUT` | `/api/job-postings/{id}` | Update DRAFT or DECLINED posting | `200 OK` |
| Partial update | `POST` | `/api/job-postings/{id}/save-draft` | Save partial fields without status change | `200 OK` |
| Close instead of delete | `POST` | `/api/job-postings/{id}/close` | Close LIVE/READY posting | `200 OK` |

## Demand

| Operation | Method | Endpoint | Description | Success Status |
|---|---|---|---|---|
| Read all | `GET` | `/api/demands` | List available demands | `200 OK` |
| Read by DB ID | `GET` | `/api/demands/{id}` | Get demand by internal ID | `200 OK` |
| Read by external ID | `GET` | `/api/demands/by-demand-id/{demandId}` | Get demand by Kafka/external ID | `200 OK` |

## Notification

| Operation | Method | Endpoint | Description | Success Status |
|---|---|---|---|---|
| Read all | `GET` | `/api/notifications` | List all current-user notifications | `200 OK` |
| Read unread | `GET` | `/api/notifications/unread` | List unread notifications | `200 OK` |
| Count unread | `GET` | `/api/notifications/unread/count` | Count unread notifications | `200 OK` |
| Update all | `PUT` | `/api/notifications/read-all` | Mark all current-user notifications as read | `200 OK` |
| Update one | `PUT` | `/api/notifications/{id}/read` | Mark notification as read | `200 OK` |

## Create Operation Details

## Create Job Posting

Endpoint:

```http
POST /api/job-postings
Authorization: Bearer <recruiter_token>
Content-Type: application/json
```

Purpose:

Create a job posting in `DRAFT` status.

Request body example:

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

Service behavior:

1. Controller receives request DTO.
2. Service creates `JobPosting` entity.
3. Status is initialized as `DRAFT`.
4. Repository saves entity in `job_postings`.
5. Response DTO is returned.

Success response:

```http
201 Created
```

Response contains `JobPostingResponse` with `postingStatus: "DRAFT"`.

Business rules:

- Authenticated user should be a recruiter.
- The posting starts as `DRAFT`.
- It is later submitted for approval through a separate workflow endpoint.

## Register User

Endpoint:

```http
POST /api/auth/register
Content-Type: application/json
```

Request:

```json
{
  "username": "Jane Smith",
  "email": "jane@company.com",
  "password": "password123",
  "role": "RECRUITER"
}
```

Success response:

```json
{
  "id": 5,
  "email": "jane@company.com",
  "message": "User registered successfully"
}
```

## Read by ID Operation Details

## Get Job Posting by ID

Endpoint:

```http
GET /api/job-postings/{id}
Authorization: Bearer <token>
```

Example:

```http
GET /api/job-postings/1
```

Behavior:

- Controller reads `id` from path variable.
- Service looks up posting by database ID.
- If found, returns `JobPostingResponse`.
- If not found, throws `ResourceNotFoundException` and returns `404`.

## Get Demand by ID

Endpoint:

```http
GET /api/demands/{id}
Authorization: Bearer <token>
```

Behavior:

- Reads demand by database primary key.
- Returns demand object if present.
- Returns `404` if missing.

## Get Demand by External Demand ID

Endpoint:

```http
GET /api/demands/by-demand-id/{demandId}
Authorization: Bearer <token>
```

Use case:

- Find a demand using the external ID from the Kafka demand event payload.

## Read All Operation Details

## List Job Postings

Endpoint:

```http
GET /api/job-postings
Authorization: Bearer <token>
```

Optional filter:

```http
GET /api/job-postings?status=PENDING_APPROVAL
```

Response:

```json
[
  {
    "id": 1,
    "title": "Senior Java Developer",
    "postingStatus": "PENDING_APPROVAL"
  }
]
```

Current behavior:

- Returns an array of `JobPostingResponse` objects.
- Supports optional exact status filtering.
- Page/size/sort pagination is a Module 7 TODO unless implemented in code.

## List Demands

Endpoint:

```http
GET /api/demands
Authorization: Bearer <token>
```

Behavior:

- Returns demands available for job posting creation.
- `DemandRepository.findAvailableDemands()` excludes demands already linked to active postings such as `PENDING_APPROVAL`, `READY_TO_PUBLISH`, or `LIVE`.

## List Notifications

Endpoint:

```http
GET /api/notifications
Authorization: Bearer <token>
```

Behavior:

- Returns current user's notifications, newest first.

## Update Operation Details

## Update Job Posting

Endpoint:

```http
PUT /api/job-postings/{id}
Authorization: Bearer <recruiter_token>
Content-Type: application/json
```

Rules:

- Only allowed when `postingStatus` is `DRAFT` or `DECLINED`.
- Same fields as create request.
- Returns updated `JobPostingResponse`.

Possible errors:

| Status | Reason |
|---|---|
| `404` | Posting does not exist |
| `400` | Posting is not in editable state |
| `403` | User lacks recruiter permission |

## Save Draft

Endpoint:

```http
POST /api/job-postings/{id}/save-draft
Authorization: Bearer <recruiter_token>
Content-Type: application/json
```

Rules:

- Accepts any subset of create-job-posting fields.
- Does not change status.
- Allowed for `DRAFT` or `DECLINED` postings.

## Mark Notification Read

Endpoint:

```http
PUT /api/notifications/{id}/read
Authorization: Bearer <token>
```

Behavior:

- Sets `isRead = true` for the notification.

## Delete Operation Details

No documented hard-delete REST endpoints exist for job postings, demands, notifications, or users.

Domain alternative for job postings:

```http
POST /api/job-postings/{id}/close
```

This is a state transition, not a database delete.

Close behavior:

- Allowed for `LIVE` or `READY_TO_PUBLISH` postings.
- Updates `postingStatus` to `CLOSED`.
- Produces Kafka event `JOB_UNPUBLISHED`.
- Broadcasts SSE `JOB_UNPUBLISHED`.
- Sends notification.

## DTO Pattern

| Entity | Request DTO | Response DTO |
|---|---|---|
| `JobPosting` | `CreateJobPostingRequest` / update payload | `JobPostingResponse` |
| `Demand` | Created from Kafka event, no REST create DTO | Demand response shape |
| `Notification` | Created internally, no REST create DTO | Notification response shape |
| `User` | Register/Login requests | Login response / profile response |

## Mapping Strategy

The documentation indicates DTOs are manually mapped in service/controller layers rather than through a confirmed MapStruct setup.

Mapping examples:

- `GenerateJdRequest` -> `JdGenerationRequest`
- `JdGenerationResponse.sections` -> `JdSectionsResponse`
- `JobPosting` -> `JobPostingResponse`

TODO:

- Confirm whether any dedicated mapper classes exist in the repository.
- If mappers exist, document class names and methods.

## Business Rules

| Rule | Resource | Description |
|---|---|---|
| Posting starts in DRAFT | Job Posting | `POST /api/job-postings` creates DRAFT |
| Only DRAFT/DECLINED can be edited | Job Posting | `PUT` and `save-draft` are restricted |
| DRAFT/DECLINED can be submitted | Job Posting | Moves to `PENDING_APPROVAL` |
| Only pending posting can be approved/declined | Job Posting | HM workflow |
| Approved becomes READY_TO_PUBLISH | Job Posting | Approval sets status |
| Ready posting can be published | Job Posting | Moves to `LIVE` |
| Live/ready posting can be closed | Job Posting | Moves to `CLOSED` |
| Demand cannot be manually created by REST | Demand | Created from Kafka `DEMAND_OPEN_EXTERNAL` |
| Notifications cannot be created by REST | Notification | Created by system workflow side effects |

## Tests Required by Module 7

Module 7 expects CRUD endpoints to be covered by integration tests using MockMvc and code coverage not less than 80%.

Current status from provided documentation:

```text
MockMvc CRUD tests: TODO / not confirmed
Coverage report: TODO / not confirmed
Controller tests: TODO / not confirmed
Service tests: TODO / not confirmed
Repository tests: TODO / not confirmed
```

Recommended test classes:

```text
JobPostingControllerMockMvcTest
DemandControllerMockMvcTest
NotificationControllerMockMvcTest
AuthControllerMockMvcTest
JobPostingServiceTest
DemandRepositoryTest
```

## Postman Demo Requests

Recommended collection folders:

```text
01 Auth
02 Job Postings CRUD
03 Job Posting Workflow
04 Demands
05 Notifications
06 AI JD Generation
07 Negative Cases
```

Demo sequence:

1. Login recruiter.
2. Create job posting.
3. Get all job postings.
4. Get posting by ID.
5. Update posting while DRAFT.
6. Submit for approval.
7. Login hiring manager.
8. Approve or decline.
9. Login recruiter.
10. Publish or close.

## TODO for Module 7 Completion

- Add hard delete only if business requires it; otherwise document close as domain delete alternative.
- Add MockMvc integration tests.
- Add code coverage report.
- Add Postman collection export to repo.
- Add pagination to get-all endpoints.
- Add validation and RFC 7807 error responses.
