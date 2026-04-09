package com.repointel.service;

import com.repointel.model.*;
import com.repointel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);

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

    public AnalysisOrchestrator(AnalysisJobRepository jobRepository, ReportRepository reportRepository,
                                 ComplexityMetricRepository complexityRepo, DeadCodeItemRepository deadCodeRepo,
                                 HotspotRepository hotspotRepo, ContributorRepository contributorRepo,
                                 DependencyNodeRepository depNodeRepo, DependencyEdgeRepository depEdgeRepo,
                                 RepoCloner repoCloner, CodeParser codeParser, MetricsEngine metricsEngine,
                                 GitAnalyzer gitAnalyzer, DependencyAnalyzer dependencyAnalyzer,
                                 ReportGenerator reportGenerator) {
        this.jobRepository = jobRepository;
        this.reportRepository = reportRepository;
        this.complexityRepo = complexityRepo;
        this.deadCodeRepo = deadCodeRepo;
        this.hotspotRepo = hotspotRepo;
        this.contributorRepo = contributorRepo;
        this.depNodeRepo = depNodeRepo;
        this.depEdgeRepo = depEdgeRepo;
        this.repoCloner = repoCloner;
        this.codeParser = codeParser;
        this.metricsEngine = metricsEngine;
        this.gitAnalyzer = gitAnalyzer;
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.reportGenerator = reportGenerator;
    }

    @Async("analysisExecutor")
    public void runAnalysis(String jobId, String repoUrl) {
        File repoDir = null;
        try {
            updateJob(jobId, AnalysisJob.JobStatus.CLONING, 10, "Cloning repository...");
            repoDir = repoCloner.cloneRepository(repoUrl, jobId);

            updateJob(jobId, AnalysisJob.JobStatus.PARSING, 25, "Parsing source files...");
            List<ComplexityMetric> complexityMetrics = codeParser.parseRepository(repoDir, jobId);
            int totalFiles = codeParser.countTotalSourceFiles(repoDir);
            long totalLines = codeParser.countTotalLines(repoDir);
            complexityRepo.saveAll(complexityMetrics);

            updateJob(jobId, AnalysisJob.JobStatus.ANALYZING, 45, "Detecting dead code...");
            List<DeadCodeItem> deadCodeItems = metricsEngine.detectDeadCode(repoDir, jobId);
            deadCodeRepo.saveAll(deadCodeItems);

            updateJob(jobId, AnalysisJob.JobStatus.HOTSPOTS, 62, "Analyzing git history...");
            Map<String, List<Integer>> fileScores = new HashMap<>();
            for (ComplexityMetric m : complexityMetrics) {
                if (m.getFilePath() != null) {
                    fileScores.computeIfAbsent(m.getFilePath(), k -> new ArrayList<>()).add(m.getCcScore());
                }
            }
            Map<String, Double> avgCCByFile = new HashMap<>();
            for (Map.Entry<String, List<Integer>> e : fileScores.entrySet()) {
                avgCCByFile.put(e.getKey(), e.getValue().stream().mapToInt(Integer::intValue).average().orElse(1.0));
            }

            GitAnalyzer.GitAnalysisResult gitResult = gitAnalyzer.analyze(repoDir, jobId, avgCCByFile);
            hotspotRepo.saveAll(gitResult.hotspots);
            contributorRepo.saveAll(gitResult.contributors);

            updateJob(jobId, AnalysisJob.JobStatus.DEPENDENCIES, 78, "Analyzing dependencies...");
            DependencyAnalyzer.DependencyResult depResult = dependencyAnalyzer.analyze(repoDir, jobId);
            List<DependencyNode> savedNodes = depNodeRepo.saveAll(depResult.nodes);

            if (!savedNodes.isEmpty()) {
                DependencyNode root = null;
                for (DependencyNode n : savedNodes) {
                    if ("ROOT".equals(n.getNodeType())) { root = n; break; }
                }
                if (root == null) root = savedNodes.get(0);

                List<DependencyEdge> edges = new ArrayList<>();
                for (DependencyNode n : savedNodes) {
                    if (!"ROOT".equals(n.getNodeType())) {
                        DependencyEdge edge = new DependencyEdge();
                        edge.setJobId(jobId);
                        edge.setSourceId(root.getId());
                        edge.setTargetId(n.getId());
                        edges.add(edge);
                    }
                }
                depEdgeRepo.saveAll(edges);
            }

            updateJob(jobId, AnalysisJob.JobStatus.REPORTING, 90, "Generating report...");
            Report report = reportGenerator.generateReport(jobId, complexityMetrics, deadCodeItems,
                    gitResult.hotspots, gitResult.contributors, savedNodes,
                    gitResult.busFactor, gitResult.totalCommits, totalFiles, totalLines);
            reportRepository.save(report);

            updateJob(jobId, AnalysisJob.JobStatus.DONE, 100, "Analysis complete");
            markCompleted(jobId);
            log.info("Analysis complete for job {}", jobId);

        } catch (Exception e) {
            log.error("Analysis failed for job {}: {}", jobId, e.getMessage(), e);
            markFailed(jobId, e.getMessage());
        } finally {
            if (repoDir != null) repoCloner.cleanup(jobId);
        }
    }

    private void updateJob(String jobId, AnalysisJob.JobStatus status, int progress, String stage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status);
            job.setProgress(progress);
            job.setCurrentStage(stage);
            if (status == AnalysisJob.JobStatus.CLONING && job.getStartedAt() == null)
                job.setStartedAt(LocalDateTime.now());
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
