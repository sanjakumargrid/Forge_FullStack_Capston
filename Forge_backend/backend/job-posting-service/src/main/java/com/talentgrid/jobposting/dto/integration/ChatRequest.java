package com.talentgrid.jobposting.dto.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
    @NotBlank
    String sessionId,

    @NotBlank
    @Size(max = 2000)
    String message,

    @NotBlank
    String teamId,

    @NotBlank
    String featureType
) {}
