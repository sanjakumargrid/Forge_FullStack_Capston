package com.talentgrid.jobposting.dto.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JdGenerationRequest(
    String demandId,

    @NotBlank(message = "Role title is required")
    @Size(max = 128)
    String roleTitle,

    @Size(max = 128)
    String department,

    @Size(max = 128)
    String location,

    String workMode,

    String experienceYears,

    @Size(max = 20, message = "Max 20 skills")
    List<String> skillsRequired,

    String seniorityLevel,

    String employmentType,

    @Size(max = 2000, message = "Additional context max 2000 chars")
    String additionalContext
) {}
