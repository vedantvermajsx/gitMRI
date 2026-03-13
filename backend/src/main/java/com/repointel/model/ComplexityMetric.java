package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "complexity_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplexityMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "package_name", length = 255)
    private String packageName;

    @Column(name = "class_name", length = 255)
    private String className;

    @Column(name = "method_name", length = 255)
    private String methodName;

    @Column(name = "cc_score")
    private int ccScore;

    @Column(name = "line_count")
    private int lineCount;

    @Column(name = "nesting_depth")
    private int nestingDepth;

    @Column(name = "start_line")
    private int startLine;

    @Column(name = "end_line")
    private int endLine;
}
