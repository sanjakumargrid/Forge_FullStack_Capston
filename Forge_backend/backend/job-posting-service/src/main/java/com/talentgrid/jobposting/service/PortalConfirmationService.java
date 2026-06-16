package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.dto.embedded.ChannelDto;
import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.enums.JobStatus;
import com.talentgrid.jobposting.event.PortalConfirmationEvent;
import com.talentgrid.jobposting.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalConfirmationService {

    private final JobPostingRepository jobPostingRepository;
    private final NotificationService notificationService;

    @Transactional
    public void handle(PortalConfirmationEvent event) {
        if (event == null || event.getPayload() == null) return;

        Long jobPostingId = event.getPayload().getJobPostingId();
        JobPosting jp = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new RuntimeException("Job posting not found: " + jobPostingId));

        switch (event.getEventType()) {
            case "JOB_LIVE"       -> handleLive(jp, event);
            case "JOB_TAKEN_DOWN" -> handleTakenDown(jp, event);
            case "JOB_FAILED"     -> handleFailed(jp, event);
            default -> log.warn("Unknown portal confirmation eventType: {}", event.getEventType());
        }
    }

    private void handleLive(JobPosting jp, PortalConfirmationEvent event) {
        jp.setChannels(withPortalState(jp.getChannels(), "live"));
        jp.setPostingStatus(JobStatus.LIVE);
        jobPostingRepository.save(jp);

        notificationService.send(jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                "PORTAL_JOB_LIVE", "Job is Live on Career Portal",
                String.format("'%s' is now live on the career portal.", jp.getTitle()));

        log.info("jobPostingId={} is now LIVE on portal. portalUrl={}",
                jp.getId(), event.getPayload().getPortalUrl());
    }

    private void handleTakenDown(JobPosting jp, PortalConfirmationEvent event) {
        jp.setChannels(withPortalState(jp.getChannels(), "idle"));
        jobPostingRepository.save(jp);

        notificationService.send(jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                "PORTAL_JOB_TAKEN_DOWN", "Job Removed from Career Portal",
                String.format("'%s' has been removed from the career portal.", jp.getTitle()));

        log.info("jobPostingId={} taken down from portal.", jp.getId());
    }

    private void handleFailed(JobPosting jp, PortalConfirmationEvent event) {
        jp.setChannels(withPortalState(jp.getChannels(), "failed"));
        jobPostingRepository.save(jp);

        String reason = event.getPayload().getReason();
        notificationService.send(jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                "PORTAL_JOB_FAILED", "Career Portal Publishing Failed",
                String.format("'%s' failed to publish to the career portal: %s", jp.getTitle(), reason));

        log.error("jobPostingId={} failed on portal. reason={}", jp.getId(), reason);
    }

    private List<ChannelDto> withPortalState(List<ChannelDto> channels, String state) {
        return channels.stream()
                .map(c -> "portal".equals(c.getKey()) ? new ChannelDto(c.getKey(), c.getLabel(), state) : c)
                .toList();
    }
}
