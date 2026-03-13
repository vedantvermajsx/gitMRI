package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "contributors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
