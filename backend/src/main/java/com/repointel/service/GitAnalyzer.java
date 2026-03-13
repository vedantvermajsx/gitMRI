package com.repointel.service;

import com.repointel.model.Contributor;
import com.repointel.model.Hotspot;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes git commit history to compute:
 * - File churn (hotspots)
 * - Contributor statistics
 * - Bus factor
 */
@Service
@Slf4j
public class GitAnalyzer {

    public static class GitAnalysisResult {
        public List<Hotspot> hotspots;
        public List<Contributor> contributors;
        public int busFactor;
        public int totalCommits;

        public GitAnalysisResult(List<Hotspot> hotspots, List<Contributor> contributors,
                                  int busFactor, int totalCommits) {
            this.hotspots = hotspots;
            this.contributors = contributors;
            this.busFactor = busFactor;
            this.totalCommits = totalCommits;
        }
    }

    public GitAnalysisResult analyze(File repoDir, String jobId,
                                      Map<String, Double> avgComplexityByFile) {
        Map<String, Integer> fileCommitCount = new HashMap<>();
        Map<String, LocalDateTime> fileLastModified = new HashMap<>();
        Map<String, AuthorStats> authorMap = new HashMap<>();
        int totalCommits = 0;

        try (Git git = Git.open(repoDir)) {
            Repository repo = git.getRepository();
            Iterable<RevCommit> commits = git.log().call();

            for (RevCommit commit : commits) {
                totalCommits++;
                PersonIdent author = commit.getAuthorIdent();
                String authorKey = author.getEmailAddress();
                LocalDateTime commitTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(commit.getCommitTime()), ZoneId.systemDefault());

                // Track author stats
                AuthorStats stats = authorMap.computeIfAbsent(authorKey,
                        k -> new AuthorStats(author.getName(), author.getEmailAddress()));
                stats.commitCount++;
                stats.lastCommit = stats.lastCommit == null || commitTime.isAfter(stats.lastCommit)
                        ? commitTime : stats.lastCommit;
                if (stats.firstCommit == null || commitTime.isBefore(stats.firstCommit)) {
                    stats.firstCommit = commitTime;
                }

                // Get changed files for this commit
                try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    df.setRepository(repo);
                    if (commit.getParentCount() > 0) {
                        RevCommit parent = commit.getParent(0);
                        repo.resolve(parent.getName());
                        List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                        for (DiffEntry diff : diffs) {
                            String path = diff.getNewPath().equals("/dev/null")
                                    ? diff.getOldPath() : diff.getNewPath();
                            if (path.endsWith(".java")) {
                                fileCommitCount.merge(path, 1, Integer::sum);
                                fileLastModified.merge(path, commitTime,
                                        (existing, newTime) -> newTime.isAfter(existing) ? newTime : existing);
                                stats.filesOwned.add(path);

                                // Line stats
                                try {
                                    df.toFileHeader(diff).toEditList().forEach(edit -> {
                                        stats.linesAdded += edit.getLengthB();
                                        stats.linesRemoved += edit.getLengthA();
                                    });
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }

            log.info("Git analysis: {} commits, {} files changed, {} authors",
                    totalCommits, fileCommitCount.size(), authorMap.size());

        } catch (Exception e) {
            log.error("Git analysis failed: {}", e.getMessage(), e);
        }

        // Build hotspots
        List<Hotspot> hotspots = buildHotspots(fileCommitCount, fileLastModified,
                avgComplexityByFile, jobId);

        // Build contributors
        List<Contributor> contributors = buildContributors(authorMap, jobId);

        // Compute bus factor
        int busFactor = computeBusFactor(contributors, totalCommits);

        return new GitAnalysisResult(hotspots, contributors, busFactor, totalCommits);
    }

    private List<Hotspot> buildHotspots(Map<String, Integer> fileCommitCount,
                                          Map<String, LocalDateTime> fileLastModified,
                                          Map<String, Double> avgComplexityByFile,
                                          String jobId) {
        if (fileCommitCount.isEmpty()) return Collections.emptyList();

        int maxCommits = fileCommitCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double maxComplexity = avgComplexityByFile.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);

        return fileCommitCount.entrySet().stream()
                .map(entry -> {
                    String file = entry.getKey();
                    int commits = entry.getValue();
                    double avgCC = avgComplexityByFile.getOrDefault(file, 1.0);

                    double normalizedChurn = (double) commits / maxCommits;
                    double normalizedCC = maxComplexity > 0 ? avgCC / maxComplexity : 0;
                    double score = normalizedChurn * normalizedCC;

                    return Hotspot.builder()
                            .jobId(jobId)
                            .filePath(file)
                            .commitCount(commits)
                            .lastModified(fileLastModified.get(file))
                            .avgComplexity(BigDecimal.valueOf(avgCC).setScale(2, RoundingMode.HALF_UP))
                            .hotspotScore(BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP))
                            .build();
                })
                .sorted(Comparator.comparing(Hotspot::getHotspotScore).reversed())
                .collect(Collectors.toList());
    }

    private List<Contributor> buildContributors(Map<String, AuthorStats> authorMap, String jobId) {
        return authorMap.values().stream()
                .map(stats -> Contributor.builder()
                        .jobId(jobId)
                        .authorName(stats.name)
                        .authorEmail(stats.email)
                        .commitCount(stats.commitCount)
                        .filesOwned(stats.filesOwned.size())
                        .linesAdded(stats.linesAdded)
                        .linesRemoved(stats.linesRemoved)
                        .firstCommit(stats.firstCommit)
                        .lastCommit(stats.lastCommit)
                        .build())
                .sorted(Comparator.comparingInt(Contributor::getCommitCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Bus factor = minimum number of contributors whose combined commits >= 50% of total.
     */
    private int computeBusFactor(List<Contributor> contributors, int totalCommits) {
        if (contributors.isEmpty() || totalCommits == 0) return 1;
        int cumulative = 0;
        int factor = 0;
        for (Contributor c : contributors) {
            cumulative += c.getCommitCount();
            factor++;
            if ((double) cumulative / totalCommits >= 0.5) break;
        }
        return factor;
    }

    private static class AuthorStats {
        String name;
        String email;
        int commitCount = 0;
        Set<String> filesOwned = new HashSet<>();
        int linesAdded = 0;
        int linesRemoved = 0;
        LocalDateTime firstCommit;
        LocalDateTime lastCommit;

        AuthorStats(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
