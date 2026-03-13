package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
