package com.example.validation;

public class ValidationResult {
    public enum Severity { PASS, WARNING, ERROR, CRITICAL }
    
    public enum Category { MESH, MATERIAL, SKELETON, SKIN, RIG, ANIMATION, SCENE, EXPORT }

    private Severity severity;
    private Category category;
    private String message;
    private String repairSuggestion;
    private String targetObjectId;
    private long timestampMs;

    public ValidationResult(Severity severity, String message, String repairSuggestion) {
        this(severity, Category.SCENE, message, repairSuggestion, null);
    }

    public ValidationResult(Severity severity, Category category, String message, String repairSuggestion, String targetObjectId) {
        this.severity = severity != null ? severity : Severity.PASS;
        this.category = category != null ? category : Category.SCENE;
        this.message = message != null ? message : "";
        this.repairSuggestion = repairSuggestion;
        this.targetObjectId = targetObjectId;
        this.timestampMs = System.currentTimeMillis();
    }

    public Severity getSeverity() { return severity; }
    public Category getCategory() { return category; }
    public String getMessage() { return message; }
    public String getRepairSuggestion() { return repairSuggestion; }
    public String getTargetObjectId() { return targetObjectId; }
    public long getTimestampMs() { return timestampMs; }

    public boolean isPassed() { 
        return severity == Severity.PASS || severity == Severity.WARNING; 
    }

    public void setSeverity(Severity severity) { this.severity = severity; }
    public void setCategory(Category category) { this.category = category; }
    public void setMessage(String message) { this.message = message; }
    public void setRepairSuggestion(String suggestion) { this.repairSuggestion = suggestion; }
    
    public ValidationResult setTargetObjectId(String objectId) { 
        this.targetObjectId = objectId; 
        return this;
    }
}