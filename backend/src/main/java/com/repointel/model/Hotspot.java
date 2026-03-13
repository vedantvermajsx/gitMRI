package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotspots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
