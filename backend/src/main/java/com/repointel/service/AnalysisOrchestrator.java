package com.repointel.service;

import com.repointel.model.*;
import com.repointel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coordinates the full 6-stage analysis pipeline:
 *   1. CLONING    - Clone the repository via JGit
 *   2. PARSING    - Parse all Java files with JavaParser
 *   3. ANALYZING  - Dead code detection
 *   4. HOTSPOTS   - Git history analysis (churn + bus factor)
 *   5. DEPENDENCIES - Dependency graph extraction
 *   6. REPORTING  - Aggregate report + health score
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisOrchestrator {

    private final AnalysisJobRepository jobRepository;
    private final ReportRepository reportRepository;
    private final ComplexityMetricRepository complexityRepo;
    private final DeadCodeItemRepository deadCodeRepo;
    private final HotspotRepository hotspotRepo;
    private final ContributorRepository contributorRepo;
    private final DependencyNodeRepository depNodeRepo;
    private final DependencyEdgeRepository depEdgeRepo;

    private final RepoCloner repoCloner;
    private final CodeParser codeParser;
    private final MetricsEngine metricsEngine;
    private final GitAnalyzer gitAnalyzer;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final ReportGenerator reportGenerator;

    @Async("analysisExecutor")
    public void runAnalysis(String jobId, String repoUrl) {
        File repoDir = null;

        try {
            // ── Stage 1: Clone ────────────────────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.CLONING, 10, "Cloning repository...");
            repoDir = repoCloner.cloneRepository(repoUrl, jobId);

            // ── Stage 2: Parse Java source ────────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.PARSING, 25, "Parsing Java source files...");
            List<ComplexityMetric> complexityMetrics = codeParser.parseRepository(repoDir, jobId);
            int totalFiles = codeParser.countJavaFiles(repoDir);
            long totalLines = codeParser.countTotalLines(repoDir);

            // Batch save complexity metrics
            complexityRepo.saveAll(complexityMetrics);

            // ── Stage 3: Dead code detection ──────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.ANALYZING, 45, "Detecting dead code...");
            List<DeadCodeItem> deadCodeItems = metricsEngine.detectDeadCode(repoDir, jobId);
            deadCodeRepo.saveAll(deadCodeItems);

            // ── Stage 4: Git history analysis ─────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.HOTSPOTS, 62, "Analyzing git history...");

            // Build avgCC map: filePath → average cyclomatic complexity
            Map<String, List<Integer>> fileScores = new HashMap<>();
            for (ComplexityMetric m : complexityMetrics) {
                if (m.getFilePath() != null) {
                    fileScores.computeIfAbsent(m.getFilePath(), k -> new ArrayList<>())
                              .add(m.getCcScore());
                }
            }
            Map<String, Double> avgCCByFile = new HashMap<>();
            for (Map.Entry<String, List<Integer>> e : fileScores.entrySet()) {
                double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(1.0);
                avgCCByFile.put(e.getKey(), avg);
            }

            GitAnalyzer.GitAnalysisResult gitResult =
                    gitAnalyzer.analyze(repoDir, jobId, avgCCByFile);
            hotspotRepo.saveAll(gitResult.hotspots);
            contributorRepo.saveAll(gitResult.contributors);

            // ── Stage 5: Dependency analysis ──────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.DEPENDENCIES, 78, "Analyzing dependencies...");
            DependencyAnalyzer.DependencyResult depResult =
                    dependencyAnalyzer.analyze(repoDir, jobId);
            List<DependencyNode> savedNodes = depNodeRepo.saveAll(depResult.nodes);

            // Auto-create edges: root → all deps
            if (!savedNodes.isEmpty()) {
                DependencyNode root = savedNodes.stream()
                        .filter(n -> "ROOT".equals(n.getNodeType()))
                        .findFirst()
                        .orElse(savedNodes.get(0));

                List<DependencyEdge> edges = savedNodes.stream()
                        .filter(n -> !"ROOT".equals(n.getNodeType()))
                        .map(n -> DependencyEdge.builder()
                                .jobId(jobId)
                                .sourceId(root.getId())
                                .targetId(n.getId())
                                .build())
                        .collect(Collectors.toList());
                depEdgeRepo.saveAll(edges);
            }

            // ── Stage 6: Generate report ───────────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.REPORTING, 90, "Generating report...");
            Report report = reportGenerator.generateReport(
                    jobId,
                    complexityMetrics,
                    deadCodeItems,
                    gitResult.hotspots,
                    gitResult.contributors,
                    savedNodes,
                    gitResult.busFactor,
                    gitResult.totalCommits,
                    totalFiles,
                    totalLines);

            reportRepository.save(report);

            // ── Done ───────────────────────────────────────────────────────────
            updateJob(jobId, AnalysisJob.JobStatus.DONE, 100, "Analysis complete");
            markCompleted(jobId);
            log.info("Analysis complete for job {}", jobId);

        } catch (Exception e) {
            log.error("Analysis failed for job {}: {}", jobId, e.getMessage(), e);
            markFailed(jobId, e.getMessage());
        } finally {
            // Clean up cloned repo to free disk space
            if (repoDir != null) {
                repoCloner.cleanup(jobId);
            }
        }
    }

    private void updateJob(String jobId, AnalysisJob.JobStatus status, int progress, String stage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status);
            job.setProgress(progress);
            job.setCurrentStage(stage);
            if (status == AnalysisJob.JobStatus.CLONING && job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }
            jobRepository.save(job);
        });
        log.info("Job {} → {} ({}%): {}", jobId, status, progress, stage);
    }

    private void markCompleted(String jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        });
    }

    private void markFailed(String jobId, String error) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(AnalysisJob.JobStatus.FAILED);
            job.setErrorMessage(error != null ? error.substring(0, Math.min(error.length(), 500)) : "Unknown error");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        });
    }
}
