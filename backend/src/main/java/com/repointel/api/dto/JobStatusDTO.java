package com.repointel.api.dto;

import com.repointel.model.AnalysisJob;
import java.time.LocalDateTime;

public class JobStatusDTO {
    private String jobId;
    private String repoUrl;
    private String repoName;
    private String status;
    private int progress;
    private String currentStage;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public JobStatusDTO() {}

    public static JobStatusDTO from(AnalysisJob job) {
        JobStatusDTO d = new JobStatusDTO();
        d.jobId = job.getId();
        d.repoUrl = job.getRepoUrl();
        d.repoName = job.getRepoName();
        d.status = job.getStatus().name();
        d.progress = job.getProgress();
        d.currentStage = job.getCurrentStage();
        d.errorMessage = job.getErrorMessage();
        d.createdAt = job.getCreatedAt();
        d.startedAt = job.getStartedAt();
        d.completedAt = job.getCompletedAt();
        return d;
    }

    public String getJobId() { return jobId; }
    public String getRepoUrl() { return repoUrl; }
    public String getRepoName() { return repoName; }
    public String getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getCurrentStage() { return currentStage; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
