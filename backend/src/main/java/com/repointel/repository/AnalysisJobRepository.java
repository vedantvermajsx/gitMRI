package com.repointel.repository;

import com.repointel.model.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, String> {
    List<AnalysisJob> findAllByOrderByCreatedAtDesc();
    Optional<AnalysisJob> findByRepoUrlAndStatus(String repoUrl, AnalysisJob.JobStatus status);
}
