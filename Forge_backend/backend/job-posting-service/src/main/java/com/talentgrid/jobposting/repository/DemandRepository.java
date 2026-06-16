package com.talentgrid.jobposting.repository;

import com.talentgrid.jobposting.entity.Demand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DemandRepository extends JpaRepository<Demand, Long> {

    Optional<Demand> findByDemandId(Long demandId);

    boolean existsByDemandId(Long demandId);

    // Returns demands that are not yet tied to an in-progress or live job posting.
    // DRAFT and DECLINED postings are excluded from the filter so the recruiter
    // can still pick up those demands and retry.
    @Query("""
        SELECT d FROM Demand d
        WHERE d.demandId NOT IN (
            SELECT jp.demandId FROM JobPosting jp
            WHERE jp.demandId IS NOT NULL
            AND jp.postingStatus IN (
                com.talentgrid.jobposting.enums.JobStatus.PENDING_APPROVAL,
                com.talentgrid.jobposting.enums.JobStatus.READY_TO_PUBLISH,
                com.talentgrid.jobposting.enums.JobStatus.LIVE
            )
        )
    """)
    List<Demand> findAvailableDemands();
}
