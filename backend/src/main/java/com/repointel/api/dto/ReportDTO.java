package com.repointel.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private String jobId;
    private String repoName;
    private String repoUrl;
    private BigDecimal healthScore;
    private int totalFiles;
    private int totalClasses;
    private int totalMethods;
    private int totalLines;
    private BigDecimal avgComplexity;
    private int maxComplexity;
    private int busFactor;
    private int deadCodeCount;
    private BigDecimal deadCodeRatio;
    private int hotspotCount;
    private int totalCommits;
    private int contributorCount;
    private int dependencyCount;
    private LocalDateTime analyzedAt;
}
