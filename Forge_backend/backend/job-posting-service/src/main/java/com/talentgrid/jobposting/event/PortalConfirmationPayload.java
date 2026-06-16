package com.talentgrid.jobposting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalConfirmationPayload {
    private Long jobPostingId;
    private String portalJobId;   // career portal's own reference ID
    private String portalUrl;     // public URL where the job is live (set on JOB_LIVE)
    private String reason;        // error detail for JOB_FAILED
}
