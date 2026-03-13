package com.repointel.api.controller;

import com.repointel.api.dto.*;
import com.repointel.model.*;
import com.repointel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AnalysisJobRepository jobRepository;
    private final ReportRepository reportRepository;
    private final ComplexityMetricRepository complexityRepo;
    private final DeadCodeItemRepository deadCodeRepo;
    private final HotspotRepository hotspotRepo;
    private final ContributorRepository contributorRepo;
    private final DependencyNodeRepository depNodeRepo;
    private final DependencyEdgeRepository depEdgeRepo;

    /** GET /api/reports/{jobId} - Full report summary */
    @GetMapping("/{jobId}")
    public ResponseEntity<ReportDTO> getReport(@PathVariable String jobId) {
        return reportRepository.findByJobId(jobId)
                .flatMap(report -> jobRepository.findById(jobId).map(job -> {
                    ReportDTO dto = ReportDTO.builder()
                            .jobId(jobId)
                            .repoName(job.getRepoName())
                            .repoUrl(job.getRepoUrl())
                            .healthScore(report.getHealthScore())
                            .totalFiles(report.getTotalFiles())
                            .totalClasses(report.getTotalClasses())
                            .totalMethods(report.getTotalMethods())
                            .totalLines(report.getTotalLines())
                            .avgComplexity(report.getAvgComplexity())
                            .maxComplexity(report.getMaxComplexity())
                            .busFactor(report.getBusFactor())
                            .deadCodeCount(report.getDeadCodeCount())
                            .deadCodeRatio(report.getDeadCodeRatio())
                            .hotspotCount(report.getHotspotCount())
                            .totalCommits(report.getTotalCommits())
                            .contributorCount(report.getContributorCount())
                            .dependencyCount(report.getDependencyCount())
                            .analyzedAt(report.getCreatedAt())
                            .build();
                    return ResponseEntity.ok(dto);
                }))
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/reports/{jobId}/complexity - Method complexity details */
    @GetMapping("/{jobId}/complexity")
    public ResponseEntity<List<ComplexityDTO>> getComplexity(@PathVariable String jobId) {
        List<ComplexityDTO> dtos = complexityRepo.findByJobIdOrderByCcScoreDesc(jobId)
                .stream().map(m -> ComplexityDTO.builder()
                        .id(m.getId())
                        .filePath(m.getFilePath())
                        .packageName(m.getPackageName())
                        .className(m.getClassName())
                        .methodName(m.getMethodName())
                        .ccScore(m.getCcScore())
                        .riskLevel(ccRisk(m.getCcScore()))
                        .lineCount(m.getLineCount())
                        .nestingDepth(m.getNestingDepth())
                        .startLine(m.getStartLine())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** GET /api/reports/{jobId}/deadcode */
    @GetMapping("/{jobId}/deadcode")
    public ResponseEntity<List<DeadCodeDTO>> getDeadCode(@PathVariable String jobId,
                                                          @RequestParam(required = false) String risk) {
        List<DeadCodeItem> items = risk != null
                ? deadCodeRepo.findByJobIdAndRiskLevel(jobId, risk.toUpperCase())
                : deadCodeRepo.findByJobId(jobId);

        List<DeadCodeDTO> dtos = items.stream().map(d -> DeadCodeDTO.builder()
                .id(d.getId())
                .itemType(d.getItemType())
                .name(d.getName())
                .qualifiedName(d.getQualifiedName())
                .filePath(d.getFilePath())
                .lineNumber(d.getLineNumber())
                .riskLevel(d.getRiskLevel())
                .reason(d.getReason())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /** GET /api/reports/{jobId}/hotspots */
    @GetMapping("/{jobId}/hotspots")
    public ResponseEntity<List<HotspotDTO>> getHotspots(@PathVariable String jobId) {
        List<HotspotDTO> dtos = hotspotRepo.findByJobIdOrderByHotspotScoreDesc(jobId)
                .stream().map(h -> {
                    String fileName = h.getFilePath() != null
                            ? h.getFilePath().substring(h.getFilePath().lastIndexOf('/') + 1)
                            : "";
                    double score = h.getHotspotScore() != null ? h.getHotspotScore().doubleValue() : 0;
                    return HotspotDTO.builder()
                            .id(h.getId())
                            .filePath(h.getFilePath())
                            .fileName(fileName)
                            .commitCount(h.getCommitCount())
                            .avgComplexity(h.getAvgComplexity())
                            .hotspotScore(h.getHotspotScore())
                            .riskLevel(score > 0.7 ? "HIGH" : score > 0.4 ? "MEDIUM" : "LOW")
                            .lastModified(h.getLastModified())
                            .build();
                }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** GET /api/reports/{jobId}/contributors */
    @GetMapping("/{jobId}/contributors")
    public ResponseEntity<List<ContributorDTO>> getContributors(@PathVariable String jobId) {
        List<Contributor> contributors = contributorRepo.findByJobIdOrderByCommitCountDesc(jobId);
        int total = contributors.stream().mapToInt(Contributor::getCommitCount).sum();

        List<ContributorDTO> dtos = contributors.stream().map(c -> ContributorDTO.builder()
                .id(c.getId())
                .authorName(c.getAuthorName())
                .authorEmail(c.getAuthorEmail())
                .commitCount(c.getCommitCount())
                .filesOwned(c.getFilesOwned())
                .linesAdded(c.getLinesAdded())
                .linesRemoved(c.getLinesRemoved())
                .commitPercentage(total > 0 ? Math.round((double) c.getCommitCount() / total * 1000) / 10.0 : 0)
                .firstCommit(c.getFirstCommit())
                .lastCommit(c.getLastCommit())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /** GET /api/reports/{jobId}/dependencies */
    @GetMapping("/{jobId}/dependencies")
    public ResponseEntity<DependencyGraphDTO> getDependencies(@PathVariable String jobId) {
        List<DependencyNode> nodes = depNodeRepo.findByJobId(jobId);
        List<DependencyEdge> edges = depEdgeRepo.findByJobId(jobId);

        List<DependencyGraphDTO.DepNodeDTO> nodeDtos = nodes.stream()
                .map(n -> DependencyGraphDTO.DepNodeDTO.builder()
                        .id(n.getId())
                        .label(n.getArtifactId())
                        .groupId(n.getGroupId())
                        .artifactId(n.getArtifactId())
                        .version(n.getVersion())
                        .scope(n.getScope())
                        .nodeType(n.getNodeType())
                        .build()).collect(Collectors.toList());

        List<DependencyGraphDTO.DepEdgeDTO> edgeDtos = edges.stream()
                .map(e -> DependencyGraphDTO.DepEdgeDTO.builder()
                        .id(e.getId())
                        .sourceId(e.getSourceId())
                        .targetId(e.getTargetId())
                        .build()).collect(Collectors.toList());

        return ResponseEntity.ok(DependencyGraphDTO.builder()
                .nodes(nodeDtos).edges(edgeDtos).build());
    }

    private String ccRisk(int cc) {
        if (cc >= 20) return "HIGH";
        if (cc >= 12) return "COMPLEX";
        if (cc >= 7)  return "MODERATE";
        return "SIMPLE";
    }
}
