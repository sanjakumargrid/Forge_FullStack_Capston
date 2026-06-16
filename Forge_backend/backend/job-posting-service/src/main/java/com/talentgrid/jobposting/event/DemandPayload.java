package com.talentgrid.jobposting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandPayload {

    private Long demandId;
    private String roleTitle;
    private String description;
    private List<String> skillsRequired;
    private String employmentType;
    private String experienceLevel;
    private Double experienceYears;
    private String workMode;
    private String locationCity;
    private String locationState;
    private String locationCountry;
    private String department;
    private String jobCategory;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private Boolean showSalary;
    private String demandStatus;
    private Instant createdAt;
    private Integer requiredCount;
    private String previousStatus;
    private Instant targetDate;
}
