package com.repointel.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyGraphDTO {

    private List<DepNodeDTO> nodes;
    private List<DepEdgeDTO> edges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepNodeDTO {
        private Long id;
        private String label;
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;
        private String nodeType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepEdgeDTO {
        private Long id;
        private Long sourceId;
        private Long targetId;
    }
}
