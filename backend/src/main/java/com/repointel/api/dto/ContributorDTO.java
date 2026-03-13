package com.repointel.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributorDTO {
    private Long id;
    private String authorName;
    private String authorEmail;
    private int commitCount;
    private int filesOwned;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public int getFilesOwned() {
        return filesOwned;
    }

    public void setFilesOwned(int filesOwned) {
        this.filesOwned = filesOwned;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(int linesRemoved) {
        this.linesRemoved = linesRemoved;
    }

    public double getCommitPercentage() {
        return commitPercentage;
    }

    public void setCommitPercentage(double commitPercentage) {
        this.commitPercentage = commitPercentage;
    }

    public LocalDateTime getFirstCommit() {
        return firstCommit;
    }

    public void setFirstCommit(LocalDateTime firstCommit) {
        this.firstCommit = firstCommit;
    }

    public LocalDateTime getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(LocalDateTime lastCommit) {
        this.lastCommit = lastCommit;
    }

    public ContributorDTO(String authorName, Long id, String authorEmail, int commitCount, int filesOwned, int linesAdded, int linesRemoved, double commitPercentage, LocalDateTime firstCommit, LocalDateTime lastCommit) {
        this.authorName = authorName;
        this.id = id;
        this.authorEmail = authorEmail;
        this.commitCount = commitCount;
        this.filesOwned = filesOwned;
        this.linesAdded = linesAdded;
        this.linesRemoved = linesRemoved;
        this.commitPercentage = commitPercentage;
        this.firstCommit = firstCommit;
        this.lastCommit = lastCommit;
    }

    private int linesAdded;
    private int linesRemoved;
    private double commitPercentage;
    private LocalDateTime firstCommit;
    private LocalDateTime lastCommit;
}
