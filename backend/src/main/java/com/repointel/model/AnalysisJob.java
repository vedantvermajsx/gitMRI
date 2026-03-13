package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisJob {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "repo_url", nullable = false, columnDefinition = "TEXT")
    private String repoUrl;

    @Column(name = "repo_name")
    private String repoName;

    @Column(name = "branch")
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "progress")
    private int progress;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum JobStatus {
        PENDING, CLONING, PARSING, ANALYZING, HOTSPOTS, DEPENDENCIES, REPORTING, DONE, FAILED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = JobStatus.PENDING;
        if (branch == null) branch = "main";
    }
}
