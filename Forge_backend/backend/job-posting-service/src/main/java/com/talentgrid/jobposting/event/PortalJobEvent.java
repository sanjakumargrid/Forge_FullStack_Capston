package com.talentgrid.jobposting.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PortalJobEvent {
    private String eventId;
    private String eventType;    // JOB_PUBLISHED | JOB_UNPUBLISHED
    private Instant timestamp;
    private String source;
    private String version;
    private String correlationId;
    private PortalJobPayload payload;

    public static PortalJobEvent of(String eventType, String correlationId, PortalJobPayload payload) {
        return PortalJobEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .timestamp(Instant.now())
                .source("job-posting-service")
                .version("1.0")
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
