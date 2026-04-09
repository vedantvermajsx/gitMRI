package com.repointel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dependency_nodes")
public class DependencyNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "group_id", length = 255)
    private String groupId;

    @Column(name = "artifact_id", length = 255)
    private String artifactId;

    @Column(name = "version", length = 100)
    private String version;

    @Column(name = "scope", length = 20)
    private String scope;

    @Column(name = "node_type", length = 20)
    private String nodeType;

    public DependencyNode() {}

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public String getGroupId() { return groupId; }
    public String getArtifactId() { return artifactId; }
    public String getVersion() { return version; }
    public String getScope() { return scope; }
    public String getNodeType() { return nodeType; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String v) { this.jobId = v; }
    public void setGroupId(String v) { this.groupId = v; }
    public void setArtifactId(String v) { this.artifactId = v; }
    public void setVersion(String v) { this.version = v; }
    public void setScope(String v) { this.scope = v; }
    public void setNodeType(String v) { this.nodeType = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final DependencyNode n = new DependencyNode();
        public Builder jobId(String v) { n.jobId = v; return this; }
        public Builder groupId(String v) { n.groupId = v; return this; }
        public Builder artifactId(String v) { n.artifactId = v; return this; }
        public Builder version(String v) { n.version = v; return this; }
        public Builder scope(String v) { n.scope = v; return this; }
        public Builder nodeType(String v) { n.nodeType = v; return this; }
        public DependencyNode build() { return n; }
    }
}
