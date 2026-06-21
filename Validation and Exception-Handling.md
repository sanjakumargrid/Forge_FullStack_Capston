# 05 - Validation and Exception Handling

## Module 7 Requirement Covered

This README covers **Part 5: Validations and Exception handling** from Module 7 Spring Web.

Module 7 expects:

- CRUD endpoints have validation rules.
- Error responses for `400+` statuses conform to RFC 7807.
- Multiple validation errors are returned together and are distinguishable by field.
- At least one custom validation annotation is implemented.
- Negative cases are covered by unit and integration tests.
- Negative cases are prepared in Postman for demo.

## Validation Overview in Forge

Forge uses several kinds of validation and safety checks:

1. **Bean Validation / Jakarta Validation** on DTOs where `@Valid` is used.
2. **Business-state validation** in services, for example only editable statuses can be updated.
3. **Security/RBAC validation** in Spring Security configuration and filters.
4. **AI guardrail validation** through `PromptGuardService` and `forge-ai-guardrail`.
5. **Integration response validation** around Team 4 AI service responses.

## Bean Validation Status

Confirmed/mentioned validation examples:

| DTO | Validation Status |
|---|---|
| `ChatRequest` | `@Valid` is used on controller; fields have `@NotBlank`; message capped at 2000 chars |
| `GenerateJdRequest` | Has Bean Validation annotations such as `@NotBlank` / `@Size` according to project notes, but endpoint lacks `@Valid`, so they are not enforced yet |
| `CreateJobPostingRequest` | Validation rules should be confirmed in code; Module 7 requires full validation |
| Auth login/register request | Validation should be confirmed in code |

Important known gap:

```text
POST /api/job-postings/generate-jd currently does not enforce Bean Validation unless @Valid is added to the controller parameter.
```

## Request DTO Validation Tables

## `ChatRequest`

Used by:

```http
POST /api/recruiter-ai/chat
```

| Field | Type | Validation | Error Meaning |
|---|---|---|---|
| `sessionId` | string | `@NotBlank` | Chat session ID is required |
| `message` | string | `@NotBlank`, size cap around 2000 | Candidate message is required and cannot exceed maximum length |
| `teamId` | string | `@NotBlank` | Team identifier is required |
| `featureType` | string | `@NotBlank` | Feature type is required |

Invalid example:

```json
{
  "sessionId": "",
  "message": "",
  "teamId": "BACKEND",
  "featureType": "CAREERS_CHATBOT"
}
```

Expected behavior:

- Spring MVC validation should reject before service logic.
- Response should be `400 Bad Request`.
- Module 7 target response format should be RFC 7807 with field-level errors.

## `GenerateJdRequest`

Used by:

```http
POST /api/job-postings/generate-jd
```

| Field | Type | Intended Validation | Current Note |
|---|---|---|---|
| `roleTitle` | string | `@NotBlank` | Mentioned but not enforced without `@Valid` |
| `department` | string | Optional / size if annotated | Confirm in code |
| `location` | string | Optional / size if annotated | Confirm in code |
| `workMode` | string | Optional enum-like value | Confirm in code |
| `experienceYears` | string | Optional / numeric format could be added | Confirm in code |
| `skillsRequired` | array<string> | Optional, list item size could be added | Confirm in code |
| `seniorityLevel` | string | Optional enum-like value | Confirm in code |
| `employmentType` | string | Optional enum-like value | Confirm in code |
| `additionalContext` | string | Size plus guardrail validation | Guardrail currently validates this field |

Current guardrail behavior:

- `additionalContext` is passed to `PromptGuardService.validateAndSanitize(...)`.
- If blocked, `IllegalArgumentException` is thrown before Team 4 AI service is called.
- Frontend receives `400` and displays a user-friendly blocked-input message.

Recommended fix:

```java
@PostMapping("/generate-jd")
public ResponseEntity<?> generateJd(@Valid @RequestBody GenerateJdRequest req) {
    ...
}
```

## `CreateJobPostingRequest`

Used by:

```http
POST /api/job-postings
PUT /api/job-postings/{id}
POST /api/job-postings/{id}/save-draft
```

Recommended Module 7 validation rules based on actual request fields:

