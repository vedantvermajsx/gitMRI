package com.repointel.service;

import com.repointel.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates all pipeline results into a final Report with a composite health score.
 *
 * Health Score Formula (100 points total):
 *   - Complexity penalty:  max -25pts  (based on % methods with CC > 10)
 *   - Dead code penalty:   max -20pts  (based on dead code ratio)
 *   - Bus factor penalty:  max -20pts  (based on bus factor < 3)
 *   - Hotspot penalty:     max -20pts  (based on number of high hotspots)
 *   - Dependency penalty:  max -15pts  (placeholder for outdated deps)
 */
@Service
@Slf4j
public class ReportGenerator {

    public Report generateReport(String jobId,
                                  List<ComplexityMetric> complexityMetrics,
                                  List<DeadCodeItem> deadCodeItems,
                                  List<Hotspot> hotspots,
                                  List<Contributor> contributors,
                                  List<DependencyNode> depNodes,
                                  int busFactor,
                                  int totalCommits,
                                  int totalFiles,
                                  long totalLines) {

        // Aggregate stats
        int totalMethods = complexityMetrics.size();
        long distinctClasses = complexityMetrics.stream()
                .map(m -> m.getFilePath() + "#" + m.getClassName())
                .distinct().count();

        double avgCC = complexityMetrics.isEmpty() ? 0 :
                complexityMetrics.stream().mapToInt(ComplexityMetric::getCcScore).average().orElse(0);
        int maxCC = complexityMetrics.stream().mapToInt(ComplexityMetric::getCcScore).max().orElse(0);

        long deadCodeCount = deadCodeItems.size();
        double deadCodeRatio = totalMethods > 0 ? (double) deadCodeCount / totalMethods : 0;

        long highHotspots = hotspots.stream()
                .filter(h -> h.getHotspotScore() != null && h.getHotspotScore().doubleValue() > 0.7)
                .count();

        // Health score computation
        double score = 100.0;

        // Complexity penalty (max -25): based on % of methods with CC > 10
        long complexMethods = complexityMetrics.stream()
                .filter(m -> m.getCcScore() > 10).count();
        double complexRatio = totalMethods > 0 ? (double) complexMethods / totalMethods : 0;
        score -= complexRatio * 25;

        // Dead code penalty (max -20)
        score -= Math.min(deadCodeRatio * 40, 20);

        // Bus factor penalty (max -20)
        if (busFactor <= 1) score -= 20;
        else if (busFactor == 2) score -= 12;
        else if (busFactor == 3) score -= 5;

        // Hotspot penalty (max -20): based on high-risk hotspot count
        double hotspotPenalty = Math.min((double) highHotspots / Math.max(totalFiles, 1) * 40, 20);
        score -= hotspotPenalty;

        // Dependency penalty (max -15): placeholder, could check for known CVEs
        int depCount = (int) depNodes.stream().filter(n -> !"ROOT".equals(n.getNodeType())).count();
        if (depCount > 50) score -= 10;
        else if (depCount > 25) score -= 5;

        score = Math.max(0, Math.min(100, score));

        log.info("Report for job {}: health={}, CC avg={}, dead code={}, bus factor={}",
                jobId, score, avgCC, deadCodeCount, busFactor);

        return Report.builder()
                .id(UUID.randomUUID().toString())
                .jobId(jobId)
                .healthScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                .totalFiles(totalFiles)
                .totalClasses((int) distinctClasses)
                .totalMethods(totalMethods)
                .totalLines((int) totalLines)
                .avgComplexity(BigDecimal.valueOf(avgCC).setScale(2, RoundingMode.HALF_UP))
                .maxComplexity(maxCC)
                .busFactor(busFactor)
                .deadCodeCount((int) deadCodeCount)
                .deadCodeRatio(BigDecimal.valueOf(deadCodeRatio * 100).setScale(2, RoundingMode.HALF_UP))
                .hotspotCount((int) highHotspots)
                .totalCommits(totalCommits)
                .contributorCount(contributors.size())
                .dependencyCount(depCount)
                .build();
    }
}
