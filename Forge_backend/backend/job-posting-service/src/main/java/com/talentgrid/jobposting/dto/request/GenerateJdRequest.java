package com.talentgrid.jobposting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GenerateJdRequest {

    private String demandId;

    @NotBlank(message = "Role title is required")
    @Size(max = 128)
    private String roleTitle;

    @Size(max = 128)
    private String department;

    @Size(max = 128)
    private String location;

    /** HYBRID, REMOTE, ON_SITE */
    private String workMode;

    /** e.g. "3-5" or "5+" */
    private String experienceYears;

    @Size(max = 20, message = "Max 20 skills")
    private List<String> skillsRequired;

    /** e.g. JUNIOR, MID, SENIOR, LEAD, PRINCIPAL */
    private String seniorityLevel;

    /** FULL_TIME, CONTRACT, INTERNSHIP */
    private String employmentType;

    @Size(max = 2000, message = "Additional context max 2000 chars")
    private String additionalContext;
}
