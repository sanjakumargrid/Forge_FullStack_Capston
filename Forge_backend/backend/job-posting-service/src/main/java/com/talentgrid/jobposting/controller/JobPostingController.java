package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.integration.JdGenerationRequest;
import com.talentgrid.jobposting.dto.integration.JdGenerationResponse;
import com.talentgrid.jobposting.dto.request.CreateJobPostingRequest;
import com.talentgrid.jobposting.dto.request.DeclineRequest;
import com.talentgrid.jobposting.dto.request.GenerateJdRequest;
import com.talentgrid.jobposting.dto.response.JdSectionsResponse;
import com.talentgrid.jobposting.dto.response.JobPostingResponse;
import com.talentgrid.jobposting.enums.JobStatus;
import com.talentgrid.jobposting.integration.AiIntegrationClient;
import com.talentgrid.jobposting.security.AuthenticatedUser;
import com.talentgrid.jobposting.service.JobPostingService;
import com.talentgrid.jobposting.service.PromptGuardService;
import com.talentgrid.jobposting.service.SseEmitterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final SseEmitterService sseEmitterService;
    private final PromptGuardService promptGuardService;
    private final AiIntegrationClient aiIntegrationClient;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<JobPostingResponse> create(
            @Valid @RequestBody CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobPostingService.create(req, user));
    }

    @GetMapping
    public ResponseEntity<List<JobPostingResponse>> getAll(
            @RequestParam(required = false) JobStatus status
    ) {
        List<JobPostingResponse> list = status != null
                ? jobPostingService.getByStatus(status)
                : jobPostingService.getAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPostingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.update(id, req, user));
    }

    // ── Draft ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/save-draft")
    public ResponseEntity<JobPostingResponse> saveDraft(
            @PathVariable Long id,
            @RequestBody(required = false) CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.saveDraft(id, req != null ? req : new CreateJobPostingRequest(), user));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/submit-for-approval")
    public ResponseEntity<JobPostingResponse> submitForApproval(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.submitForApproval(id, user));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<JobPostingResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.approve(id, user));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<JobPostingResponse> decline(
            @PathVariable Long id,
            @Valid @RequestBody DeclineRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.decline(id, req, user));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<JobPostingResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.publish(id, user));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<JobPostingResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.closeJob(id, user));
    }

    // ── Channel publish ───────────────────────────────────────────────────────

    @PostMapping("/{id}/channels/{channel}/publish")
    public ResponseEntity<JobPostingResponse> publishChannel(
            @PathVariable Long id,
            @PathVariable String channel,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.publishChannel(id, channel, user));
    }

    // ── AI generate JD (Team 4 contract) ─────────────────────────────────────

    @PostMapping("/generate-jd")
    public ResponseEntity<JdSectionsResponse> generateJd(
            @RequestBody GenerateJdRequest req
    ) {
        String sanitizedContext = promptGuardService.validateAndSanitize(
                req.getAdditionalContext(), "JD_GENERATION");

        JdGenerationRequest aiRequest = new JdGenerationRequest(
                req.getDemandId(),
                req.getRoleTitle(),
                req.getDepartment(),
                req.getLocation(),
                req.getWorkMode(),
                req.getExperienceYears(),
                req.getSkillsRequired(),
                req.getSeniorityLevel(),
                req.getEmploymentType(),
                sanitizedContext
        );

        JdGenerationResponse aiResponse = aiIntegrationClient.generateJobDescription(aiRequest);
        JdGenerationResponse.JdSections sections = aiResponse.sections();

        return ResponseEntity.ok(JdSectionsResponse.builder()
                .summary(sections.summary())
                .responsibilities(sections.responsibilities())
                .requirements(sections.requirements())
                .benefits(sections.benefits())
                .sectionCount(sections.sectionCount())
                .build());
    }

    // ── Job posting stats (for dashboard) ────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(jobPostingService.getStats());
    }

    // ── Public: live jobs for career portal (no auth required) ───────────────

    @GetMapping("/public/live")
    public ResponseEntity<List<JobPostingResponse>> getPublicLiveJobs() {
        return ResponseEntity.ok(
            jobPostingService.getByStatuses(List.of(JobStatus.READY_TO_PUBLISH, JobStatus.LIVE))
        );
    }

    // ── Public: SSE stream for real-time career portal updates ────────────────

    @GetMapping(value = "/public/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToPortalEvents(HttpServletResponse response) {
        // Tell nginx not to buffer this stream
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return sseEmitterService.subscribe();
    }

    // ── Approval audit trail ──────────────────────────────────────────────────

    @GetMapping("/{id}/approvals")
    public ResponseEntity<List<Map<String, Object>>> getApprovalHistory(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getApprovalHistory(id));
    }
}
