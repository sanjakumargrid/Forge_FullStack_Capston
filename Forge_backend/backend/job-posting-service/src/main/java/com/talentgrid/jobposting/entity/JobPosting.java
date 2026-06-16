package com.talentgrid.jobposting.entity;

import com.talentgrid.jobposting.converter.AnalyticsConverter;
import com.talentgrid.jobposting.converter.ChannelsConverter;
import com.talentgrid.jobposting.dto.embedded.AnalyticsDto;
import com.talentgrid.jobposting.dto.embedded.ChannelDto;
import com.talentgrid.jobposting.enums.ApprovalAction;
import com.talentgrid.jobposting.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_id")
    private Long demandId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "employment_type")
    private String employmentType;

    /** SeniorityLevel — JUNIOR | MID | SENIOR | LEAD | PRINCIPAL */
    private String level;

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

    @ElementCollection
    @CollectionTable(name = "job_posting_skills", joinColumns = @JoinColumn(name = "job_posting_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "required_count")
    private Integer requiredCount;

    @Builder.Default
    private String currency = "USD";

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "show_salary")
    @Builder.Default
    private Boolean showSalary = false;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "previous_status")
    @Enumerated(EnumType.STRING)
    private JobStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_status", nullable = false)
    @Builder.Default
    private JobStatus postingStatus = JobStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalAction approvalStatus;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "recruiter_id")
    private Long recruiterId;

    @Column(name = "recruiter_email")
    private String recruiterEmail;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "channels_json", columnDefinition = "TEXT")
    @Convert(converter = ChannelsConverter.class)
    @Builder.Default
    private List<ChannelDto> channels = List.of(
            new ChannelDto("linkedin", "LinkedIn", "idle"),
            new ChannelDto("indeed", "Indeed", "idle"),
            new ChannelDto("portal", "Careers Portal", "idle")
    );

    @Column(name = "analytics_json", columnDefinition = "TEXT")
    @Convert(converter = AnalyticsConverter.class)
    @Builder.Default
    private AnalyticsDto analytics = new AnalyticsDto();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
