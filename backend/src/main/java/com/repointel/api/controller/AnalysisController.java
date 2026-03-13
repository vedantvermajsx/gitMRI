package com.repointel.api.controller;

import com.repointel.api.dto.JobStatusDTO;
import com.repointel.model.AnalysisJob;
import com.repointel.repository.AnalysisJobRepository;
import com.repointel.service.AnalysisOrchestrator;
import com.repointel.service.RepoCloner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final AnalysisJobRepository jobRepository;
    private final AnalysisOrchestrator orchestrator;
    private final RepoCloner repoCloner;

    /**
     * POST /api/analyze
     * Start a new analysis job for the given repository URL.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> startAnalysis(@RequestBody Map<String, String> body) {
        String repoUrl = body.get("repoUrl");
        if (repoUrl == null || repoUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "repoUrl is required"));
        }

        // Normalize URL
        repoUrl = repoUrl.trim();
        if (!repoUrl.startsWith("http")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only HTTPS URLs are supported"));
        }

        String jobId = UUID.randomUUID().toString();
        String repoName = repoCloner.extractRepoName(repoUrl);

        AnalysisJob job = AnalysisJob.builder()
                .id(jobId)
                .repoUrl(repoUrl)
                .repoName(repoName)
                .status(AnalysisJob.JobStatus.PENDING)
                .progress(0)
                .currentStage("Queued")
                .build();

        jobRepository.save(job);
        orchestrator.runAnalysis(jobId, repoUrl);

        log.info("Started analysis job {} for {}", jobId, repoUrl);
        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "repoName", repoName,
                "status", "PENDING"
        ));
    }

    /**
     * GET /api/jobs/{jobId}/status
     * Poll the current status and progress of a job.
     */
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        return jobRepository.findById(jobId)
                .map(job -> ResponseEntity.ok(JobStatusDTO.from(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/jobs
     * List all analysis jobs.
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<JobStatusDTO>> listJobs() {
        List<JobStatusDTO> jobs = jobRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(JobStatusDTO::from).collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    /**
     * DELETE /api/jobs/{jobId}
     * Delete a job and all associated data.
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<?> deleteJob(@PathVariable String jobId) {
        if (!jobRepository.existsById(jobId)) {
            return ResponseEntity.notFound().build();
        }
        jobRepository.deleteById(jobId);
        return ResponseEntity.noContent().build();
    }
}
