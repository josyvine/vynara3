package com.example.tasks;

import com.example.tools.ToolOperation;

import java.util.ArrayList;
import java.util.List;

public class TaskNode {
    /**
     * Phase 12 Alignment: Complete execution status states including ROLLED_BACK.
     */
    public enum Status { QUEUED, WAITING, RUNNING, COMPLETED, FAILED, RETRYING, SKIPPED, ROLLED_BACK }

    private String id;
    private String title;
    private String description;
    private Status status;
    private int progressPercent;
    private ToolOperation operation;
    private List<String> dependencyTaskIds = new ArrayList<>();
    private String errorMessage;
    private long startTimeMs = 0L;
    private long endTimeMs = 0L;

    public TaskNode(String id, String title, String description, ToolOperation operation) {
        this.id = id;
        this.title = title != null ? title : "Task";
        this.description = description != null ? description : "";
        this.operation = operation;
        this.status = Status.QUEUED;
        this.progressPercent = 0;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public int getProgressPercent() { return progressPercent; }
    public ToolOperation getOperation() { return operation; }
    public List<String> getDependencyTaskIds() { return dependencyTaskIds; }
    public String getErrorMessage() { return errorMessage; }
    public long getStartTimeMs() { return startTimeMs; }
    public long getEndTimeMs() { return endTimeMs; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setOperation(ToolOperation operation) { this.operation = operation; }
    
    public void setStatus(Status status) { 
        this.status = status; 
        if (status == Status.RUNNING && startTimeMs == 0L) {
            this.startTimeMs = System.currentTimeMillis();
        } else if (status == Status.COMPLETED || status == Status.FAILED || status == Status.ROLLED_BACK) {
            this.endTimeMs = System.currentTimeMillis();
        }
    }

    public void setProgressPercent(int progress) { 
        this.progressPercent = Math.max(0, Math.min(100, progress)); 
    }

    public void setErrorMessage(String errorMessage) { 
        this.errorMessage = errorMessage; 
    }

    public TaskNode addDependency(String taskId) {
        if (taskId != null && !taskId.trim().isEmpty() && !dependencyTaskIds.contains(taskId)) {
            dependencyTaskIds.add(taskId);
        }
        return this;
    }

    public long getExecutionDurationMs() {
        if (startTimeMs == 0L) return 0L;
        if (endTimeMs == 0L) return System.currentTimeMillis() - startTimeMs;
        return endTimeMs - startTimeMs;
    }

    public TaskNode cloneNode() {
        TaskNode copy = new TaskNode(this.id, this.title, this.description, this.operation);
        copy.setStatus(this.status);
        copy.setProgressPercent(this.progressPercent);
        copy.setErrorMessage(this.errorMessage);
        copy.dependencyTaskIds.addAll(this.dependencyTaskIds);
        return copy;
    }
}