package com.repointel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dependency_edges")
public class DependencyEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    public DependencyEdge() {}

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public Long getSourceId() { return sourceId; }
    public Long getTargetId() { return targetId; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String v) { this.jobId = v; }
    public void setSourceId(Long v) { this.sourceId = v; }
    public void setTargetId(Long v) { this.targetId = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final DependencyEdge e = new DependencyEdge();
        public Builder jobId(String v) { e.jobId = v; return this; }
        public Builder sourceId(Long v) { e.sourceId = v; return this; }
        public Builder targetId(Long v) { e.targetId = v; return this; }
        public DependencyEdge build() { return e; }
    }
}
