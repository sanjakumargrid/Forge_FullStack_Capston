package com.talentgrid.jobposting.kafka;

import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.event.PortalJobEvent;
import com.talentgrid.jobposting.event.PortalJobPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${portal.kafka.topic.job-events:portal-job-events}")
    private String jobEventsTopic;

    public void publishJob(JobPosting jp) {
        send("JOB_PUBLISHED", jp);
    }

    public void unpublishJob(JobPosting jp) {
        send("JOB_UNPUBLISHED", jp);
    }

    private void send(String eventType, JobPosting jp) {
        String correlationId = UUID.randomUUID().toString();

        PortalJobPayload payload = PortalJobPayload.builder()
                .jobPostingId(jp.getId())
                .title(jp.getTitle())
                .description(jp.getDescription())
                .responsibilities(jp.getResponsibilities())
                .requirements(jp.getRequirements())
                .benefits(jp.getBenefits())
                .department(jp.getDepartment())
                .jobCategory(jp.getJobCategory())
                .locationCity(jp.getLocationCity())
                .locationState(jp.getLocationState())
                .locationCountry(jp.getLocationCountry())
                .workMode(jp.getWorkMode())
                .employmentType(jp.getEmploymentType())
                .level(jp.getLevel())
                .experienceYears(jp.getExperienceYears())
                .skills(jp.getSkills())
                .salaryMin(jp.getSalaryMin())
                .salaryMax(jp.getSalaryMax())
                .currency(jp.getCurrency())
                .showSalary(jp.getShowSalary())
                .applicationDeadline(jp.getApplicationDeadline())
                .requiredCount(jp.getRequiredCount())
                .approvedAt(jp.getApprovedAt() != null
                        ? jp.getApprovedAt().toInstant(ZoneOffset.UTC) : null)
                .approvedBy(jp.getApprovedBy())
                .build();

        PortalJobEvent event = PortalJobEvent.of(eventType, correlationId, payload);

        kafkaTemplate.send(jobEventsTopic, String.valueOf(jp.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for jobPostingId={}: {}",
                                eventType, jp.getId(), ex.getMessage());
                    } else {
                        log.info("Published {} for jobPostingId={} | partition={} | offset={}",
                                eventType, jp.getId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
