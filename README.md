# 06 - Search Endpoints, Pagination, Filtering, and Sorting

## Module 7 Requirement Covered

This README covers **Part 6: Search endpoints and pagination** from Module 7 Spring Web.

Module 7 expects:

- Get-all endpoints support pagination with `page` and `size` request parameters.
- Responses contain metadata such as total items, current page, and max/total pages.
- Filtering is added to get-all endpoints.
- At least 3 searchable parameters are supported.
- Ordering is available for at least 2 parameters.
- Exact-value search is supported for at least 1 field, with multiple values if appropriate.
- Less-than/greater-than search is supported for at least 1 numeric or date field.
- Functionality is flexible and extensible.
- Unit and integration tests cover search/pagination.

## Current Search/Pagination Status in Forge

Confirmed from supplied documentation:

| Endpoint | Current Capability | Module 7 Gap |
|---|---|---|
| `GET /api/job-postings` | Lists all postings and supports optional `status` filter | Add `page`, `size`, `sort`, and more filters |
| `GET /api/demands` | Lists available demands | Add pagination/filtering if required |
| `GET /api/notifications` | Lists notifications newest first | Add pagination if large notification sets matter |
| `GET /api/job-postings/public/live` | Lists public READY_TO_PUBLISH/LIVE jobs | Add pagination/filtering for public careers portal if required |

Current confirmed query example:

```http
GET /api/job-postings?status=PENDING_APPROVAL
```

Current response style:

```json
[
  {
    "id": 1,
    "title": "Senior Java Developer",
    "postingStatus": "PENDING_APPROVAL"
  }
]
```

Module 7 target response should be paginated.

## Recommended Primary Endpoint for Module 7

Use job postings as the main search/pagination resource because it has many searchable fields:

```http
GET /api/job-postings/search
```

or extend the existing endpoint:

```http
GET /api/job-postings
```

Recommended final endpoint:

```http
GET /api/job-postings?page=0&size=10&sort=createdAt,desc&status=LIVE&workMode=REMOTE&level=SENIOR&salaryMinGte=100000
```

## Pagination Parameters

| Parameter | Type | Default | Example | Description |
|---|---|---|---|---|
| `page` | integer | `0` | `page=0` | Zero-based page number |
| `size` | integer | `20` | `size=10` | Number of items per page |
| `sort` | string | `createdAt,desc` recommended | `sort=title,asc` | Field and direction |

Recommended controller signature:

```java
@GetMapping
public Page<JobPostingResponse> getJobPostings(
        JobPostingSearchCriteria criteria,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return jobPostingService.search(criteria, pageable);
}
```

Alternative explicit parameters:

```java
@GetMapping
public PageResponse<JobPostingResponse> getJobPostings(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String status) {
    ...
}
```

## Pagination Response Structure

