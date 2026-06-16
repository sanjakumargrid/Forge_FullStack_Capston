package com.talentgrid.jobposting.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JdSectionsResponse {
    private String summary;
    private String responsibilities;
    private String requirements;
    private String benefits;
    private int sectionCount;
}
