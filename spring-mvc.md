# 02 - Spring MVC Implementation

## Module 7 Requirement Covered

This README covers **Part 2: Spring MVC** from Module 7 Spring Web. It documents how Forge uses Spring MVC controllers, request mappings, DTO request bodies, path variables, query parameters, validation hooks, service integration, and response handling.

Module 7 expects familiarity with:

- Spring Boot web applications.
- Spring MVC REST controllers.
- Processing query parameters and path variables.
- Servlet and DispatcherServlet request flow.
- Lombok usage.
- Request/response DTOs.

## Spring MVC Overview in Forge

Forge backend is implemented as Spring Boot microservices. HTTP requests enter through Nginx and are routed to Spring MVC controllers.

```text
Client / Angular / Postman
        |
        v
Nginx Gateway :8084
        |
        v
Spring Boot Application
        |
        v
DispatcherServlet
        |
        v
@RestController method
        |
        v
Service Layer
        |
        v
Repository Layer / Integration Client / Kafka / SSE
        |
        v
Response DTO / Error Response
```

## Request Lifecycle

1. A client sends an HTTP request to `http://localhost:8084/api/...`.
2. Nginx routes the request to either `user-auth-service` or `job-posting-service`.
3. Spring Boot receives the request through the embedded servlet container.
4. Spring MVC `DispatcherServlet` resolves the matching controller and method.
5. Controller method arguments are populated from:
   - `@PathVariable`
   - `@RequestParam`
   - `@RequestBody`
   - authenticated user/security context
6. Bean Validation runs where `@Valid` is present.
7. The controller calls a service or integration client.
8. The service applies business rules and repository operations.
9. Controller returns a DTO or `ResponseEntity`.
10. If an exception occurs, `GlobalExceptionHandler` maps it to an HTTP error.

## Controllers

## `AuthController`

Package: `user-auth-service` controller package.

Base path: `/api/auth`

Responsibility:

- User registration.
- Login.
- Access token refresh.
- Logout.
- Current user profile.

Related services:

- User service / authentication manager.
- JWT service.
- Refresh token service.
- Redis token blacklist support.

Endpoints:

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate and return JWT |
| `POST` | `/api/auth/refresh` | Refresh access token using HttpOnly cookie |
| `POST` | `/api/auth/logout` | Blacklist JWT and revoke refresh token |
| `GET` | `/api/auth/me` | Return current authenticated user |
| `GET` | `/api/auth/oauth-success` | OAuth2 callback success endpoint |
| `GET` | `/api/auth/actuator/health` | Auth service health check through gateway |

## `TestController`

Base path: `/api/test`

Responsibility:

- Diagnostic endpoints to verify JWT parsing and RBAC.

Endpoints:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/test/secure` | Shows logged-in user and authorities |
| `GET` | `/api/test/admin` | Requires admin authority and confirms RBAC |

## `JobPostingController`

Package: `job-posting-service` controller package.

Base path: `/api/job-postings`

Responsibility:

- Job posting CRUD.
- Workflow transitions.
- Approval audit lookup.
- Dashboard statistics.
- AI JD generation.
- Public job listing and SSE event stream.

Related services/classes:

- `JobPostingService`
- `PromptGuardService`
- `AiIntegrationClient`
- `SseEmitterService`
- `PortalEventProducer`
- `NotificationService`

Endpoints:

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/job-postings` | Create a DRAFT posting |
| `GET` | `/api/job-postings` | List all postings, optional status filter |
| `GET` | `/api/job-postings/{id}` | Get one posting by ID |
| `PUT` | `/api/job-postings/{id}` | Update posting while DRAFT or DECLINED |
| `POST` | `/api/job-postings/{id}/save-draft` | Save partial draft changes |
| `POST` | `/api/job-postings/{id}/submit-for-approval` | Submit to Hiring Manager |
| `POST` | `/api/job-postings/{id}/approve` | Hiring Manager approval |
| `POST` | `/api/job-postings/{id}/decline` | Hiring Manager decline with reason |
| `POST` | `/api/job-postings/{id}/publish` | Recruiter publishes READY_TO_PUBLISH posting |
| `POST` | `/api/job-postings/{id}/close` | Close posting |
| `POST` | `/api/job-postings/{id}/channels/{channel}/publish` | Mark distribution channel live |
| `GET` | `/api/job-postings/{id}/approvals` | Approval audit trail |
| `GET` | `/api/job-postings/stats` | Counts grouped by status |
| `POST` | `/api/job-postings/generate-jd` | Generate JD sections with Team 4 AI service |
| `GET` | `/api/job-postings/public/live` | Public live/ready jobs |
| `GET` | `/api/job-postings/public/events` | SSE job publish/unpublish stream |

