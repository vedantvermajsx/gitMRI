package com.repointel.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityDTO {
    private Long id;
    private String filePath;
    private String packageName;
    private String className;
    private String methodName;
    private int ccScore;
    private String riskLevel;
    private int lineCount;
    private int nestingDepth;
    private int startLine;
}
