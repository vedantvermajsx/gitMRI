package com.repointel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotspots")
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "commit_count")
    private int commitCount;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "avg_complexity", precision = 5, scale = 2)
    private BigDecimal avgComplexity;

    @Column(name = "hotspot_score", precision = 5, scale = 4)
    private BigDecimal hotspotScore;

    public Hotspot() {}

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public String getFilePath() { return filePath; }
    public int getCommitCount() { return commitCount; }
    public LocalDateTime getLastModified() { return lastModified; }
    public BigDecimal getAvgComplexity() { return avgComplexity; }
    public BigDecimal getHotspotScore() { return hotspotScore; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String v) { this.jobId = v; }
    public void setFilePath(String v) { this.filePath = v; }
    public void setCommitCount(int v) { this.commitCount = v; }
    public void setLastModified(LocalDateTime v) { this.lastModified = v; }
    public void setAvgComplexity(BigDecimal v) { this.avgComplexity = v; }
    public void setHotspotScore(BigDecimal v) { this.hotspotScore = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Hotspot h = new Hotspot();
        public Builder jobId(String v) { h.jobId = v; return this; }
        public Builder filePath(String v) { h.filePath = v; return this; }
        public Builder commitCount(int v) { h.commitCount = v; return this; }
        public Builder lastModified(LocalDateTime v) { h.lastModified = v; return this; }
        public Builder avgComplexity(BigDecimal v) { h.avgComplexity = v; return this; }
        public Builder hotspotScore(BigDecimal v) { h.hotspotScore = v; return this; }
        public Hotspot build() { return h; }
    }
}
