package com.repointel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dead_code_items")
public class DeadCodeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "item_type", length = 20)
    private String itemType;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "qualified_name", length = 1000)
    private String qualifiedName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "line_number")
    private int lineNumber;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    public DeadCodeItem() {}

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public String getItemType() { return itemType; }
    public String getName() { return name; }
    public String getQualifiedName() { return qualifiedName; }
    public String getFilePath() { return filePath; }
    public int getLineNumber() { return lineNumber; }
    public String getRiskLevel() { return riskLevel; }
    public String getReason() { return reason; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String v) { this.jobId = v; }
    public void setItemType(String v) { this.itemType = v; }
    public void setName(String v) { this.name = v; }
    public void setQualifiedName(String v) { this.qualifiedName = v; }
    public void setFilePath(String v) { this.filePath = v; }
    public void setLineNumber(int v) { this.lineNumber = v; }
    public void setRiskLevel(String v) { this.riskLevel = v; }
    public void setReason(String v) { this.reason = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final DeadCodeItem d = new DeadCodeItem();
        public Builder jobId(String v) { d.jobId = v; return this; }
        public Builder itemType(String v) { d.itemType = v; return this; }
        public Builder name(String v) { d.name = v; return this; }
        public Builder qualifiedName(String v) { d.qualifiedName = v; return this; }
        public Builder filePath(String v) { d.filePath = v; return this; }
        public Builder lineNumber(int v) { d.lineNumber = v; return this; }
        public Builder riskLevel(String v) { d.riskLevel = v; return this; }
        public Builder reason(String v) { d.reason = v; return this; }
        public DeadCodeItem build() { return d; }
    }
}
