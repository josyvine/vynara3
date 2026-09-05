package com.example.tasks;

import com.example.knowledge.KnowledgeEntry;

import java.util.ArrayList;
import java.util.List;

public class ProductionPlan {
    private String projectName;
    private String userPrompt;
    private String intent; // CREATE_3D_ASSET, CREATE_CHARACTER, CREATE_CREATURE, CREATE_ARCHITECTURE, MODIFY_SCENE
    private String style = "Photorealistic";
    private String targetEngine = "OpenGL ES / GLTF";
    private KnowledgeEntry knowledgeReference;
    private TaskGraph taskGraph;
    private List<String> referenceImageUris;
    private long createdAtMs;

    public ProductionPlan(String projectName, String userPrompt, String intent, KnowledgeEntry knowledgeReference) {
        this.projectName = projectName != null ? projectName : "3D Project";
        this.userPrompt = userPrompt != null ? userPrompt : "";
        this.intent = intent != null ? intent : "CREATE_3D_ASSET";
        this.knowledgeReference = knowledgeReference;
        this.taskGraph = new TaskGraph();
        this.referenceImageUris = new ArrayList<>();
        this.createdAtMs = System.currentTimeMillis();
    }

    public ProductionPlan(String projectName, String userPrompt, String intent, KnowledgeEntry knowledgeReference, List<String> referenceImageUris) {
        this(projectName, userPrompt, intent, knowledgeReference);
        if (referenceImageUris != null) {
            this.referenceImageUris.addAll(referenceImageUris);
        }
    }

    public String getProjectName() { return projectName; }
    public String getUserPrompt() { return userPrompt; }
    public String getIntent() { return intent; }
    public String getStyle() { return style; }
    public String getTargetEngine() { return targetEngine; }
    public KnowledgeEntry getKnowledgeReference() { return knowledgeReference; }
    public TaskGraph getTaskGraph() { return taskGraph; }
    public List<String> getReferenceImageUris() { return referenceImageUris; }
    public long getCreatedAtMs() { return createdAtMs; }

    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    public void setIntent(String intent) { this.intent = intent; }
    public void setStyle(String style) { this.style = style; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public void setKnowledgeReference(KnowledgeEntry knowledgeReference) { this.knowledgeReference = knowledgeReference; }

    public void setReferenceImageUris(List<String> referenceImageUris) {
        this.referenceImageUris = referenceImageUris != null ? referenceImageUris : new ArrayList<>();
    }

    public void addReferenceImageUri(String uriStr) {
        if (uriStr != null && !uriStr.trim().isEmpty()) {
            this.referenceImageUris.add(uriStr);
        }
    }

    public boolean hasReferenceImages() {
        return referenceImageUris != null && !referenceImageUris.isEmpty();
    }
}