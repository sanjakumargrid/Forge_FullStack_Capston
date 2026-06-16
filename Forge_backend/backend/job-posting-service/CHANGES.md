# Changes — Demand List Filtering

## What changed

Two files were modified to hide demands from the list once they have progressed
beyond the draft stage in the job posting workflow.

---

### 1. `DemandRepository.java`

Added a new JPQL query method `findAvailableDemands()`.

```java
@Query("""
    SELECT d FROM Demand d
    WHERE d.demandId NOT IN (
        SELECT jp.demandId FROM JobPosting jp
        WHERE jp.demandId IS NOT NULL
        AND jp.postingStatus IN (
            PENDING_APPROVAL,
            READY_TO_PUBLISH,
            LIVE
        )
    )
""")
List<Demand> findAvailableDemands();
```

The query excludes any demand whose `demandId` is already linked to a job posting
in one of the active/committed statuses. No schema change is required — the
`demand_id` column already exists on the `job_postings` table.

---

### 2. `DemandService.java` — `getAll()`

Changed the call from `findAll()` to `findAvailableDemands()`:

```java
// before
return demandRepository.findAll()
        .stream()
        .map(DemandResponse::from)
        .toList();

// after
return demandRepository.findAvailableDemands()
        .stream()
        .map(DemandResponse::from)
        .toList();
```

---

## Visibility rules

| Job Posting Status  | Demand visible in list? | Reason                                      |
|---------------------|-------------------------|---------------------------------------------|
| No posting yet      | Yes                     | Available for recruiter to pick up          |
| `DRAFT`             | Yes                     | Not committed — recruiter still editing     |
| `DECLINED`          | Yes                     | Recruiter can create a new posting and retry |
| `PENDING_APPROVAL`  | **No**                  | Sent to Hiring Manager — no action needed   |
| `READY_TO_PUBLISH`  | **No**                  | Approved — waiting for publish              |
| `LIVE`              | **No**                  | Already published to careers portal         |

---

## Why this approach

- **No extra column** on the `Demand` table — avoids dual-write and sync issues.
- **Self-healing** — if a posting is declined, the demand automatically reappears
  in the list without any extra update call.
- **Single query** — the database does the join; no in-memory filtering needed.
- The individual demand endpoints (`GET /api/demands/{id}` and
  `GET /api/demands/by-demand-id/{demandId}`) are **unaffected** — they still
  return any demand by ID regardless of its posting status, which is useful for
  debugging and admin purposes.
