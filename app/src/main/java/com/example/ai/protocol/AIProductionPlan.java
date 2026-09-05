package com.example.ai.protocol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AIProductionPlan {
    private String intent;
    private String sceneType;
    private String style;
    private String quality;
    private String lighting;
    private String camera;
    
    private final List<String> components = new ArrayList<>();
    private final List<AIToolCall> toolCalls = new ArrayList<>();
    private final List<AIObjectSpecification> objects = new ArrayList<>();
    private final List<AIMaterialSpecification> materials = new ArrayList<>();
    private final List<AICharacterSpecification> characters = new ArrayList<>();
    private final List<String> validationRules = new ArrayList<>();

    public AIProductionPlan() {
    }

    /**
     * Phase 2 Alignment: Parses Gemini's structured response JSON into 
     * strongly typed intent fields, object specifications, PBR materials, 
     * skeletal setups, and executable tool call DAGs.
     */
    public static AIProductionPlan fromJson(JSONObject json) {
        AIProductionPlan plan = new AIProductionPlan();
        if (json == null) return plan;

        plan.intent = json.optString("intent", "CREATE_SCENE");
        plan.sceneType = json.optString("sceneType", "generic");
        plan.style = json.optString("style", "realistic");
        plan.quality = json.optString("quality", "high");
        plan.lighting = json.optString("lighting", "daylight");
        plan.camera = json.optString("camera", "perspective");

        // Parse structural components list
        JSONArray compArr = json.optJSONArray("components");
        if (compArr != null) {
            for (int i = 0; i < compArr.length(); i++) {
                String comp = compArr.optString(i);
                if (comp != null && !comp.trim().isEmpty()) {
                    plan.components.add(comp.trim());
                }
            }
        }

        // Parse detailed object blueprints (with dimensions)
        JSONArray objectsArr = json.optJSONArray("objects");
        if (objectsArr != null) {
            for (int i = 0; i < objectsArr.length(); i++) {
                JSONObject objJson = objectsArr.optJSONObject(i);
                if (objJson != null) {
                    plan.objects.add(AIObjectSpecification.fromJson(objJson));
                }
            }
        }

        // Parse customized PBR materials
        JSONArray materialsArr = json.optJSONArray("materials");
        if (materialsArr != null) {
            for (int i = 0; i < materialsArr.length(); i++) {
                JSONObject matJson = materialsArr.optJSONObject(i);
                if (matJson != null) {
                    plan.materials.add(AIMaterialSpecification.fromJson(matJson));
                }
            }
        }

        // Parse characters/anatomy specifications
        JSONArray charsArr = json.optJSONArray("characters");
        if (charsArr != null) {
            for (int i = 0; i < charsArr.length(); i++) {
                JSONObject charJson = charsArr.optJSONObject(i);
                if (charJson != null) {
                    plan.characters.add(AICharacterSpecification.fromJson(charJson));
                }
            }
        }

        // Parse tool calls DAG sequence (safely mapping varying tool arrays)
        JSONArray toolsArr = json.optJSONArray("requiredTools");
        if (toolsArr == null) {
            toolsArr = json.optJSONArray("toolCalls");
        }
        if (toolsArr == null) {
            toolsArr = json.optJSONArray("tools");
        }
        if (toolsArr != null) {
            for (int i = 0; i < toolsArr.length(); i++) {
                JSONObject toolObj = toolsArr.optJSONObject(i);
                if (toolObj != null) {
                    plan.toolCalls.add(AIToolCall.fromJson(toolObj));
                }
            }
        }

        // Parse scene validation rules
        JSONArray validationArr = json.optJSONArray("validationRules");
        if (validationArr != null) {
            for (int i = 0; i < validationArr.length(); i++) {
                String rule = validationArr.optString(i);
                if (rule != null && !rule.trim().isEmpty()) {
                    plan.validationRules.add(rule.trim());
                }
            }
        }

        return plan;
    }

    public String getIntent() { return intent; }
    public String getSceneType() { return sceneType; }
    public String getStyle() { return style; }
    public String getQuality() { return quality; }
    public String getLighting() { return lighting; }
    public String getCamera() { return camera; }
    
    public List<String> getComponents() { return components; }
    public List<AIToolCall> getToolCalls() { return toolCalls; }
    public List<AIObjectSpecification> getObjects() { return objects; }
    public List<AIMaterialSpecification> getMaterials() { return materials; }
    public List<AICharacterSpecification> getCharacters() { return characters; }
    public List<String> getValidationRules() { return validationRules; }

    public void setIntent(String intent) { this.intent = intent; }
    public void setSceneType(String sceneType) { this.sceneType = sceneType; }
    public void setStyle(String style) { this.style = style; }
    public void setQuality(String quality) { this.quality = quality; }
    public void setLighting(String lighting) { this.lighting = lighting; }
    public void setCamera(String camera) { this.camera = camera; }
}