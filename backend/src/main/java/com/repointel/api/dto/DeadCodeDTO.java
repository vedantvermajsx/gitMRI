package com.repointel.api.dto;

public class DeadCodeDTO {
    private Long id;
    private String itemType;
    private String name;
    private String qualifiedName;
    private String filePath;
    private int lineNumber;
    private String riskLevel;
    private String reason;

    public DeadCodeDTO() {}

    public Long getId() { return id; }
    public String getItemType() { return itemType; }
    public String getName() { return name; }
    public String getQualifiedName() { return qualifiedName; }
    public String getFilePath() { return filePath; }
    public int getLineNumber() { return lineNumber; }
    public String getRiskLevel() { return riskLevel; }
    public String getReason() { return reason; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final DeadCodeDTO d = new DeadCodeDTO();
        public Builder id(Long v) { d.id = v; return this; }
        public Builder itemType(String v) { d.itemType = v; return this; }
        public Builder name(String v) { d.name = v; return this; }
        public Builder qualifiedName(String v) { d.qualifiedName = v; return this; }
        public Builder filePath(String v) { d.filePath = v; return this; }
        public Builder lineNumber(int v) { d.lineNumber = v; return this; }
        public Builder riskLevel(String v) { d.riskLevel = v; return this; }
        public Builder reason(String v) { d.reason = v; return this; }
        public DeadCodeDTO build() { return d; }
    }
}
