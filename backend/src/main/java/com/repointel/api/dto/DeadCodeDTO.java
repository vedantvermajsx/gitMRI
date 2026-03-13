package com.repointel.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadCodeDTO {
    private Long id;
    private String itemType;
    private String name;
    private String qualifiedName;
    private String filePath;
    private int lineNumber;
    private String riskLevel;
    private String reason;
}
