package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.dto.embedded.ChannelDto;
import com.talentgrid.jobposting.dto.request.CreateJobPostingRequest;
import com.talentgrid.jobposting.dto.request.DeclineRequest;
import com.talentgrid.jobposting.dto.response.JobPostingResponse;
import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.entity.JobPostingApproval;
import com.talentgrid.jobposting.enums.ApprovalAction;
import com.talentgrid.jobposting.enums.JobStatus;
import com.talentgrid.jobposting.exception.InvalidStateException;
import com.talentgrid.jobposting.exception.ResourceNotFoundException;
import com.talentgrid.jobposting.kafka.PortalEventProducer;
import com.talentgrid.jobposting.repository.JobPostingApprovalRepository;
import com.talentgrid.jobposting.repository.JobPostingRepository;
import com.talentgrid.jobposting.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final JobPostingApprovalRepository approvalRepository;
    private final NotificationService notificationService;
    private final PortalEventProducer portalEventProducer;
    private final SseEmitterService sseEmitterService;

    // HM identity — demo value; replace with user-service lookup in production
    private static final Long HM_USER_ID = 2L;
    private static final String HM_USER_EMAIL = "hm@talentgrid.com";

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse create(CreateJobPostingRequest req, AuthenticatedUser actor) {
        JobPosting jp = buildFrom(req, actor);
        return JobPostingResponse.from(jobPostingRepository.save(jp));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<JobPostingResponse> getAll() {
        return jobPostingRepository.findAll().stream().map(JobPostingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<JobPostingResponse> getByStatus(JobStatus status) {
        return jobPostingRepository.findByPostingStatus(status).stream().map(JobPostingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<JobPostingResponse> getByStatuses(List<JobStatus> statuses) {
        return jobPostingRepository.findByPostingStatusIn(statuses).stream().map(JobPostingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getById(Long id) {
        return JobPostingResponse.from(findOrThrow(id));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse update(Long id, CreateJobPostingRequest req, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.DRAFT && jp.getPostingStatus() != JobStatus.DECLINED) {
            throw new InvalidStateException("Posting can only be edited in DRAFT or DECLINED state");
        }
        applyFields(jp, req);
        return JobPostingResponse.from(jobPostingRepository.save(jp));
    }

    // ── Save Draft ────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse saveDraft(Long id, CreateJobPostingRequest req, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.DRAFT && jp.getPostingStatus() != JobStatus.DECLINED) {
            throw new InvalidStateException("Cannot save draft in state: " + jp.getPostingStatus());
        }
        applyFields(jp, req);
        jp.setPostingStatus(JobStatus.DRAFT);
        return JobPostingResponse.from(jobPostingRepository.save(jp));
    }

    // ── Submit for Approval ───────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse submitForApproval(Long id, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.DRAFT && jp.getPostingStatus() != JobStatus.DECLINED) {
            throw new InvalidStateException("Only DRAFT or DECLINED postings can be submitted");
        }
        jp.setPreviousStatus(jp.getPostingStatus());
        jp.setPostingStatus(JobStatus.PENDING_APPROVAL);
        jp.setApprovalStatus(ApprovalAction.SUBMITTED);
        jobPostingRepository.save(jp);

        recordApproval(jp.getId(), ApprovalAction.SUBMITTED, null, actor.getUserId(), actor.getEmail());

        notificationService.send(HM_USER_ID, HM_USER_EMAIL, jp.getId(),
                "APPROVAL_REQUESTED", "New Job Posting Requires Approval",
                String.format("'%s' submitted by %s awaits your approval.", jp.getTitle(), actor.getEmail()));

        return JobPostingResponse.from(jp);
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse approve(Long id, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.PENDING_APPROVAL) {
            throw new InvalidStateException("Only PENDING_APPROVAL postings can be approved");
        }
        jp.setPreviousStatus(jp.getPostingStatus());
        jp.setPostingStatus(JobStatus.READY_TO_PUBLISH);
        jp.setApprovalStatus(ApprovalAction.APPROVED);
        jp.setApprovedBy(actor.getUserId());
        jp.setApprovedAt(LocalDateTime.now());
        // Portal channel goes to "pending" — becomes "live" when career portal confirms via Kafka
        jp.setChannels(withPortalPending(jp.getChannels()));
        jobPostingRepository.save(jp);

        recordApproval(jp.getId(), ApprovalAction.APPROVED, null, actor.getUserId(), actor.getEmail());

        notificationService.send(jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                "POSTING_APPROVED", "Job Posting Approved",
                String.format("'%s' has been approved and is being sent to the career portal.", jp.getTitle()));

        portalEventProducer.publishJob(jp);

        // Broadcast to all connected career portal clients in real time
        sseEmitterService.broadcast("JOB_PUBLISHED", JobPostingResponse.from(jp));

        return JobPostingResponse.from(jp);
    }

    // ── Decline ───────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse decline(Long id, DeclineRequest req, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.PENDING_APPROVAL) {
            throw new InvalidStateException("Only PENDING_APPROVAL postings can be declined");
        }
        jp.setPreviousStatus(jp.getPostingStatus());
        jp.setPostingStatus(JobStatus.DECLINED);
        jp.setApprovalStatus(ApprovalAction.DECLINED);
        jp.setDeclineReason(req.getReason());
        jobPostingRepository.save(jp);

        recordApproval(jp.getId(), ApprovalAction.DECLINED, req.getReason(), actor.getUserId(), actor.getEmail());

        notificationService.send(jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                "POSTING_DECLINED", "Job Posting Declined",
                String.format("'%s' was declined. Reason: %s", jp.getTitle(), req.getReason()));

        return JobPostingResponse.from(jp);
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse publish(Long id, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.READY_TO_PUBLISH) {
            throw new InvalidStateException("Only READY_TO_PUBLISH postings can be published");
        }
        jp.setPreviousStatus(jp.getPostingStatus());
        jp.setPostingStatus(JobStatus.LIVE);
        jp.setApprovalStatus(ApprovalAction.PUBLISHED);
        jp.setPublishedAt(LocalDateTime.now());
        jp.setChannels(withAllLive(jp.getChannels()));
        jobPostingRepository.save(jp);

        recordApproval(jp.getId(), ApprovalAction.PUBLISHED, null, actor.getUserId(), actor.getEmail());

        notificationService.send(actor.getUserId(), actor.getEmail(), jp.getId(),
                "POSTING_PUBLISHED", "Job Posting is Now Live",
                String.format("'%s' is now live on the careers portal.", jp.getTitle()));

        return JobPostingResponse.from(jp);
    }

    // ── Close (unpublish from portal) ─────────────────────────────────────────

    @Transactional
    public JobPostingResponse closeJob(Long id, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        if (jp.getPostingStatus() != JobStatus.LIVE && jp.getPostingStatus() != JobStatus.READY_TO_PUBLISH) {
            throw new InvalidStateException("Only LIVE or READY_TO_PUBLISH postings can be closed");
        }
        jp.setPreviousStatus(jp.getPostingStatus());
        jp.setPostingStatus(JobStatus.CLOSED);
        // Portal channel goes to "pending" — becomes "idle" when career portal confirms takedown
        jp.setChannels(withPortalPending(jp.getChannels()));
        jobPostingRepository.save(jp);

        notificationService.send(jp.getRecruiterId(), jp.getRecruiterEmail(), jp.getId(),
                "POSTING_CLOSED", "Job Posting Closed",
                String.format("'%s' is being removed from the career portal.", jp.getTitle()));

        portalEventProducer.unpublishJob(jp);

        // Broadcast removal to all connected career portal clients in real time
        sseEmitterService.broadcast("JOB_UNPUBLISHED", Map.of("jobId", id));

        return JobPostingResponse.from(jp);
    }

    // ── Close active postings for a demand (demand-closure flow) ──────────────

    @Transactional
    public void closePostingsForDemand(Long demandId) {
        List<JobStatus> active = List.of(JobStatus.READY_TO_PUBLISH, JobStatus.LIVE, JobStatus.DRAFT, JobStatus.PENDING_APPROVAL);
        List<JobPosting> affected = jobPostingRepository.findByDemandIdAndPostingStatusIn(demandId, active);
        for (JobPosting jp : affected) {
            jp.setPreviousStatus(jp.getPostingStatus());
            jp.setPostingStatus(JobStatus.CLOSED);
            jobPostingRepository.save(jp);
            portalEventProducer.unpublishJob(jp);
            sseEmitterService.broadcast("JOB_UNPUBLISHED", Map.of("jobId", jp.getId()));
            log.info("Closed posting id={} due to demand {} closure", jp.getId(), demandId);
        }
    }

    // ── Channel publish ───────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse publishChannel(Long id, String channel, AuthenticatedUser actor) {
        JobPosting jp = findOrThrow(id);
        List<ChannelDto> updated = jp.getChannels().stream()
                .map(c -> c.getKey().equals(channel) ? new ChannelDto(c.getKey(), c.getLabel(), "live") : c)
                .toList();
        jp.setChannels(updated);
        return JobPostingResponse.from(jobPostingRepository.save(jp));
    }

    // ── Stats (for dashboard) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new java.util.HashMap<>();
        for (JobStatus status : JobStatus.values()) {
            stats.put(status.name(), jobPostingRepository.countByPostingStatus(status));
        }
        return stats;
    }

    // ── Approval History ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApprovalHistory(Long jobPostingId) {
        return approvalRepository.findByJobPostingIdOrderByActionAtDesc(jobPostingId)
                .stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "jobPostingId", a.getJobPostingId(),
                        "action", a.getAction(),
                        "comments", a.getComments() != null ? a.getComments() : "",
                        "actionByEmail", a.getActionByEmail() != null ? a.getActionByEmail() : "",
                        "actionAt", a.getActionAt().toString()
                )).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JobPosting findOrThrow(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found: " + id));
    }

    private void recordApproval(Long jobPostingId, ApprovalAction action, String comments, Long actorId, String actorEmail) {
        approvalRepository.save(JobPostingApproval.builder()
                .jobPostingId(jobPostingId)
                .action(action)
                .comments(comments)
                .actionBy(actorId)
                .actionByEmail(actorEmail)
                .build());
    }

    private JobPosting buildFrom(CreateJobPostingRequest req, AuthenticatedUser actor) {
        return JobPosting.builder()
                .demandId(req.getDemandId())
                .title(req.getTitle())
                .description(req.getDescription())
                .responsibilities(req.getResponsibilities())
                .requirements(req.getRequirements())
                .benefits(req.getBenefits())
                .employmentType(req.getEmploymentType())
                .level(req.getLevel())
                .experienceYears(req.getExperienceYears())
                .workMode(req.getWorkMode())
                .locationCity(req.getLocationCity())
                .locationState(req.getLocationState())
                .locationCountry(req.getLocationCountry())
                .department(req.getDepartment())
                .jobCategory(req.getJobCategory())
                .skills(req.getSkills() != null ? req.getSkills() : new ArrayList<>())
                .budget(req.getBudget())
                .requiredCount(req.getRequiredCount())
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .salaryMin(req.getSalaryMin())
                .salaryMax(req.getSalaryMax())
                .showSalary(req.getShowSalary() != null ? req.getShowSalary() : false)
                .applicationDeadline(req.getApplicationDeadline())
                .postingStatus(JobStatus.DRAFT)
                .recruiterId(actor.getUserId())
                .recruiterEmail(actor.getEmail())
                .build();
    }

    private void applyFields(JobPosting jp, CreateJobPostingRequest req) {
        if (req.getTitle() != null) jp.setTitle(req.getTitle());
        if (req.getDescription() != null) jp.setDescription(req.getDescription());
        if (req.getResponsibilities() != null) jp.setResponsibilities(req.getResponsibilities());
        if (req.getRequirements() != null) jp.setRequirements(req.getRequirements());
        if (req.getBenefits() != null) jp.setBenefits(req.getBenefits());
        if (req.getEmploymentType() != null) jp.setEmploymentType(req.getEmploymentType());
        if (req.getLevel() != null) jp.setLevel(req.getLevel());
        if (req.getExperienceYears() != null) jp.setExperienceYears(req.getExperienceYears());
        if (req.getWorkMode() != null) jp.setWorkMode(req.getWorkMode());
        if (req.getLocationCity() != null) jp.setLocationCity(req.getLocationCity());
        if (req.getLocationState() != null) jp.setLocationState(req.getLocationState());
        if (req.getLocationCountry() != null) jp.setLocationCountry(req.getLocationCountry());
        if (req.getDepartment() != null) jp.setDepartment(req.getDepartment());
        if (req.getJobCategory() != null) jp.setJobCategory(req.getJobCategory());
        if (req.getSkills() != null) jp.setSkills(req.getSkills());
        if (req.getBudget() != null) jp.setBudget(req.getBudget());
        if (req.getRequiredCount() != null) jp.setRequiredCount(req.getRequiredCount());
        if (req.getCurrency() != null) jp.setCurrency(req.getCurrency());
        if (req.getSalaryMin() != null) jp.setSalaryMin(req.getSalaryMin());
        if (req.getSalaryMax() != null) jp.setSalaryMax(req.getSalaryMax());
        if (req.getShowSalary() != null) jp.setShowSalary(req.getShowSalary());
        if (req.getApplicationDeadline() != null) jp.setApplicationDeadline(req.getApplicationDeadline());
    }

    private List<ChannelDto> withPortalPending(List<ChannelDto> channels) {
        return channels.stream()
                .map(c -> "portal".equals(c.getKey()) ? new ChannelDto(c.getKey(), c.getLabel(), "pending") : c)
                .toList();
    }

    private List<ChannelDto> withAllLive(List<ChannelDto> channels) {
        return channels.stream()
                .map(c -> new ChannelDto(c.getKey(), c.getLabel(), "live"))
                .toList();
    }
}
