package com.repointel.repository;

import com.repointel.model.Hotspot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotspotRepository extends JpaRepository<Hotspot, Long> {
    List<Hotspot> findByJobIdOrderByHotspotScoreDesc(String jobId);
    List<Hotspot> findByJobId(String jobId);
}
