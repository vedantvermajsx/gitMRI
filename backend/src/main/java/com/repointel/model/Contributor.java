package com.repointel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contributors")
public class Contributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "author_email", length = 255)
    private String authorEmail;

    @Column(name = "commit_count")
    private int commitCount;

    @Column(name = "files_owned")
    private int filesOwned;

    @Column(name = "lines_added")
    private int linesAdded;

    @Column(name = "lines_removed")
    private int linesRemoved;

    @Column(name = "first_commit")
    private LocalDateTime firstCommit;

    @Column(name = "last_commit")
    private LocalDateTime lastCommit;

    public Contributor() {}

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public int getCommitCount() { return commitCount; }
    public int getFilesOwned() { return filesOwned; }
    public int getLinesAdded() { return linesAdded; }
    public int getLinesRemoved() { return linesRemoved; }
    public LocalDateTime getFirstCommit() { return firstCommit; }
    public LocalDateTime getLastCommit() { return lastCommit; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String v) { this.jobId = v; }
    public void setAuthorName(String v) { this.authorName = v; }
    public void setAuthorEmail(String v) { this.authorEmail = v; }
    public void setCommitCount(int v) { this.commitCount = v; }
    public void setFilesOwned(int v) { this.filesOwned = v; }
    public void setLinesAdded(int v) { this.linesAdded = v; }
    public void setLinesRemoved(int v) { this.linesRemoved = v; }
    public void setFirstCommit(LocalDateTime v) { this.firstCommit = v; }
    public void setLastCommit(LocalDateTime v) { this.lastCommit = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Contributor c = new Contributor();
        public Builder jobId(String v) { c.jobId = v; return this; }
        public Builder authorName(String v) { c.authorName = v; return this; }
        public Builder authorEmail(String v) { c.authorEmail = v; return this; }
        public Builder commitCount(int v) { c.commitCount = v; return this; }
        public Builder filesOwned(int v) { c.filesOwned = v; return this; }
        public Builder linesAdded(int v) { c.linesAdded = v; return this; }
        public Builder linesRemoved(int v) { c.linesRemoved = v; return this; }
        public Builder firstCommit(LocalDateTime v) { c.firstCommit = v; return this; }
        public Builder lastCommit(LocalDateTime v) { c.lastCommit = v; return this; }
        public Contributor build() { return c; }
    }
}
