package com.repointel.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Service
public class RepoCloner {

    private static final Logger log = LoggerFactory.getLogger(RepoCloner.class);

    @Value("${repointel.clone.base-dir:/tmp/repointel/repos}")
    private String baseDir;

    public File cloneRepository(String repoUrl, String jobId) throws GitAPIException, IOException {
        Path targetPath = Paths.get(baseDir, jobId);
        Files.createDirectories(targetPath);
        log.info("Cloning {} into {}", repoUrl, targetPath);
        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetPath.toFile())
                .call();
        git.close();
        log.info("Clone complete: {}", targetPath);
        return targetPath.toFile();
    }

    public Git openRepository(String jobId) throws IOException {
        return Git.open(Paths.get(baseDir, jobId).toFile());
    }

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

    public String extractRepoName(String repoUrl) {
        String name = repoUrl.replaceAll("\\.git$", "");
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
}
