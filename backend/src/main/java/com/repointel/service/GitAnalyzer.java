package com.repointel.service;

import com.repointel.model.Contributor;
import com.repointel.model.Hotspot;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GitAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(GitAnalyzer.class);

    public static class GitAnalysisResult {
        public final List<Hotspot> hotspots;
        public final List<Contributor> contributors;
        public final int busFactor;
        public final int totalCommits;

        public GitAnalysisResult(List<Hotspot> hotspots, List<Contributor> contributors, int busFactor, int totalCommits) {
            this.hotspots = hotspots;
            this.contributors = contributors;
            this.busFactor = busFactor;
            this.totalCommits = totalCommits;
        }
    }

    public GitAnalysisResult analyze(File repoDir, String jobId, Map<String, Double> avgComplexityByFile) {
        Map<String, Integer> fileCommitCount = new HashMap<>();
        Map<String, LocalDateTime> fileLastModified = new HashMap<>();
        Map<String, AuthorStats> authorMap = new HashMap<>();
        int totalCommits = 0;

        try (Git git = Git.open(repoDir)) {
            Repository repo = git.getRepository();
            for (RevCommit commit : git.log().call()) {
                totalCommits++;
                PersonIdent author = commit.getAuthorIdent();
                String authorKey = author.getEmailAddress();
                LocalDateTime commitTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(commit.getCommitTime()), ZoneId.systemDefault());

                AuthorStats stats = authorMap.computeIfAbsent(authorKey,
                        k -> new AuthorStats(author.getName(), author.getEmailAddress()));
                stats.commitCount++;
                if (stats.lastCommit == null || commitTime.isAfter(stats.lastCommit)) stats.lastCommit = commitTime;
                if (stats.firstCommit == null || commitTime.isBefore(stats.firstCommit)) stats.firstCommit = commitTime;

                try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    df.setRepository(repo);
                    if (commit.getParentCount() > 0) {
                        RevCommit parent = commit.getParent(0);
                        List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                        for (DiffEntry diff : diffs) {
                            String path = diff.getNewPath().equals("/dev/null") ? diff.getOldPath() : diff.getNewPath();
                            if (path.endsWith(".java")) {
                                fileCommitCount.merge(path, 1, Integer::sum);
                                fileLastModified.merge(path, commitTime, (ex, nw) -> nw.isAfter(ex) ? nw : ex);
                                stats.filesOwned.add(path);
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
            log.info("Git analysis: {} commits, {} files, {} authors", totalCommits, fileCommitCount.size(), authorMap.size());
        } catch (Exception e) {
            log.error("Git analysis failed: {}", e.getMessage(), e);
        }

        List<Hotspot> hotspots = buildHotspots(fileCommitCount, fileLastModified, avgComplexityByFile, jobId);
        List<Contributor> contributors = buildContributors(authorMap, jobId);
        int busFactor = computeBusFactor(contributors, totalCommits);
        return new GitAnalysisResult(hotspots, contributors, busFactor, totalCommits);
    }

    private List<Hotspot> buildHotspots(Map<String, Integer> fileCommitCount,
                                         Map<String, LocalDateTime> fileLastModified,
                                         Map<String, Double> avgComplexityByFile, String jobId) {
        if (fileCommitCount.isEmpty()) return Collections.emptyList();
        int maxCommits = fileCommitCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double maxCC = avgComplexityByFile.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        return fileCommitCount.entrySet().stream().map(entry -> {
            String file = entry.getKey();
            int commits = entry.getValue();
            double avgCC = avgComplexityByFile.getOrDefault(file, 1.0);
            double score = ((double) commits / maxCommits) * (maxCC > 0 ? avgCC / maxCC : 0);
            return Hotspot.builder()
                    .jobId(jobId).filePath(file).commitCount(commits)
                    .lastModified(fileLastModified.get(file))
                    .avgComplexity(BigDecimal.valueOf(avgCC).setScale(2, RoundingMode.HALF_UP))
                    .hotspotScore(BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP))
                    .build();
        }).sorted(Comparator.comparing(h -> h.getHotspotScore() != null ? h.getHotspotScore().doubleValue() : 0,
                Comparator.reverseOrder())).collect(Collectors.toList());
    }

    private List<Contributor> buildContributors(Map<String, AuthorStats> authorMap, String jobId) {
        return authorMap.values().stream().map(s -> Contributor.builder()
                .jobId(jobId).authorName(s.name).authorEmail(s.email)
                .commitCount(s.commitCount).filesOwned(s.filesOwned.size())
                .linesAdded(s.linesAdded).linesRemoved(s.linesRemoved)
                .firstCommit(s.firstCommit).lastCommit(s.lastCommit)
                .build())
                .sorted(Comparator.comparingInt(Contributor::getCommitCount).reversed())
                .collect(Collectors.toList());
    }

    private int computeBusFactor(List<Contributor> contributors, int totalCommits) {
        if (contributors.isEmpty() || totalCommits == 0) return 1;
        int cumulative = 0, factor = 0;
        for (Contributor c : contributors) {
            cumulative += c.getCommitCount();
            factor++;
            if ((double) cumulative / totalCommits >= 0.5) break;
        }
        return factor;
    }

    private static class AuthorStats {
        String name, email;
        int commitCount = 0, linesAdded = 0, linesRemoved = 0;
        Set<String> filesOwned = new HashSet<>();
        LocalDateTime firstCommit, lastCommit;
        AuthorStats(String name, String email) { this.name = name; this.email = email; }
    }
}
