package com.talentgrid.jobposting.dto.integration;

import lombok.Builder;
import java.time.Instant;

@Builder
public record ChatResponse(
    String sessionId,
    String message,
    boolean aiAssisted,
    boolean fallbackTriggered,
    boolean blocked,
    int chunksUsed,
    long latencyMs,
    Instant timestamp
) {}
