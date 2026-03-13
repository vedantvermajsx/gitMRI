package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "dependency_nodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String scope; // compile, test, runtime, provided

    @Column(name = "node_type", length = 20)
    private String nodeType; // ROOT, EXTERNAL, INTERNAL
}
