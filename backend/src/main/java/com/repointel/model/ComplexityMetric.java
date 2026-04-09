package com.repointel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "complexity_metrics")
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

    public ComplexityMetric() {}

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public String getFilePath() { return filePath; }
    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public int getCcScore() { return ccScore; }
    public int getLineCount() { return lineCount; }
    public int getNestingDepth() { return nestingDepth; }
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String v) { this.jobId = v; }
    public void setFilePath(String v) { this.filePath = v; }
    public void setPackageName(String v) { this.packageName = v; }
    public void setClassName(String v) { this.className = v; }
    public void setMethodName(String v) { this.methodName = v; }
    public void setCcScore(int v) { this.ccScore = v; }
    public void setLineCount(int v) { this.lineCount = v; }
    public void setNestingDepth(int v) { this.nestingDepth = v; }
    public void setStartLine(int v) { this.startLine = v; }
    public void setEndLine(int v) { this.endLine = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ComplexityMetric m = new ComplexityMetric();
        public Builder jobId(String v) { m.jobId = v; return this; }
        public Builder filePath(String v) { m.filePath = v; return this; }
        public Builder packageName(String v) { m.packageName = v; return this; }
        public Builder className(String v) { m.className = v; return this; }
        public Builder methodName(String v) { m.methodName = v; return this; }
        public Builder ccScore(int v) { m.ccScore = v; return this; }
        public Builder lineCount(int v) { m.lineCount = v; return this; }
        public Builder nestingDepth(int v) { m.nestingDepth = v; return this; }
        public Builder startLine(int v) { m.startLine = v; return this; }
        public Builder endLine(int v) { m.endLine = v; return this; }
        public ComplexityMetric build() { return m; }
    }
}
