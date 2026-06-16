package com.talentgrid.jobposting.dto.response;

import com.talentgrid.jobposting.dto.embedded.AnalyticsDto;
import com.talentgrid.jobposting.dto.embedded.ChannelDto;
import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.enums.ApprovalAction;
import com.talentgrid.jobposting.enums.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirrors the frontend JobPosting TypeScript interface exactly.
 * Field names must match — the Angular HttpClient deserialises directly into that interface.
 */
@Data
@Builder
public class JobPostingResponse {

    private Long id;
    private Long demandId;
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
    private JobStatus previousStatus;
    private JobStatus postingStatus;
    private ApprovalAction approvalStatus;
    private String declineReason;
    private Long recruiterId;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChannelDto> channels;
    private AnalyticsDto analytics;

    public static JobPostingResponse from(JobPosting jp) {
        return JobPostingResponse.builder()
                .id(jp.getId())
                .demandId(jp.getDemandId())
                .title(jp.getTitle())
                .description(jp.getDescription())
                .responsibilities(jp.getResponsibilities())
                .requirements(jp.getRequirements())
                .benefits(jp.getBenefits())
                .employmentType(jp.getEmploymentType())
                .level(jp.getLevel())
                .experienceYears(jp.getExperienceYears())
                .workMode(jp.getWorkMode())
                .locationCity(jp.getLocationCity())
                .locationState(jp.getLocationState())
                .locationCountry(jp.getLocationCountry())
                .department(jp.getDepartment())
                .jobCategory(jp.getJobCategory())
                .skills(jp.getSkills())
                .budget(jp.getBudget())
                .requiredCount(jp.getRequiredCount())
                .currency(jp.getCurrency())
                .salaryMin(jp.getSalaryMin())
                .salaryMax(jp.getSalaryMax())
                .showSalary(jp.getShowSalary())
                .applicationDeadline(jp.getApplicationDeadline())
                .previousStatus(jp.getPreviousStatus())
                .postingStatus(jp.getPostingStatus())
                .approvalStatus(jp.getApprovalStatus())
                .declineReason(jp.getDeclineReason())
                .recruiterId(jp.getRecruiterId())
                .approvedBy(jp.getApprovedBy())
                .approvedAt(jp.getApprovedAt())
                .publishedAt(jp.getPublishedAt())
                .createdAt(jp.getCreatedAt())
                .updatedAt(jp.getUpdatedAt())
                .channels(jp.getChannels())
                .analytics(jp.getAnalytics())
                .build();
    }
}