| Field | Type | Suggested Validation | Reason |
|---|---|---|---|
| `demandId` | number/string | required for demand-linked postings if business requires | Prevent orphaned posting if demand is required |
| `title` | string | `@NotBlank`, `@Size` | Posting must have title |
| `description` | string | `@NotBlank`, `@Size` | Posting must have description |
| `responsibilities` | string | `@Size` / maybe required | JD quality |
| `requirements` | string | `@Size` / maybe required | JD quality |
| `benefits` | string | `@Size` | JD quality |
| `employmentType` | string | `@NotBlank` / enum | Required job attribute |
| `level` | string | enum-like validation | Seniority consistency |
| `experienceYears` | number | `@Min(0)` | Prevent negative experience |
| `workMode` | string | enum-like validation | REMOTE/HYBRID/ONSITE |
| `locationCity` | string | optional / required depending work mode | Location consistency |
| `locationCountry` | string | optional / required depending work mode | Location consistency |
| `skills` | array<string> | not empty if required | Job matching |
| `salaryMin` | number | `@PositiveOrZero` | Salary cannot be negative |
| `salaryMax` | number | `@PositiveOrZero`, custom `>= salaryMin` | Salary range integrity |
| `currency` | string | `@Pattern` or enum | Valid currency code |
| `requiredCount` | number | `@Min(1)` | At least one opening |
| `applicationDeadline` | date | future/present | Prevent expired posting at creation |

TODO:

- Confirm exact annotations from source code.
- Add missing validations for Module 7 acceptance.

## Custom Validation

Module 7 requires at least one custom validation annotation.

Current status from supplied documentation:

```text
Custom validation annotation: TODO / not confirmed
```

Recommended custom validator for Forge:

```text
@ValidSalaryRange
```

Purpose:

- Ensures `salaryMax >= salaryMin` when both fields are provided.

Example valid request:

```json
{
  "salaryMin": 120000,
  "salaryMax": 160000
}
```

Example invalid request:

```json
{
  "salaryMin": 180000,
  "salaryMax": 120000
}
```

Recommended files:

```text
validation/ValidSalaryRange.java
validation/ValidSalaryRangeValidator.java
``` 

Alternative custom validators:

- `@ValidPostingStatusTransition`
- `@ValidWorkModeLocation`
- `@ValidCurrencyCode`
- `@FutureOrPresentDeadline`

## Guardrail Validation

Forge uses `forge-ai-guardrail` through `PromptGuardService`.

### Input Guardrail

| Feature | Input Checked | Context Key |
|---|---|---|
| JD generation | `additionalContext` | `JD_GENERATION` |
| Recruiter chatbot | `message` | `RECRUITER_CHATBOT` |

### Output Guardrail

| Feature | Output Checked | Context Key |
|---|---|---|
| JD generation | `summary`, `responsibilities`, `requirements`, `benefits` | `JD_GENERATION_OUTPUT` |
| Recruiter chatbot | Bot reply message | `RECRUITER_CHATBOT_OUTPUT` |

### Behavior

If blocked:

```java
throw new IllegalArgumentException("Prompt was blocked by security guardrail");
```

or:

```java
throw new IllegalArgumentException("AI response was blocked by security guardrail");
```

HTTP response:

- `400 Bad Request` through global exception handling.
- Frontend shows a message asking the user to revise input.

## Business Validation

Forge enforces domain state rules.

| Rule | Error Type |
|---|---|
| Cannot update posting unless status is `DRAFT` or `DECLINED` | Invalid state / `400` |
| Cannot submit unless status is `DRAFT` or `DECLINED` | Invalid state / `400` |
| Cannot approve unless status is `PENDING_APPROVAL` | Invalid state / `400` |
| Cannot decline unless status is `PENDING_APPROVAL` | Invalid state / `400` |
| Cannot publish unless status is `READY_TO_PUBLISH` | Invalid state / `400` |
| Cannot close unless status is `LIVE` or `READY_TO_PUBLISH` | Invalid state / `400` |
| Missing posting/demand/notification | Resource not found / `404` |

## Exception Classes

Confirmed/documented custom exceptions:

| Exception | Package Area | When Thrown | HTTP Status |
|---|---|---|---|
| `ResourceNotFoundException` | `job-posting-service.exception` | Entity ID does not exist | `404` |
| `InvalidStateException` | `job-posting-service.exception` | Workflow action invalid for current status | `400` |
| `IllegalArgumentException` | Java standard | Guardrail block or bad input | `400` |
| `RestClientException` | Spring | Team 4 AI unavailable, timeout, 4xx/5xx | `502` for AI endpoints |

## Global Exception Handler

Documented handler:

```text
GlobalExceptionHandler
```

Responsibilities:

