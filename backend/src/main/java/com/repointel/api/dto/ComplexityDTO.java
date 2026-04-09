package com.repointel.api.dto;

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

    public ComplexityDTO() {}

    public Long getId() { return id; }
    public String getFilePath() { return filePath; }
    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public int getCcScore() { return ccScore; }
    public String getRiskLevel() { return riskLevel; }
    public int getLineCount() { return lineCount; }
    public int getNestingDepth() { return nestingDepth; }
    public int getStartLine() { return startLine; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ComplexityDTO d = new ComplexityDTO();
        public Builder id(Long v) { d.id = v; return this; }
        public Builder filePath(String v) { d.filePath = v; return this; }
        public Builder packageName(String v) { d.packageName = v; return this; }
        public Builder className(String v) { d.className = v; return this; }
        public Builder methodName(String v) { d.methodName = v; return this; }
        public Builder ccScore(int v) { d.ccScore = v; return this; }
        public Builder riskLevel(String v) { d.riskLevel = v; return this; }
        public Builder lineCount(int v) { d.lineCount = v; return this; }
        public Builder nestingDepth(int v) { d.nestingDepth = v; return this; }
        public Builder startLine(int v) { d.startLine = v; return this; }
        public ComplexityDTO build() { return d; }
    }
}