Recommended response if using Spring `Page` directly:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Senior Java Developer",
      "postingStatus": "LIVE"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "numberOfElements": 10,
  "empty": false
}
```

Recommended cleaner custom response:

```json
{
  "items": [
    {
      "id": 1,
      "title": "Senior Java Developer",
      "postingStatus": "LIVE"
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 42,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false,
  "sort": ["createdAt,desc"]
}
```

Either approach can satisfy Module 7 if metadata is present.

## Filtering Requirements and Recommended Fields

The job posting entity has enough fields to meet the requirement of at least 3 searchable parameters.

Recommended searchable parameters:

| Field | Query Parameter | Operator | Multiple Values | Example | Purpose |
|---|---|---|---|---|---|
| `postingStatus` | `status` | exact `==` | Yes | `status=LIVE&status=READY_TO_PUBLISH` | Filter by lifecycle state |
| `workMode` | `workMode` | exact `==` | Yes | `workMode=REMOTE` | Filter remote/hybrid/onsite |
| `level` | `level` | exact `==` | Yes | `level=SENIOR` | Filter seniority |
| `department` | `department` | exact or partial | No | `department=Engineering` | Filter business unit |
| `locationCity` | `city` | exact or partial | No | `city=New York` | Filter location |
| `skills` | `skill` | contains | Yes | `skill=Java&skill=Kafka` | Filter by required skills |
| `salaryMin` | `salaryMinGte` | `>=` | No | `salaryMinGte=100000` | Numeric lower-bound search |
| `salaryMax` | `salaryMaxLte` | `<=` | No | `salaryMaxLte=180000` | Numeric upper-bound search |
| `applicationDeadline` | `deadlineBefore` | `<` | No | `deadlineBefore=2026-09-01` | Date less-than search |
| `applicationDeadline` | `deadlineAfter` | `>` | No | `deadlineAfter=2026-06-01` | Date greater-than search |
| `createdAt` | `createdAfter` | `>` | No | `createdAfter=2026-06-01T00:00:00` | Recent postings |

Minimum Module 7 set:

1. `status` exact match, multiple values.
2. `workMode` exact match.
3. `level` exact match.
4. `salaryMinGte` numeric greater-than.
5. `deadlineBefore` date less-than.

## Sorting Requirements

Module 7 requires ordering for at least 2 parameters.

Recommended sortable fields:

| Sort Field | Example | Description |
|---|---|---|
| `createdAt` | `sort=createdAt,desc` | Newest postings first |
| `title` | `sort=title,asc` | Alphabetical title |
| `applicationDeadline` | `sort=applicationDeadline,asc` | Soonest deadlines first |
| `salaryMin` | `sort=salaryMin,desc` | Highest salary minimum first |
| `updatedAt` | `sort=updatedAt,desc` | Recently changed postings first |

Example requests:

```http
GET /api/job-postings?page=0&size=10&sort=createdAt,desc
```

```http
GET /api/job-postings?page=0&size=10&sort=title,asc
```

```http
GET /api/job-postings?page=0&size=10&sort=applicationDeadline,asc
```

## Search Implementation Options

Module 7 lists multiple possible implementation styles:

- Criteria API.
- JPA Specification API.
- QueryDSL.
- Advanced Search Operations.
- jOOQ dynamic query generation.
- Specification argument resolver.

Recommended for Forge:

```text
Spring Data JPA Specification API
```

Reason:

- The project already uses Spring Data JPA repositories.
- Job posting filters are dynamic and optional.
- Specifications are easy to extend with new fields/operators.
- It maps well to Module 7 requirement for flexible and extensible filtering.

## Recommended Implementation Design

## `JobPostingSearchCriteria`

```java
public record JobPostingSearchCriteria(
        List<JobStatus> status,
        List<String> workMode,
        List<String> level,
        String department,
        String city,
        List<String> skills,
        BigDecimal salaryMinGte,
        BigDecimal salaryMaxLte,
        LocalDate deadlineBefore,
        LocalDate deadlineAfter,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {}
```

## Repository

Change repository to support specifications:

```java
public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {
    ...
}
```

## Specification Builder

```java
public class JobPostingSpecifications {
    public static Specification<JobPosting> hasStatuses(List<JobStatus> statuses) { ... }
    public static Specification<JobPosting> hasWorkModes(List<String> workModes) { ... }
    public static Specification<JobPosting> hasLevels(List<String> levels) { ... }
    public static Specification<JobPosting> salaryMinGreaterThanOrEqual(BigDecimal value) { ... }
    public static Specification<JobPosting> deadlineBefore(LocalDate value) { ... }
}
```

## Service Flow

```text
Controller receives query parameters + Pageable
        |
        v
Parameters mapped to JobPostingSearchCriteria
        |
        v
Service builds Specification<JobPosting>
        |
        v
Repository executes findAll(specification, pageable)
        |
        v
Page<JobPosting> mapped to Page<JobPostingResponse>
        |
        v
Controller returns paginated response
```

## Example Requests

## First Page

```http
GET /api/job-postings?page=0&size=10
```

## Second Page

```http
GET /api/job-postings?page=1&size=10
```

## Sort by Created Date Descending

```http
GET /api/job-postings?page=0&size=10&sort=createdAt,desc
```

## Sort by Title Ascending

```http
GET /api/job-postings?page=0&size=10&sort=title,asc
```

## Filter by Exact Status

```http
GET /api/job-postings?status=LIVE
```

## Filter by Multiple Statuses

```http
GET /api/job-postings?status=LIVE&status=READY_TO_PUBLISH
```

## Filter by Work Mode and Level

```http
GET /api/job-postings?workMode=REMOTE&level=SENIOR
```

## Numeric Greater-Than Filter

```http
GET /api/job-postings?salaryMinGte=100000
```

## Date Less-Than Filter

```http
GET /api/job-postings?deadlineBefore=2026-09-01
```

## Combined Search

```http
GET /api/job-postings?page=0&size=10&sort=createdAt,desc&status=LIVE&workMode=REMOTE&level=SENIOR&salaryMinGte=100000&deadlineBefore=2026-12-31
```

## Public Careers Search Recommendation

Public careers currently uses:

```http
GET /api/job-postings/public/live
```

Recommended future public search:

```http
GET /api/job-postings/public/live?page=0&size=12&city=Bangalore&skill=Java&workMode=HYBRID
```

This would improve candidate browsing and aligns with the domain.

## Validation for Search Parameters

Recommended validations:

| Parameter | Rule | Error |
|---|---|---|
| `page` | `>= 0` | `400` if negative |
| `size` | `1..100` | `400` if too small/large |
| `sort` | Allowed field only | `400` if unknown field |
| `status` | Valid enum | `400` if unsupported status |
| `salaryMinGte` | `>= 0` | `400` if negative |
| `deadlineBefore` | valid ISO date | `400` if invalid date |

Recommended RFC 7807 error:

```json
{
  "type": "https://forge/errors/invalid-search-parameter",
  "title": "Invalid search parameter",
  "status": 400,
  "detail": "Search parameter validation failed",
  "errors": [
    {
      "field": "size",
      "message": "must be between 1 and 100"
    }
  ]
}
```

## Tests Required

Module 7 requires unit and integration coverage.

Recommended unit tests:

| Test Class | Purpose |
|---|---|
| `JobPostingSpecificationsTest` | Verify each filter specification produces expected results |
| `JobPostingSearchServiceTest` | Verify criteria and pageable are passed correctly |
| `JobPostingSearchCriteriaValidationTest` | Verify invalid page/size/status/sort errors |

Recommended MockMvc tests:

| Scenario | Request | Expected |
|---|---|---|
| First page | `GET /api/job-postings?page=0&size=10` | `200`, metadata present |
| Status filter | `GET /api/job-postings?status=LIVE` | All items LIVE |
| Multiple status filter | `status=LIVE&status=READY_TO_PUBLISH` | Items match either status |
| Numeric filter | `salaryMinGte=100000` | All salaries >= 100000 |
| Date filter | `deadlineBefore=2026-09-01` | Deadlines before date |
| Sort title | `sort=title,asc` | Alphabetical order |
| Invalid size | `size=0` | `400` |
| Invalid status | `status=BAD` | `400` |
| Unknown sort field | `sort=password,asc` | `400` |
| No results | Search impossible criteria | `200` with empty page |

Current status from supplied docs:

```text
Search/pagination tests: TODO / not confirmed
```

## Postman Demo Plan

Create folder:

```text
06 Search Pagination Sorting
```

Requests:

1. `GET /api/job-postings?page=0&size=5`
2. `GET /api/job-postings?page=1&size=5`
3. `GET /api/job-postings?status=LIVE`
4. `GET /api/job-postings?status=LIVE&status=READY_TO_PUBLISH`
5. `GET /api/job-postings?workMode=REMOTE&level=SENIOR`
6. `GET /api/job-postings?salaryMinGte=100000`
7. `GET /api/job-postings?deadlineBefore=2026-09-01`
8. `GET /api/job-postings?sort=createdAt,desc`
9. `GET /api/job-postings?sort=title,asc`
10. Invalid: `GET /api/job-postings?page=-1`
11. Invalid: `GET /api/job-postings?size=0`
12. Invalid: `GET /api/job-postings?sort=unknown,asc`

## TODO for Module 7 Completion

- Implement `page` and `size` request parameters.
- Return pagination metadata.
- Add at least 3 searchable parameters.
- Add at least 2 sortable fields.
- Add exact-value filtering with multiple values.
- Add numeric/date greater-than or less-than filtering.
- Implement dynamic query using JPA Specification or approved alternative.
- Add unit and MockMvc integration tests.
- Add Postman search demo requests.
