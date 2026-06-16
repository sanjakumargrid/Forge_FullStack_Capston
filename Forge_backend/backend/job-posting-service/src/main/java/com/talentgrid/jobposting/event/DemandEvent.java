package com.talentgrid.jobposting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private String version;
    private String correlationId;
    private DemandPayload payload;
}
