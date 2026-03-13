package com.repointel.repository;

import com.repointel.model.ComplexityMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplexityMetricRepository extends JpaRepository<ComplexityMetric, Long> {
    List<ComplexityMetric> findByJobId(String jobId);
    List<ComplexityMetric> findByJobIdOrderByCcScoreDesc(String jobId);

    @Query("SELECT c FROM ComplexityMetric c WHERE c.jobId = ?1 AND c.ccScore >= ?2")
    List<ComplexityMetric> findHighComplexity(String jobId, int threshold);

    @Query("SELECT c.filePath, AVG(c.ccScore) FROM ComplexityMetric c WHERE c.jobId = ?1 GROUP BY c.filePath")
    List<Object[]> findAvgComplexityByFile(String jobId);
}