## `DemandController`

Base path: `/api/demands`

Responsibility:

- Expose demands consumed from Kafka.
- Provide available demands to the recruiter flow.

Related service:

- `DemandService`

Endpoints:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/demands` | List demands available for job posting creation |
| `GET` | `/api/demands/{id}` | Get demand by database primary key |
| `GET` | `/api/demands/by-demand-id/{demandId}` | Get demand by external demand ID |

## `NotificationController`

Base path: `/api/notifications`

Responsibility:

- Return user notifications.
- Mark notifications as read.

Related service:

- `NotificationService`

Endpoints:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/notifications` | List all notifications for current user |
| `GET` | `/api/notifications/unread` | List unread notifications |
| `GET` | `/api/notifications/unread/count` | Count unread notifications |
| `PUT` | `/api/notifications/read-all` | Mark all notifications read |
| `PUT` | `/api/notifications/{id}/read` | Mark one notification read |

## `RecruiterAiController`

Base path: `/api/recruiter-ai`

Responsibility:

- Candidate-facing AI chatbot on the public careers portal.
- Runs input guardrail before calling Team 4 chatbot.
- Runs output guardrail before returning the chatbot response.

Related classes:

- `PromptGuardService`
- `AiIntegrationClient`

Endpoints:

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/recruiter-ai/chat` | Send a candidate chat message and return AI response |

## Spring MVC Annotations Used

| Annotation | Purpose in Forge |
|---|---|
| `@RestController` | Declares REST controller classes |
| `@RequestMapping` | Sets base path for controller resources |
| `@GetMapping` | Handles read endpoints |
| `@PostMapping` | Handles create endpoints and workflow commands |
| `@PutMapping` | Handles full update/read-state update endpoints |
| `@PathVariable` | Reads `{id}`, `{channel}`, `{demandId}` from URL |
| `@RequestParam` | Reads query parameters such as `status` |
| `@RequestBody` | Maps JSON request body to DTO |
| `@Valid` | Triggers Bean Validation where present, e.g. chatbot request |
| `@ControllerAdvice` / `@RestControllerAdvice` | Central exception handling, if implemented as documented by `GlobalExceptionHandler` |
| `@ExceptionHandler` | Maps exceptions to HTTP responses |

## DTO Usage

Module 7 requires that entity classes are not returned/used directly at controller level. Forge follows a DTO pattern for key endpoints.

Important DTO groups:

| DTO | Direction | Used By | Purpose |
|---|---|---|---|
| `CreateJobPostingRequest` | Request | `POST /api/job-postings` | Create posting payload |
| `GenerateJdRequest` | Request | `POST /api/job-postings/generate-jd` | AI JD request from frontend |
| `JdGenerationRequest` | Integration request | Team 4 AI service | Sanitized backend-to-AI payload |
| `JdGenerationResponse` | Integration response | Team 4 AI service | Raw AI response contract |
| `JdSectionsResponse` | Response | Frontend | Clean JD sections returned to UI |
| `ChatRequest` | Request | `POST /api/recruiter-ai/chat` | Chatbot request |
| `ChatResponse` | Response | Chatbot endpoint | Chatbot reply metadata |
| `JobPostingResponse` | Response | Job posting endpoints | Job posting DTO returned to clients |
| `Demand` response shape | Response | Demand endpoints | Demand fields from Kafka event |
| `Notification` response shape | Response | Notification endpoints | Notification data for header/list |
| `LoginResponse` | Response | Auth login/refresh | JWT and user profile |

## Service Integration

| Controller Method | Service/Client Method | Purpose |
|---|---|---|
| Create job posting | `JobPostingService.create(...)` | Persist new DRAFT posting |
| List postings | `JobPostingService.findAll(...)` | Return all postings or by status |
| Update posting | `JobPostingService.update(...)` | Update only DRAFT/DECLINED posting |
| Submit for approval | `JobPostingService.submitForApproval(...)` | Transition and notify HM |
| Approve posting | `JobPostingService.approve(...)` | Transition, audit, notify, publish event |
| Decline posting | `JobPostingService.decline(...)` | Transition, store reason, notify recruiter |
| Publish posting | `JobPostingService.publish(...)` | Move READY_TO_PUBLISH to LIVE |
| Close posting | `JobPostingService.close(...)` | Close and unpublish |
| Generate JD | `PromptGuardService`, `AiIntegrationClient.generateJobDescription(...)` | Guardrail + Team 4 AI integration |
| Chatbot | `PromptGuardService`, `AiIntegrationClient.chat(...)` | Guardrail + Team 4 chatbot |
| List demands | `DemandService` | Read available demands |
| Notifications | `NotificationService` | Read/update notification records |

## Example Request Flow: AI JD Generation

Endpoint:

```http
POST /api/job-postings/generate-jd
Authorization: Bearer <token>
Content-Type: application/json
```

Request:

```json
{
  "roleTitle": "Senior Backend Engineer",
  "department": "Engineering",
  "location": "New York",
  "workMode": "HYBRID",
  "experienceYears": "5+",
  "skillsRequired": ["Java", "Spring Boot", "Kafka"],
  "seniorityLevel": "SENIOR",
  "employmentType": "FULL_TIME",
  "additionalContext": "Emphasize distributed systems and growth opportunities."
}
```

Flow:

```text
Frontend button click
  -> JobPostingApiService.generateJd()
  -> POST /api/job-postings/generate-jd
  -> Nginx routes to job-posting-service
  -> JobPostingController.generateJd()
  -> PromptGuardService.validateAndSanitize(additionalContext)
  -> AiIntegrationClient.generateJobDescription()
  -> Team 4 AI endpoint
  -> PromptGuardService.validateOutput(each generated section)
  -> JdSectionsResponse returned
  -> Angular patches form fields
