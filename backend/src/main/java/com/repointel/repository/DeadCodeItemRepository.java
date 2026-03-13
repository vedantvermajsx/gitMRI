package com.repointel.repository;

import com.repointel.model.DeadCodeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadCodeItemRepository extends JpaRepository<DeadCodeItem, Long> {
    List<DeadCodeItem> findByJobId(String jobId);
    List<DeadCodeItem> findByJobIdAndRiskLevel(String jobId, String riskLevel);
    long countByJobId(String jobId);
}
