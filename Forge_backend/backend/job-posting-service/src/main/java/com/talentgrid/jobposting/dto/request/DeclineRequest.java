package com.talentgrid.jobposting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeclineRequest {

    @NotBlank(message = "Decline reason is required")
    private String reason;
}
