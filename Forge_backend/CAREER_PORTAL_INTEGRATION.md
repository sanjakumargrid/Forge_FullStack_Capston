# Career Portal — Kafka Integration Guide

This document describes how the Career Portal integrates with the Job Posting Service via Kafka to receive job listings and send back confirmation events.

---

## Overview

```
job-posting-service ──► portal-job-events ──► Career Portal
                                                    │
Career Portal ──────► portal-confirmations ──► job-posting-service
```

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `portal-job-events` | job-posting-service | Career Portal | Publish / unpublish a job |
| `portal-confirmations` | Career Portal | job-posting-service | Confirm the action succeeded or failed |

---

## Topic: `portal-job-events`

The Career Portal **consumes** from this topic.

### When events are produced

| Trigger | Event Type |
|---|---|
| Hiring Manager approves a job posting | `JOB_PUBLISHED` |
| Recruiter closes a live or ready-to-publish job | `JOB_UNPUBLISHED` |

### Event envelope

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "JOB_PUBLISHED",
  "timestamp": "2026-06-14T10:30:00Z",
  "source": "job-posting-service",
  "version": "1.0",
  "correlationId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "payload": { ... }
}
```

### Payload fields

| Field | Type | Notes |
|---|---|---|
| `jobPostingId` | `Long` | Primary key — use this to correlate confirmations |
| `title` | `String` | Job title |
| `description` | `String` | Full job description |
| `responsibilities` | `String` | Responsibilities section |
| `requirements` | `String` | Requirements section |
| `benefits` | `String` | Benefits section |
| `department` | `String` | e.g. `Engineering` |
| `jobCategory` | `String` | e.g. `Backend` |
| `locationCity` | `String` | |
| `locationState` | `String` | |
| `locationCountry` | `String` | |
| `workMode` | `String` | `REMOTE` \| `HYBRID` \| `ONSITE` |
| `employmentType` | `String` | `FULL_TIME` \| `PART_TIME` \| `CONTRACT` \| `INTERNSHIP` |
| `level` | `String` | `JUNIOR` \| `MID` \| `SENIOR` \| `LEAD` \| `PRINCIPAL` |
| `experienceYears` | `Double` | |
| `skills` | `String[]` | e.g. `["Java", "Spring Boot"]` |
| `salaryMin` | `BigDecimal` | Only show if `showSalary = true` |
| `salaryMax` | `BigDecimal` | Only show if `showSalary = true` |
| `currency` | `String` | ISO 4217, e.g. `USD` |
| `showSalary` | `Boolean` | If `false`, hide salary on the portal |
| `applicationDeadline` | `LocalDate` | `YYYY-MM-DD` — filter out expired jobs |
| `requiredCount` | `Integer` | Number of openings |
| `approvedAt` | `Instant` | ISO 8601 UTC |
| `approvedBy` | `Long` | User ID of the approving Hiring Manager |

### Full example — JOB_PUBLISHED

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "JOB_PUBLISHED",
  "timestamp": "2026-06-14T10:30:00Z",
  "source": "job-posting-service",
  "version": "1.0",
  "correlationId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "payload": {
    "jobPostingId": 42,
    "title": "Senior Backend Engineer",
    "description": "Design and operate the microservices powering Forge.",
    "responsibilities": "Own service reliability, mentor engineers.",
    "requirements": "6+ years Java/Spring Boot.",
    "benefits": "Health, equity, flexible PTO.",
    "department": "Engineering",
    "jobCategory": "Backend",
    "locationCity": "Remote",
    "locationState": "",
    "locationCountry": "USA",
    "workMode": "REMOTE",
    "employmentType": "FULL_TIME",
    "level": "SENIOR",
    "experienceYears": 6.0,
    "skills": ["Java", "Spring Boot", "Kafka"],
    "salaryMin": 160000,
    "salaryMax": 210000,
    "currency": "USD",
    "showSalary": true,
    "applicationDeadline": "2026-09-01",
    "requiredCount": 2,
    "approvedAt": "2026-06-14T10:30:00Z",
    "approvedBy": 2
  }
}
```

### Full example — JOB_UNPUBLISHED

```json
{
  "eventId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "eventType": "JOB_UNPUBLISHED",
  "timestamp": "2026-07-01T09:00:00Z",
  "source": "job-posting-service",
  "version": "1.0",
  "correlationId": "abc12345-dead-beef-cafe-000000000001",
  "payload": {
    "jobPostingId": 42,
    "title": "Senior Backend Engineer",
    ...
  }
}
```

---

## Topic: `portal-confirmations`

The Career Portal **produces** to this topic after acting on a `portal-job-events` event.

### Event types to produce

| Action taken | Event type |
|---|---|
| Job successfully published to portal | `JOB_LIVE` |
| Job successfully removed from portal | `JOB_TAKEN_DOWN` |
| Publish or takedown failed | `JOB_FAILED` |

### Event envelope

```json
{
  "eventId": "uuid",
  "eventType": "JOB_LIVE",
  "timestamp": "2026-06-14T10:31:05Z",
  "source": "career-portal",
  "correlationId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "payload": {
    "jobPostingId": 42,
    "portalJobId": "portal-job-abc-123",
    "portalUrl": "https://careers.example.com/jobs/42",
    "reason": null
  }
}
```

### Payload fields

