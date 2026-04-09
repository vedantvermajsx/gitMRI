package com.repointel.api.dto;

import java.util.List;

public class DependencyGraphDTO {
    private List<DepNodeDTO> nodes;
    private List<DepEdgeDTO> edges;

    public DependencyGraphDTO() {}
    public DependencyGraphDTO(List<DepNodeDTO> nodes, List<DepEdgeDTO> edges) {
        this.nodes = nodes; this.edges = edges;
    }

    public List<DepNodeDTO> getNodes() { return nodes; }
    public List<DepEdgeDTO> getEdges() { return edges; }
    public void setNodes(List<DepNodeDTO> v) { this.nodes = v; }
    public void setEdges(List<DepEdgeDTO> v) { this.edges = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final DependencyGraphDTO d = new DependencyGraphDTO();
        public Builder nodes(List<DepNodeDTO> v) { d.nodes = v; return this; }
        public Builder edges(List<DepEdgeDTO> v) { d.edges = v; return this; }
        public DependencyGraphDTO build() { return d; }
    }

    public static class DepNodeDTO {
        private Long id; private String label; private String groupId;
        private String artifactId; private String version; private String scope; private String nodeType;

        public DepNodeDTO() {}
        public Long getId() { return id; }
        public String getLabel() { return label; }
        public String getGroupId() { return groupId; }
        public String getArtifactId() { return artifactId; }
        public String getVersion() { return version; }
        public String getScope() { return scope; }
        public String getNodeType() { return nodeType; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final DepNodeDTO n = new DepNodeDTO();
            public Builder id(Long v) { n.id = v; return this; }
            public Builder label(String v) { n.label = v; return this; }
            public Builder groupId(String v) { n.groupId = v; return this; }
            public Builder artifactId(String v) { n.artifactId = v; return this; }
            public Builder version(String v) { n.version = v; return this; }
            public Builder scope(String v) { n.scope = v; return this; }
            public Builder nodeType(String v) { n.nodeType = v; return this; }
            public DepNodeDTO build() { return n; }
        }
    }

    public static class DepEdgeDTO {
        private Long id; private Long sourceId; private Long targetId;

        public DepEdgeDTO() {}
        public Long getId() { return id; }
        public Long getSourceId() { return sourceId; }
        public Long getTargetId() { return targetId; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final DepEdgeDTO e = new DepEdgeDTO();
            public Builder id(Long v) { e.id = v; return this; }
            public Builder sourceId(Long v) { e.sourceId = v; return this; }
            public Builder targetId(Long v) { e.targetId = v; return this; }
            public DepEdgeDTO build() { return e; }
        }
    }
}
