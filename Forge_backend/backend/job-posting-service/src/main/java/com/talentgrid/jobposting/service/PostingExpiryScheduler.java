package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.enums.JobStatus;
import com.talentgrid.jobposting.kafka.PortalEventProducer;
import com.talentgrid.jobposting.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingExpiryScheduler {

    private final JobPostingRepository jobPostingRepository;
    private final SseEmitterService sseEmitterService;
    private final PortalEventProducer portalEventProducer;
    private final NotificationService notificationService;

    private static final List<JobStatus> ACTIVE_STATUSES =
            List.of(JobStatus.READY_TO_PUBLISH, JobStatus.LIVE);

    /** Runs every hour. Closes any posting whose applicationDeadline has passed. */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void expireOverduePostings() {
        LocalDate today = LocalDate.now();

        List<JobPosting> expired = jobPostingRepository
                .findByPostingStatusInAndApplicationDeadlineBefore(ACTIVE_STATUSES, today);

        if (expired.isEmpty()) {
            log.debug("No postings to expire today ({})", today);
            return;
        }

        log.info("Auto-expiring {} overdue postings", expired.size());

        for (JobPosting jp : expired) {
            jp.setPreviousStatus(jp.getPostingStatus());
            jp.setPostingStatus(JobStatus.CLOSED);
            jobPostingRepository.save(jp);

            // Notify the career portal in real time
            sseEmitterService.broadcast("JOB_UNPUBLISHED", Map.of("jobId", jp.getId()));

            // Notify external portal service via Kafka
            portalEventProducer.unpublishJob(jp);

            // Notify the recruiter
            notificationService.send(
                    jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                    "POSTING_EXPIRED", "Job Posting Auto-Closed",
                    String.format(
                            "'%s' has been automatically closed — the application deadline (%s) has passed.",
                            jp.getTitle(), jp.getApplicationDeadline())
            );
        }

        log.info("Auto-expired {} postings", expired.size());
    }
}
