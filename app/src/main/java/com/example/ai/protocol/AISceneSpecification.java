package com.example.ai.protocol;

import org.json.JSONObject;

public class AISceneSpecification {
    private String sceneType;
    private String lightingTime;
    private String cameraAngle;
    private String environmentMap;

    public AISceneSpecification() {
        this.sceneType = "generic";
        this.lightingTime = "daylight";
        this.cameraAngle = "perspective";
        this.environmentMap = "default";
    }

    public static AISceneSpecification fromJson(JSONObject json) {
        AISceneSpecification spec = new AISceneSpecification();
        if (json == null) return spec;

        spec.sceneType = json.optString("sceneType", "generic");
        spec.lightingTime = json.optString("lightingTime", "daylight");
        spec.cameraAngle = json.optString("cameraAngle", "perspective");
        spec.environmentMap = json.optString("environmentMap", "default");

        return spec;
    }

    public String getSceneType() { return sceneType; }
    public String getLightingTime() { return lightingTime; }
    public String getCameraAngle() { return cameraAngle; }
    public String getEnvironmentMap() { return environmentMap; }

    public void setSceneType(String sceneType) { this.sceneType = sceneType; }
    public void setLightingTime(String lightingTime) { this.lightingTime = lightingTime; }
    public void setCameraAngle(String cameraAngle) { this.cameraAngle = cameraAngle; }
    public void setEnvironmentMap(String environmentMap) { this.environmentMap = environmentMap; }
}