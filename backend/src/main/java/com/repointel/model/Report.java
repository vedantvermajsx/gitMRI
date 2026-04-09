package com.repointel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "health_score", precision = 5, scale = 2)
    private BigDecimal healthScore;

    @Column(name = "total_files")
    private int totalFiles;

    @Column(name = "total_classes")
    private int totalClasses;

    @Column(name = "total_methods")
    private int totalMethods;

    @Column(name = "total_lines")
    private int totalLines;

    @Column(name = "avg_complexity", precision = 5, scale = 2)
    private BigDecimal avgComplexity;

    @Column(name = "max_complexity")
    private int maxComplexity;

    @Column(name = "bus_factor")
    private int busFactor;

    @Column(name = "dead_code_count")
    private int deadCodeCount;

    @Column(name = "dead_code_ratio", precision = 5, scale = 2)
    private BigDecimal deadCodeRatio;

    @Column(name = "hotspot_count")
    private int hotspotCount;

    @Column(name = "total_commits")
    private int totalCommits;

    @Column(name = "contributor_count")
    private int contributorCount;

    @Column(name = "dependency_count")
    private int dependencyCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Report() {}

    public String getId() { return id; }
    public String getJobId() { return jobId; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setJobId(String jobId) { this.jobId = jobId; }
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

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Report r = new Report();
        public Builder id(String v) { r.id = v; return this; }
        public Builder jobId(String v) { r.jobId = v; return this; }
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
        public Report build() { return r; }
    }
}
