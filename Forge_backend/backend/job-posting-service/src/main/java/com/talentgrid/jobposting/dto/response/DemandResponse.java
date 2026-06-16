package com.talentgrid.jobposting.dto.response;

import com.talentgrid.jobposting.entity.Demand;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Mirrors the frontend Demand TypeScript interface exactly.
 */
@Data
@Builder
public class DemandResponse {

    private Long id;
    private Long demandId;
    private String ref;
    private String title;
    private String description;
    private String level;
    private String employmentType;
    private Double experienceYears;
    private String workMode;
    private String locationCity;
    private String locationState;
    private String locationCountry;
    private String department;
    private String jobCategory;
    private List<String> skills;
    private Integer requiredCount;
    private int filledCount;
    private String priority;
    private Double budgetMin;
    private Double budgetMax;
    private String currency;
    private boolean showSalary;
    private String demandStatus;
    private String targetDate;
    private boolean aiRecommended;
    private LocalDateTime receivedAt;

    public static DemandResponse from(Demand d) {
        double min = d.getSalaryMin() != null ? d.getSalaryMin().doubleValue() : 0;
        double max = d.getSalaryMax() != null ? d.getSalaryMax().doubleValue() : 0;

        return DemandResponse.builder()
                .id(d.getId())
                .demandId(d.getDemandId())
                .ref("DEM-" + d.getDemandId())
                .title(d.getRoleTitle())
                .description(d.getDescription())
                .level(mapLevel(d.getExperienceLevel()))
                .employmentType(d.getEmploymentType())
                .experienceYears(d.getExperienceYears())
                .workMode(d.getWorkMode())
                .locationCity(d.getLocationCity())
                .locationState(d.getLocationState())
                .locationCountry(d.getLocationCountry())
                .department(d.getDepartment())
                .jobCategory(d.getJobCategory())
                .skills(d.getSkillsRequired())
                .requiredCount(d.getRequiredCount())
                .filledCount(0)
                .priority("MEDIUM")
                .budgetMin(min)
                .budgetMax(max)
                .currency(d.getCurrency() != null ? d.getCurrency() : "USD")
                .showSalary(Boolean.TRUE.equals(d.getShowSalary()))
                .demandStatus(d.getDemandStatus())
                .targetDate(d.getTargetDate() != null
                        ? d.getTargetDate().atZone(ZoneId.of("UTC")).toLocalDate().toString()
                        : null)
                .aiRecommended(false)
                .receivedAt(d.getReceivedAt())
                .build();
    }

    private static String mapLevel(String experienceLevel) {
        if (experienceLevel == null) return "MID";
        return switch (experienceLevel.toUpperCase()) {
            case "JUNIOR", "ENTRY" -> "JUNIOR";
            case "SENIOR", "SR" -> "SENIOR";
            case "LEAD", "PRINCIPAL", "STAFF" -> "LEAD";
            default -> "MID";
        };
    }
}
