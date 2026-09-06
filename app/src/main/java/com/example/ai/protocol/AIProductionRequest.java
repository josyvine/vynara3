package com.example.ai.protocol;

import java.util.ArrayList;
import java.util.List;

public class AIProductionRequest {
    private String userPrompt;
    private String style;
    private String targetEngine;
    private String qualityLevel;
    private boolean isDemoPreset = false;
    private String demoPresetId = null;
    private final List<String> referenceImageUris = new ArrayList<>();

    public AIProductionRequest(String userPrompt) {
        this.userPrompt = userPrompt != null ? userPrompt : "";
        this.style = "Photorealistic";
        this.targetEngine = "OpenGL ES / GLTF";
        this.qualityLevel = "high";
    }

    public AIProductionRequest(String userPrompt, String style, String targetEngine) {
        this.userPrompt = userPrompt != null ? userPrompt : "";
        this.style = style != null ? style : "Photorealistic";
        this.targetEngine = targetEngine != null ? targetEngine : "OpenGL ES / GLTF";
        this.qualityLevel = "high";
    }

    public AIProductionRequest(String userPrompt, String style, String targetEngine, String qualityLevel) {
        this.userPrompt = userPrompt != null ? userPrompt : "";
        this.style = style != null ? style : "Photorealistic";
        this.targetEngine = targetEngine != null ? targetEngine : "OpenGL ES / GLTF";
        this.qualityLevel = qualityLevel != null ? qualityLevel : "high";
    }

    public String getUserPrompt() { return userPrompt; }
    public String getStyle() { return style; }
    public String getTargetEngine() { return targetEngine; }
    public String getQualityLevel() { return qualityLevel; }
    public List<String> getReferenceImageUris() { return referenceImageUris; }
    public boolean isDemoPreset() { return isDemoPreset; }
    public String getDemoPresetId() { return demoPresetId; }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setTargetEngine(String targetEngine) {
        this.targetEngine = targetEngine;
    }

    public void setQualityLevel(String qualityLevel) {
        this.qualityLevel = qualityLevel;
    }

    public void setDemoPreset(boolean isDemoPreset) {
        this.isDemoPreset = isDemoPreset;
    }

    public void setDemoPresetId(String demoPresetId) {
        this.demoPresetId = demoPresetId;
    }

    public AIProductionRequest addReferenceImageUri(String uriStr) {
        if (uriStr != null && !uriStr.trim().isEmpty() && !referenceImageUris.contains(uriStr.trim())) {
            referenceImageUris.add(uriStr.trim());
        }
        return this;
    }

    public void setReferenceImageUris(List<String> uris) {
        referenceImageUris.clear();
        if (uris != null) {
            for (String u : uris) {
                if (u != null && !u.trim().isEmpty() && !referenceImageUris.contains(u.trim())) {
                    referenceImageUris.add(u.trim());
                }
            }
        }
    }

    public void clearReferenceImages() {
        referenceImageUris.clear();
    }

    public boolean hasReferenceImages() {
        return !referenceImageUris.isEmpty();
    }
}