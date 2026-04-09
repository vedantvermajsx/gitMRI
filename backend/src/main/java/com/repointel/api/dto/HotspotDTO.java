package com.repointel.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HotspotDTO {
    private Long id;
    private String filePath;
    private String fileName;
    private int commitCount;
    private BigDecimal avgComplexity;
    private BigDecimal hotspotScore;
    private String riskLevel;
    private LocalDateTime lastModified;

    public HotspotDTO() {}

    public Long getId() { return id; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public int getCommitCount() { return commitCount; }
    public BigDecimal getAvgComplexity() { return avgComplexity; }
    public BigDecimal getHotspotScore() { return hotspotScore; }
    public String getRiskLevel() { return riskLevel; }
    public LocalDateTime getLastModified() { return lastModified; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final HotspotDTO d = new HotspotDTO();
        public Builder id(Long v) { d.id = v; return this; }
        public Builder filePath(String v) { d.filePath = v; return this; }
        public Builder fileName(String v) { d.fileName = v; return this; }
        public Builder commitCount(int v) { d.commitCount = v; return this; }
        public Builder avgComplexity(BigDecimal v) { d.avgComplexity = v; return this; }
        public Builder hotspotScore(BigDecimal v) { d.hotspotScore = v; return this; }
        public Builder riskLevel(String v) { d.riskLevel = v; return this; }
        public Builder lastModified(LocalDateTime v) { d.lastModified = v; return this; }
        public HotspotDTO build() { return d; }
    }
}
