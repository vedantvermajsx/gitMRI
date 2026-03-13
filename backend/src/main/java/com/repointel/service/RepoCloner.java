package com.repointel.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Clones public GitHub repositories to local disk using JGit.
 */
@Service
@Slf4j
public class RepoCloner {

    @Value("${repointel.clone.base-dir:/tmp/repointel/repos}")
    private String baseDir;

    /**
     * Clone a public repository by URL into a job-specific directory.
     *
     * @param repoUrl the public HTTPS GitHub URL
     * @param jobId   unique job identifier for directory isolation
     * @return File pointing to the cloned repository root
     */
    public File cloneRepository(String repoUrl, String jobId) throws GitAPIException, IOException {
        Path targetPath = Paths.get(baseDir, jobId);
        Files.createDirectories(targetPath);

        log.info("Cloning {} into {}", repoUrl, targetPath);

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetPath.toFile())
                .setDepth(1) // full history for hotspot/bus factor analysis
                .call();

        git.close();
        log.info("Clone complete: {}", targetPath);
        return targetPath.toFile();
    }

    /**
     * Open an already-cloned repository.
     */
    public Git openRepository(String jobId) throws IOException {
        Path repoPath = Paths.get(baseDir, jobId);
        return Git.open(repoPath.toFile());
    }

    /**
     * Delete cloned repository to free disk space.
     */
    public void cleanup(String jobId) {
        Path targetPath = Paths.get(baseDir, jobId);
        try {
            if (Files.exists(targetPath)) {
                Files.walk(targetPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Cleaned up repo directory for job {}", jobId);
            }
        } catch (IOException e) {
            log.warn("Failed to clean up directory for job {}: {}", jobId, e.getMessage());
        }
    }

    /**
     * Extract a user-friendly repository name from the URL.
     * e.g. https://github.com/org/repo.git → repo
     */
    public String extractRepoName(String repoUrl) {
        String name = repoUrl.replaceAll("\\.git$", "");
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
}
