package com.talentgrid.jobposting.repository;

import com.talentgrid.jobposting.entity.JobPostingApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingApprovalRepository extends JpaRepository<JobPostingApproval, Long> {

    List<JobPostingApproval> findByJobPostingIdOrderByActionAtDesc(Long jobPostingId);
}
