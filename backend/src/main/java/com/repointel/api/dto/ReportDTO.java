package com.repointel.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public ReportDTO() {}

    public String getJobId() { return jobId; }
    public String getRepoName() { return repoName; }
    public String getRepoUrl() { return repoUrl; }
    public BigDecimal getHealthScore() { return healthScore; }
    public int getTotalFiles() { return totalFiles; }
    public int getTotalClasses() { return totalClasses; }
    public int getTotalMethods() { return totalMethods; }
    public int getTotalLines() { return totalLines; }
    public BigDecimal getAvgComplexity() { return avgComplexity; }
    public int getMaxComplexity() { return maxComplexity; }
    public int getBusFactor() { return busFactor; }
    public int getDeadCodeCount() { return deadCodeCount; }
    public BigDecimal getDeadCodeRatio() { return deadCodeRatio; }
    public int getHotspotCount() { return hotspotCount; }
    public int getTotalCommits() { return totalCommits; }
    public int getContributorCount() { return contributorCount; }
    public int getDependencyCount() { return dependencyCount; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }

    public void setJobId(String v) { this.jobId = v; }
    public void setRepoName(String v) { this.repoName = v; }
    public void setRepoUrl(String v) { this.repoUrl = v; }
    public void setHealthScore(BigDecimal v) { this.healthScore = v; }
    public void setTotalFiles(int v) { this.totalFiles = v; }
    public void setTotalClasses(int v) { this.totalClasses = v; }
    public void setTotalMethods(int v) { this.totalMethods = v; }
    public void setTotalLines(int v) { this.totalLines = v; }
    public void setAvgComplexity(BigDecimal v) { this.avgComplexity = v; }
    public void setMaxComplexity(int v) { this.maxComplexity = v; }
    public void setBusFactor(int v) { this.busFactor = v; }
    public void setDeadCodeCount(int v) { this.deadCodeCount = v; }
    public void setDeadCodeRatio(BigDecimal v) { this.deadCodeRatio = v; }
    public void setHotspotCount(int v) { this.hotspotCount = v; }
    public void setTotalCommits(int v) { this.totalCommits = v; }
    public void setContributorCount(int v) { this.contributorCount = v; }
    public void setDependencyCount(int v) { this.dependencyCount = v; }
    public void setAnalyzedAt(LocalDateTime v) { this.analyzedAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ReportDTO r = new ReportDTO();
        public Builder jobId(String v) { r.jobId = v; return this; }
        public Builder repoName(String v) { r.repoName = v; return this; }
        public Builder repoUrl(String v) { r.repoUrl = v; return this; }
        public Builder healthScore(BigDecimal v) { r.healthScore = v; return this; }
        public Builder totalFiles(int v) { r.totalFiles = v; return this; }
        public Builder totalClasses(int v) { r.totalClasses = v; return this; }
        public Builder totalMethods(int v) { r.totalMethods = v; return this; }
        public Builder totalLines(int v) { r.totalLines = v; return this; }
        public Builder avgComplexity(BigDecimal v) { r.avgComplexity = v; return this; }
        public Builder maxComplexity(int v) { r.maxComplexity = v; return this; }
        public Builder busFactor(int v) { r.busFactor = v; return this; }
        public Builder deadCodeCount(int v) { r.deadCodeCount = v; return this; }
        public Builder deadCodeRatio(BigDecimal v) { r.deadCodeRatio = v; return this; }
        public Builder hotspotCount(int v) { r.hotspotCount = v; return this; }
        public Builder totalCommits(int v) { r.totalCommits = v; return this; }
        public Builder contributorCount(int v) { r.contributorCount = v; return this; }
        public Builder dependencyCount(int v) { r.dependencyCount = v; return this; }
        public Builder analyzedAt(LocalDateTime v) { r.analyzedAt = v; return this; }
        public ReportDTO build() { return r; }
    }
}
