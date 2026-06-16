package com.talentgrid.jobposting.repository;

import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    List<JobPosting> findByPostingStatus(JobStatus status);

    List<JobPosting> findByRecruiterId(Long recruiterId);

    List<JobPosting> findByPostingStatusIn(List<JobStatus> statuses);

    long countByPostingStatus(JobStatus status);

    /** Used by expiry scheduler: active postings whose deadline has passed. */
    List<JobPosting> findByPostingStatusInAndApplicationDeadlineBefore(
            List<JobStatus> statuses, LocalDate deadline);

    /** Used by demand-closure handler: active postings tied to a specific demand. */
    List<JobPosting> findByDemandIdAndPostingStatusIn(Long demandId, List<JobStatus> statuses);
}
