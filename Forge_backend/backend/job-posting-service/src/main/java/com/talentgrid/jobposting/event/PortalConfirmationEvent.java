package com.talentgrid.jobposting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalConfirmationEvent {
    private String eventId;
    private String eventType;    // JOB_LIVE | JOB_TAKEN_DOWN | JOB_FAILED
    private Instant timestamp;
    private String source;
    private String correlationId;
    private PortalConfirmationPayload payload;
}