```

Response:

```json
{
  "summary": "...",
  "responsibilities": "...",
  "requirements": "...",
  "benefits": "...",
  "sectionCount": 4
}
```

## Example Request Flow: Get Job Posting by ID

Endpoint:

```http
GET /api/job-postings/1
Authorization: Bearer <token>
```

Flow:

```text
Controller reads id from @PathVariable
  -> service looks up repository by id
  -> if found, entity is mapped to JobPostingResponse
  -> 200 OK returned
  -> if not found, ResourceNotFoundException -> 404 response
```

## HTTP Status Codes Returned by Controllers

| Status | Example Endpoint |
|---|---|
| `200 OK` | `GET /api/job-postings`, `POST /api/job-postings/{id}/approve` |
| `201 Created` | `POST /api/job-postings` |
| `400 Bad Request` | Guardrail block, invalid workflow state, validation error |
| `401 Unauthorized` | Protected endpoint without JWT |
| `403 Forbidden` | Wrong role for protected action |
| `404 Not Found` | Missing job posting or demand |
| `502 Bad Gateway` | Team 4 AI service unavailable |

## Module 7 TODOs for Spring MVC

- Confirm all controller request bodies that have validation annotations use `@Valid`.
- Add `@Valid` to `GenerateJdRequest` endpoint if Bean Validation rules should be enforced.
- Add MockMvc integration tests for every controller endpoint.
- Add pagination-friendly controller signatures for get-all endpoints.
- Add standardized RFC 7807 exception handling.
