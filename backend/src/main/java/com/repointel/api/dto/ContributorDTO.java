package com.repointel.api.dto;

import java.time.LocalDateTime;

public class ContributorDTO {
    private Long id;
    private String authorName;
    private String authorEmail;
    private int commitCount;
    private int filesOwned;
    private int linesAdded;
    private int linesRemoved;
    private double commitPercentage;
    private LocalDateTime firstCommit;
    private LocalDateTime lastCommit;

    public ContributorDTO() {}

    public Long getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public int getCommitCount() { return commitCount; }
    public int getFilesOwned() { return filesOwned; }
    public int getLinesAdded() { return linesAdded; }
    public int getLinesRemoved() { return linesRemoved; }
    public double getCommitPercentage() { return commitPercentage; }
    public LocalDateTime getFirstCommit() { return firstCommit; }
    public LocalDateTime getLastCommit() { return lastCommit; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ContributorDTO d = new ContributorDTO();
        public Builder id(Long v) { d.id = v; return this; }
        public Builder authorName(String v) { d.authorName = v; return this; }
        public Builder authorEmail(String v) { d.authorEmail = v; return this; }
        public Builder commitCount(int v) { d.commitCount = v; return this; }
        public Builder filesOwned(int v) { d.filesOwned = v; return this; }
        public Builder linesAdded(int v) { d.linesAdded = v; return this; }
        public Builder linesRemoved(int v) { d.linesRemoved = v; return this; }
        public Builder commitPercentage(double v) { d.commitPercentage = v; return this; }
        public Builder firstCommit(LocalDateTime v) { d.firstCommit = v; return this; }
        public Builder lastCommit(LocalDateTime v) { d.lastCommit = v; return this; }
        public ContributorDTO build() { return d; }
    }
}