| Field | Type | Required | Notes |
|---|---|---|---|
| `jobPostingId` | `Long` | Always | Must match the `jobPostingId` from the inbound event |
| `portalJobId` | `String` | On `JOB_LIVE` | Career portal's own internal reference |
| `portalUrl` | `String` | On `JOB_LIVE` | Public URL of the live listing |
| `reason` | `String` | On `JOB_FAILED` | Human-readable error description |

> **Important:** always include `jobPostingId` — this is how `job-posting-service` routes the confirmation to the correct posting.

### Effect on job-posting-service

| Event type | Portal channel state | Posting status |
|---|---|---|
| `JOB_LIVE` | `live` | `LIVE` |
| `JOB_TAKEN_DOWN` | `idle` | unchanged (`CLOSED`) |
| `JOB_FAILED` | `failed` | unchanged |

The recruiter receives a push notification for all three outcomes.

---

## Kafka connection details

All values are configurable via environment variables on the `job-posting-service` container.

| Setting | Default | Override env var |
|---|---|---|
| Bootstrap servers | `kafka:29092` (internal) | `KAFKA_BOOTSTRAP_SERVERS` |
| Inbound topic | `portal-job-events` | `PORTAL_JOB_EVENTS_TOPIC` |
| Outbound topic | `portal-confirmations` | `PORTAL_CONFIRMATIONS_TOPIC` |
| Consumer group (job-posting-service) | `job-posting-group` | — |

For local development the Kafka broker is accessible on the host at `localhost:9093` (see `docker-compose.yml`).

---

## Consuming `portal-job-events` — quick-start (Spring Boot)

```java
@KafkaListener(topics = "portal-job-events", groupId = "career-portal-group")
public void handle(PortalJobEvent event, Acknowledgment ack) {
    if ("JOB_PUBLISHED".equals(event.getEventType())) {
        portalService.publish(event.getPayload());
    } else if ("JOB_UNPUBLISHED".equals(event.getEventType())) {
        portalService.unpublish(event.getPayload().getJobPostingId());
    }
    ack.acknowledge();
}
```

After publishing / removing the job, send the confirmation:

```java
// On success
PortalConfirmationEvent confirmation = new PortalConfirmationEvent();
confirmation.setEventId(UUID.randomUUID().toString());
confirmation.setEventType("JOB_LIVE");            // or JOB_TAKEN_DOWN / JOB_FAILED
confirmation.setTimestamp(Instant.now());
confirmation.setSource("career-portal");
confirmation.setCorrelationId(event.getCorrelationId());

PortalConfirmationPayload payload = new PortalConfirmationPayload();
payload.setJobPostingId(event.getPayload().getJobPostingId());
payload.setPortalJobId("portal-internal-id");
payload.setPortalUrl("https://careers.example.com/jobs/" + event.getPayload().getJobPostingId());
confirmation.setPayload(payload);

kafkaTemplate.send("portal-confirmations", String.valueOf(payload.getJobPostingId()), confirmation);
```

### Consumer group

Use a **different** consumer group from `job-posting-service` (e.g. `career-portal-group`) so both services can independently read from `portal-job-events` in the future.

---

## Consuming `portal-job-events` — quick-start (Node.js / KafkaJS)

```js
const { Kafka } = require('kafkajs');

const kafka = new Kafka({ brokers: ['localhost:9093'] });
const consumer = kafka.consumer({ groupId: 'career-portal-group' });
const producer = kafka.producer();

await consumer.connect();
await producer.connect();
await consumer.subscribe({ topic: 'portal-job-events', fromBeginning: false });

await consumer.run({
  eachMessage: async ({ message }) => {
    const event = JSON.parse(message.value.toString());

    if (event.eventType === 'JOB_PUBLISHED') {
      // Save to your DB / CMS / search index
      const portalJobId = await portalService.publish(event.payload);

      await producer.send({
        topic: 'portal-confirmations',
        messages: [{
          key: String(event.payload.jobPostingId),
          value: JSON.stringify({
            eventId: crypto.randomUUID(),
            eventType: 'JOB_LIVE',
            timestamp: new Date().toISOString(),
            source: 'career-portal',
            correlationId: event.correlationId,
            payload: {
              jobPostingId: event.payload.jobPostingId,
              portalJobId,
              portalUrl: `https://careers.example.com/jobs/${event.payload.jobPostingId}`,
            },
          }),
        }],
      });

    } else if (event.eventType === 'JOB_UNPUBLISHED') {
      await portalService.remove(event.payload.jobPostingId);

      await producer.send({
        topic: 'portal-confirmations',
        messages: [{
          key: String(event.payload.jobPostingId),
          value: JSON.stringify({
            eventId: crypto.randomUUID(),
            eventType: 'JOB_TAKEN_DOWN',
            timestamp: new Date().toISOString(),
            source: 'career-portal',
            correlationId: event.correlationId,
            payload: { jobPostingId: event.payload.jobPostingId },
          }),
        }],
      });
    }
  },
});
```

---

## Error handling recommendations

- Always acknowledge the Kafka message even on failure, then publish a `JOB_FAILED` confirmation so the recruiter is notified and can retry.
- Use a Dead Letter Queue (DLQ) topic for messages the portal cannot process after multiple retries.
- Include `correlationId` in all confirmation events — it links the confirmation back to the original outbound event for tracing.
- The `showSalary` field must be respected: if `false`, do **not** render salary on the public listing.
- Filter out jobs where `applicationDeadline` is in the past before displaying to candidates.
