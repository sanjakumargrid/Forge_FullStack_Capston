package com.talentgrid.jobposting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "demands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_id", unique = true, nullable = false)
    private Long demandId;

    @Column(name = "role_title", nullable = false)
    private String roleTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "demand_skills", joinColumns = @JoinColumn(name = "demand_id"))
    @Column(name = "skill")
    private List<String> skillsRequired;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "experience_years")
    private Double experienceYears;

    @Column(name = "work_mode")
    private String workMode;

    @Column(name = "location_city")
    private String locationCity;

    @Column(name = "location_state")
    private String locationState;

    @Column(name = "location_country")
    private String locationCountry;

    private String department;

    @Column(name = "job_category")
    private String jobCategory;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    private String currency;

    @Column(name = "show_salary")
    private Boolean showSalary;

    @Column(name = "demand_status")
    private String demandStatus;

    @Column(name = "required_count")
    private Integer requiredCount;

    @Column(name = "target_date")
    private Instant targetDate;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        this.receivedAt = LocalDateTime.now();
    }
}
