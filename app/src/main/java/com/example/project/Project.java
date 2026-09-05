package com.example.project;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Project {
    private String id;
    private String title;
    private String type; // CHARACTER, CREATURE, ARCHITECTURE, SCENE, FURNITURE
    private String status; // READY, IN_PROGRESS, DRAFT
    private String userPrompt;
    private String filePath;
    private String thumbnailPath;
    private String style = "Photorealistic";
    private String targetEngine = "OpenGL ES / GLTF";
    private int polyCount;
    private long lastModifiedMs;

    public Project(String id, String title, String type, String status, int polyCount) {
        this.id = id != null ? id : "proj_" + System.currentTimeMillis();
        this.title = title != null ? title : "Untitled Project";
        this.type = type != null ? type : "SCENE";
        this.status = status != null ? status : "READY";
        this.polyCount = Math.max(0, polyCount);
        this.lastModifiedMs = System.currentTimeMillis();
    }

    public Project(String id, String title, String type, String status, String userPrompt, int polyCount) {
        this(id, title, type, status, polyCount);
        this.userPrompt = userPrompt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getUserPrompt() { return userPrompt; }
    public String getFilePath() { return filePath; }
    public String getThumbnailPath() { return thumbnailPath; }
    public String getStyle() { return style; }
    public String getTargetEngine() { return targetEngine; }
    public int getPolyCount() { return polyCount; }
    public long getLastModifiedMs() { return lastModifiedMs; }

    public void setTitle(String title) { 
        this.title = title; 
        touch(); 
    }

    public void setType(String type) { 
        this.type = type; 
        touch(); 
    }

    public void setStatus(String status) { 
        this.status = status; 
        touch(); 
    }

    public void setUserPrompt(String userPrompt) { 
        this.userPrompt = userPrompt; 
        touch(); 
    }

    public void setFilePath(String filePath) { 
        this.filePath = filePath; 
    }

    public void setThumbnailPath(String thumbnailPath) { 
        this.thumbnailPath = thumbnailPath; 
    }

    public void setStyle(String style) { 
        this.style = style; 
    }

    public void setTargetEngine(String targetEngine) { 
        this.targetEngine = targetEngine; 
    }

    public void setPolyCount(int polyCount) { 
        this.polyCount = Math.max(0, polyCount); 
        touch(); 
    }

    public void setLastModifiedMs(long lastModifiedMs) { 
        this.lastModifiedMs = lastModifiedMs; 
    }

    public void touch() {
        this.lastModifiedMs = System.currentTimeMillis();
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastModifiedMs));
    }
}