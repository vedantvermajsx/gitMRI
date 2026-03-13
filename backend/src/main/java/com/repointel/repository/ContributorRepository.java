package com.repointel.repository;

import com.repointel.model.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, Long> {
    List<Contributor> findByJobIdOrderByCommitCountDesc(String jobId);
    List<Contributor> findByJobId(String jobId);
}