- Convert `IllegalArgumentException` to `400`.
- Convert resource-not-found exceptions to `404`.
- Convert AI service failures to `502`.
- Convert validation exceptions to `400` where implemented.

Current response shape is documented as custom JSON maps for some paths, for example:

```json
{
  "error": "AI service unavailable",
  "message": "Team4 AI service did not respond. Please try again later."
}
```

## RFC 7807 / ProblemDetail Status

Module 7 expects RFC 7807 for all `400+` error responses.

Current status from supplied docs:

```text
ProblemDetail / RFC 7807: TODO / not confirmed as implemented
```

Recommended standard error response:

```json
{
  "type": "https://forge/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/job-postings",
  "errors": [
    {
      "field": "title",
      "message": "must not be blank"
    },
    {
      "field": "salaryMax",
      "message": "must be greater than or equal to salaryMin"
    }
  ]
}
```

Recommended Spring implementation:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Validation failed");
    problem.setDetail("Request validation failed");
    problem.setProperty("errors", fieldErrors);
    return problem;
}
```

## Validation Error Examples

## Blank Chat Message

Request:

```http
POST /api/recruiter-ai/chat
Content-Type: application/json
```

```json
{
  "sessionId": "s1",
  "message": "",
  "teamId": "BACKEND",
  "featureType": "CAREERS_CHATBOT"
}
```

Expected status:

```http
400 Bad Request
```

Expected Module 7 response style:

```json
{
  "type": "https://forge/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": [
    {
      "field": "message",
      "message": "must not be blank"
    }
  ]
}
```

## Guardrail Block

Request:

```json
{
  "roleTitle": "Java Developer",
  "skillsRequired": ["Java", "Spring Boot"],
  "additionalContext": "Only hire men for this role."
}
```

Expected behavior:

- Backend blocks before Team 4 AI service call.
- Returns `400 Bad Request`.
- Frontend shows: `Your input could not be used to generate a job description. Please revise the text and try again.`

## Invalid Workflow State

Example:

```http
POST /api/job-postings/1/approve
Authorization: Bearer <hm_token>
```

If posting is still `DRAFT`:

```http
400 Bad Request
```

Reason:

```text
Only PENDING_APPROVAL postings can be approved.
```

## HTTP Error Status Table

| Status | Meaning in Forge | Example |
|---|---|---|
| `400` | Validation, guardrail block, invalid workflow state | Blank chat message, prompt injection, approve draft |
| `401` | Authentication missing/invalid | No Bearer token on protected endpoint |
| `403` | Authenticated but not allowed | Recruiter tries HM-only approve |
| `404` | Resource not found | Missing job posting ID |
| `502` | External AI service failure | Team 4 service timeout |
| `500` | Unhandled server error | Unexpected bug |

## Negative Tests Required

Recommended tests:

| Test Class | Scenario | Expected |
|---|---|---|
| `RecruiterAiControllerTest` | Blank message | `400` with field error |
| `JobPostingControllerValidationTest` | Missing title | `400` with field error |
| `JobPostingControllerValidationTest` | Negative salary | `400` with field error |
| `JobPostingControllerValidationTest` | salaryMax below salaryMin | `400` with custom validation error |
| `JobPostingControllerWorkflowTest` | Approve DRAFT posting | `400` invalid state |
| `JobPostingControllerWorkflowTest` | Get missing posting | `404` |
| `JobPostingControllerAiTest` | Guardrail blocked context | `400` and Team 4 not called |
| `JobPostingControllerAiTest` | Team 4 timeout | `502` |

Current status from supplied docs:

```text
Negative tests: TODO / not confirmed
MockMvc tests: TODO / not confirmed
```

## Postman Negative Demo Cases

Add requests to a `Negative Cases` Postman folder:

1. Login with wrong password -> `401`.
2. Create posting with blank title -> `400`.
3. Create posting with negative salary -> `400`.
4. Approve a DRAFT posting -> `400`.
5. Get a missing posting ID -> `404`.
6. Generate JD with guardrail-blocked context -> `400`.
7. Stop/disable AI relay and generate JD -> `502`.

## TODO for Module 7 Completion

- Add/confirm Bean Validation annotations on all CRUD request DTOs.
- Add `@Valid` to all controller request body parameters that require validation.
- Implement at least one custom validation annotation.
- Standardize all `400+` responses with RFC 7807 `ProblemDetail`.
- Include multiple field errors in one response.
- Add negative MockMvc and unit tests.
- Export Postman negative-case collection.
