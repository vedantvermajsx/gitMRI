package com.repointel.repository;

import com.repointel.model.DependencyEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DependencyEdgeRepository extends JpaRepository<DependencyEdge, Long> {
    List<DependencyEdge> findByJobId(String jobId);
}
