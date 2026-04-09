package com.repointel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_jobs")
public class AnalysisJob {

    public enum JobStatus {
        PENDING, CLONING, PARSING, ANALYZING, HOTSPOTS, DEPENDENCIES, REPORTING, DONE, FAILED
    }

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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = JobStatus.PENDING;
        if (branch == null) branch = "main";
    }

    public AnalysisJob() {}

    // Getters
    public String getId() { return id; }
    public String getRepoUrl() { return repoUrl; }
    public String getRepoName() { return repoName; }
    public String getBranch() { return branch; }
    public JobStatus getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getCurrentStage() { return currentStage; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setStatus(JobStatus status) { this.status = status; }
    public void setProgress(int progress) { this.progress = progress; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AnalysisJob job = new AnalysisJob();
        public Builder id(String v) { job.id = v; return this; }
        public Builder repoUrl(String v) { job.repoUrl = v; return this; }
        public Builder repoName(String v) { job.repoName = v; return this; }
        public Builder branch(String v) { job.branch = v; return this; }
        public Builder status(JobStatus v) { job.status = v; return this; }
        public Builder progress(int v) { job.progress = v; return this; }
        public Builder currentStage(String v) { job.currentStage = v; return this; }
        public Builder errorMessage(String v) { job.errorMessage = v; return this; }
        public AnalysisJob build() { return job; }
    }
}
