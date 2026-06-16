package com.talentgrid.jobposting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateJobPostingRequest {

    private Long demandId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String responsibilities;
    private String requirements;
    private String benefits;
    private String employmentType;
    private String level;
    private Double experienceYears;
    private String workMode;
    private String locationCity;
    private String locationState;
    private String locationCountry;
    private String department;
    private String jobCategory;
    private List<String> skills;
    private BigDecimal budget;
    private Integer requiredCount;
    private String currency;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Boolean showSalary;
    private LocalDate applicationDeadline;
}
