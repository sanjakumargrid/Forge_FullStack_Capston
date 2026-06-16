package com.talentgrid.jobposting.dto.integration;

import lombok.Builder;

@Builder
public record JdGenerationResponse(
    String demandId,
    String roleTitle,
    JdSections sections,
    String rawText,
    long latencyMs,
    String modelUsed,
    Integer promptTokens,
    Integer completionTokens,
    boolean piiStripped
) {
    public record JdSections(
        String summary,
        String responsibilities,
        String requirements,
        String benefits,
        int sectionCount
    ) {}
}
