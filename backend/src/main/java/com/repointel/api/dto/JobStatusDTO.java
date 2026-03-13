package com.repointel.api.dto;

import com.repointel.model.AnalysisJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public static JobStatusDTO from(AnalysisJob job) {
        return JobStatusDTO.builder()
                .jobId(job.getId())
                .repoUrl(job.getRepoUrl())
                .repoName(job.getRepoName())
                .status(job.getStatus().name())
                .progress(job.getProgress())
                .currentStage(job.getCurrentStage())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
