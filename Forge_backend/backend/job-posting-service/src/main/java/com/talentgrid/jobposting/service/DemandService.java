package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.dto.response.DemandResponse;
import com.talentgrid.jobposting.entity.Demand;
import com.talentgrid.jobposting.event.DemandEvent;
import com.talentgrid.jobposting.event.DemandPayload;
import com.talentgrid.jobposting.exception.ResourceNotFoundException;
import com.talentgrid.jobposting.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;
    private final JobPostingService jobPostingService;

    @Transactional
    public void processKafkaEvent(DemandEvent event) {
        DemandPayload p = event.getPayload();
        if (p == null || p.getDemandId() == null) {
            log.warn("Received demand event with null payload or demandId, skipping");
            return;
        }

        // Demand closed — auto-close any linked active job postings
        if ("DEMAND_CLOSED".equals(event.getEventType())) {
            log.info("Demand {} closed — closing linked job postings", p.getDemandId());
            demandRepository.findByDemandId(p.getDemandId()).ifPresent(d -> {
                jobPostingService.closePostingsForDemand(d.getDemandId());
            });
            return;
        }

        if (!"DEMAND_OPEN_EXTERNAL".equals(event.getEventType())) {
            log.debug("Ignoring event type: {}", event.getEventType());
            return;
        }

        if (demandRepository.existsByDemandId(p.getDemandId())) {
            log.info("Demand {} already exists, skipping duplicate", p.getDemandId());
            return;
        }

        Demand demand = Demand.builder()
                .demandId(p.getDemandId())
                .roleTitle(p.getRoleTitle())
                .description(p.getDescription())
                .skillsRequired(p.getSkillsRequired())
                .employmentType(p.getEmploymentType())
                .experienceLevel(p.getExperienceLevel())
                .experienceYears(p.getExperienceYears())
                .workMode(p.getWorkMode())
                .locationCity(p.getLocationCity())
                .locationState(p.getLocationState())
                .locationCountry(p.getLocationCountry())
                .department(p.getDepartment())
                .jobCategory(p.getJobCategory())
                .salaryMin(p.getSalaryMin())
                .salaryMax(p.getSalaryMax())
                .currency(p.getCurrency())
                .showSalary(p.getShowSalary())
                .demandStatus(p.getDemandStatus())
                .requiredCount(p.getRequiredCount())
                .targetDate(p.getTargetDate())
                .eventId(event.getEventId())
                .correlationId(event.getCorrelationId())
                .build();

        demandRepository.save(demand);
        log.info("Persisted demand from Kafka: demandId={} role={}", p.getDemandId(), p.getRoleTitle());
    }

    @Transactional(readOnly = true)
    public List<DemandResponse> getAll() {
        return demandRepository.findAvailableDemands()
                .stream()
                .map(DemandResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DemandResponse getById(Long id) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found: " + id));
        return DemandResponse.from(demand);
    }

    @Transactional(readOnly = true)
    public DemandResponse getByDemandId(Long demandId) {
        Demand demand = demandRepository.findByDemandId(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found: " + demandId));
        return DemandResponse.from(demand);
    }
}
