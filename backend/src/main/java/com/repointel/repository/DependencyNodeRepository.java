package com.repointel.repository;

import com.repointel.model.DependencyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependencyNodeRepository extends JpaRepository<DependencyNode, Long> {
    List<DependencyNode> findByJobId(String jobId);
    Optional<DependencyNode> findByJobIdAndGroupIdAndArtifactId(String jobId, String groupId, String artifactId);
}
