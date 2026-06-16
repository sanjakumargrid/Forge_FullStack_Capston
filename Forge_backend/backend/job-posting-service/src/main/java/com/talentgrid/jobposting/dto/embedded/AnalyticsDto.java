package com.talentgrid.jobposting.dto.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDto {
    private int views = 0;
    private int clicks = 0;
    private int applyStarts = 0;
    private int applyCompletions = 0;
}
