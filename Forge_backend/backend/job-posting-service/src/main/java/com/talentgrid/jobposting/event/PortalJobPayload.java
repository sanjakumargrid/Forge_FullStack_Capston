package com.talentgrid.jobposting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalJobPayload {
    private Long jobPostingId;
    private String title;
    private String description;
    private String responsibilities;
    private String requirements;
    private String benefits;
    private String department;
    private String jobCategory;
    private String locationCity;
    private String locationState;
    private String locationCountry;
    private String workMode;
    private String employmentType;
    private String level;
    private Double experienceYears;
    private List<String> skills;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private Boolean showSalary;
    private LocalDate applicationDeadline;
    private Integer requiredCount;
    private Instant approvedAt;
    private Long approvedBy;
}
