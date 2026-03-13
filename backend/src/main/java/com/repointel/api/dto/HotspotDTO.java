package com.repointel.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotspotDTO {
    private Long id;
    private String filePath;
    private String fileName;
    private int commitCount;
    private BigDecimal avgComplexity;
    private BigDecimal hotspotScore;
    private String riskLevel;
    private LocalDateTime lastModified;
}
