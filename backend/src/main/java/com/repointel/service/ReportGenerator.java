package com.repointel.service;

import com.repointel.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    public Report generateReport(String jobId,
                                  List<ComplexityMetric> complexityMetrics,
                                  List<DeadCodeItem> deadCodeItems,
                                  List<Hotspot> hotspots,
                                  List<Contributor> contributors,
                                  List<DependencyNode> depNodes,
                                  int busFactor, int totalCommits,
                                  int totalFiles, long totalLines) {

        int totalMethods = complexityMetrics.size();

        long distinctClasses = complexityMetrics.stream()
                .map(m -> m.getFilePath() + "#" + m.getClassName())
                .distinct().count();

        double avgCC = complexityMetrics.isEmpty() ? 0 :
                complexityMetrics.stream().mapToInt(ComplexityMetric::getCcScore).average().orElse(0);
        int maxCC = complexityMetrics.stream().mapToInt(ComplexityMetric::getCcScore).max().orElse(0);

        int deadCodeCount = deadCodeItems.size();
        double deadCodeRatio = totalMethods > 0 ? (double) deadCodeCount / totalMethods : 0;

        long highHotspots = hotspots.stream()
                .filter(h -> h.getHotspotScore() != null && h.getHotspotScore().doubleValue() > 0.7)
                .count();

        double score = 100.0;

        long complexMethods = complexityMetrics.stream().filter(m -> m.getCcScore() > 10).count();
        double complexRatio = totalMethods > 0 ? (double) complexMethods / totalMethods : 0;
        score -= complexRatio * 25;

        score -= Math.min(deadCodeRatio * 40, 20);

        if (busFactor <= 1) score -= 20;
        else if (busFactor == 2) score -= 12;
        else if (busFactor == 3) score -= 5;

        score -= Math.min((double) highHotspots / Math.max(totalFiles, 1) * 40, 20);

        int depCount = (int) depNodes.stream().filter(n -> !"ROOT".equals(n.getNodeType())).count();
        if (depCount > 50) score -= 10;
        else if (depCount > 25) score -= 5;

        score = Math.max(0, Math.min(100, score));

        log.info("Report for {}: health={}, avgCC={}, deadCode={}, busFactor={}",
                jobId, score, avgCC, deadCodeCount, busFactor);

        return Report.builder()
                .id(UUID.randomUUID().toString()).jobId(jobId)
                .healthScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                .totalFiles(totalFiles).totalClasses((int) distinctClasses)
                .totalMethods(totalMethods).totalLines((int) totalLines)
                .avgComplexity(BigDecimal.valueOf(avgCC).setScale(2, RoundingMode.HALF_UP))
                .maxComplexity(maxCC).busFactor(busFactor)
                .deadCodeCount(deadCodeCount)
                .deadCodeRatio(BigDecimal.valueOf(deadCodeRatio * 100).setScale(2, RoundingMode.HALF_UP))
                .hotspotCount((int) highHotspots).totalCommits(totalCommits)
                .contributorCount(contributors.size()).dependencyCount(depCount)
                .build();
    }
}
