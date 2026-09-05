package com.example.ai.protocol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AIDirectorSpec {
    // Provenance Tracking (Zero Guesswork)
    private boolean isLiveAiGenerated = false;
    private String generationSource = "UNINITIALIZED";

    // High-Level Artistic Intent
    private String sceneType = "environment";
    private String mood = "misty_dawn";
    private String visualStyleNotes = "Dynamic procedural 3D scene.";

    // Camera Contract
    private float focalLengthMm = 50.0f;
    private float apertureFStop = 1.8f; // f/1.8 for shallow depth-of-field bokeh
    private float focusDistance = 4.5f;
    private float[] cameraPosition = new float[] { 0.0f, -7.0f, 2.5f };
    private float[] cameraTarget = new float[] { 0.0f, 0.0f, 1.2f };

    // Atmosphere & Volumetric Lighting Contract
    private boolean useVolumetrics = true;
    private float volumetricDensity = 0.015f; // Standard density for light shafts
    private float sunElevation = 18.0f;       // Low sun angle for long shadows
    private float sunAzimuth = 45.0f;
    private float sunIntensity = 4.5f;
    private String ambientColorHex = "#202835";

    // Palette & Material Contract
    private String primaryColorHex = "#3D4A32";
    private String secondaryColorHex = "#5A4432";
    private String accentColorHex = "#D4A359";
    private final List<String> requiredMaterials = new ArrayList<>();

    // Modular Seeds (for 1-click re-rolling of individual layers)
    private int seedTerrain = 101;
    private int seedHero = 202;
    private int seedVegetation = 303;
    private int seedLighting = 404;

    public AIDirectorSpec() {
        requiredMaterials.add("mat_hero_primary");
        requiredMaterials.add("mat_secondary_detail");
        requiredMaterials.add("mat_foliage_leaf");
    }

    public static AIDirectorSpec fromJson(JSONObject json, String sourceModel) {
        AIDirectorSpec spec = new AIDirectorSpec();
        if (json == null) return spec;

        spec.isLiveAiGenerated = true;
        spec.generationSource = "LIVE_GEMINI_API: " + (sourceModel != null ? sourceModel : "Unknown");

        spec.sceneType = json.optString("sceneType", spec.sceneType);
        spec.mood = json.optString("mood", spec.mood);
        spec.visualStyleNotes = json.optString("visualStyleNotes", spec.visualStyleNotes);

        // Parse Camera
        JSONObject camObj = json.optJSONObject("camera");
        if (camObj != null) {
            spec.focalLengthMm = (float) camObj.optDouble("focalLengthMm", spec.focalLengthMm);
            spec.apertureFStop = (float) camObj.optDouble("apertureFStop", spec.apertureFStop);
            spec.focusDistance = (float) camObj.optDouble("focusDistance", spec.focusDistance);

            JSONArray posArr = camObj.optJSONArray("position");
            if (posArr != null && posArr.length() >= 3) {
                spec.cameraPosition[0] = (float) posArr.optDouble(0, spec.cameraPosition[0]);
                spec.cameraPosition[1] = (float) posArr.optDouble(1, spec.cameraPosition[1]);
                spec.cameraPosition[2] = (float) posArr.optDouble(2, spec.cameraPosition[2]);
            }

            JSONArray targetArr = camObj.optJSONArray("target");
            if (targetArr != null && targetArr.length() >= 3) {
                spec.cameraTarget[0] = (float) targetArr.optDouble(0, spec.cameraTarget[0]);
                spec.cameraTarget[1] = (float) targetArr.optDouble(1, spec.cameraTarget[1]);
                spec.cameraTarget[2] = (float) targetArr.optDouble(2, spec.cameraTarget[2]);
            }
        }

        // Parse Lighting
        JSONObject lightObj = json.optJSONObject("lighting");
        if (lightObj != null) {
            spec.useVolumetrics = lightObj.optBoolean("useVolumetrics", spec.useVolumetrics);
            spec.volumetricDensity = (float) lightObj.optDouble("volumetricDensity", spec.volumetricDensity);
            spec.sunElevation = (float) lightObj.optDouble("sunElevation", spec.sunElevation);
            spec.sunAzimuth = (float) lightObj.optDouble("sunAzimuth", spec.sunAzimuth);
            spec.sunIntensity = (float) lightObj.optDouble("sunIntensity", spec.sunIntensity);
            spec.ambientColorHex = lightObj.optString("ambientColorHex", spec.ambientColorHex);
        }

        // Parse Palette
        JSONObject palObj = json.optJSONObject("palette");
        if (palObj != null) {
            spec.primaryColorHex = palObj.optString("primaryColorHex", spec.primaryColorHex);
            spec.secondaryColorHex = palObj.optString("secondaryColorHex", spec.secondaryColorHex);
            spec.accentColorHex = palObj.optString("accentColorHex", spec.accentColorHex);
        }

        // Parse Seeds
        JSONObject seedsObj = json.optJSONObject("seeds");
        if (seedsObj != null) {
            spec.seedTerrain = seedsObj.optInt("seedTerrain", spec.seedTerrain);
            spec.seedHero = seedsObj.optInt("seedHero", spec.seedHero);
            spec.seedVegetation = seedsObj.optInt("seedVegetation", spec.seedVegetation);
            spec.seedLighting = seedsObj.optInt("seedLighting", spec.seedLighting);
        }

        return spec;
    }

    public JSONObject toJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("sceneType", sceneType);
            root.put("mood", mood);
            root.put("visualStyleNotes", visualStyleNotes);

            JSONObject camObj = new JSONObject();
            camObj.put("focalLengthMm", focalLengthMm);
            camObj.put("apertureFStop", apertureFStop);
            camObj.put("focusDistance", focusDistance);
            JSONArray posArr = new JSONArray();
            posArr.put(cameraPosition[0]).put(cameraPosition[1]).put(cameraPosition[2]);
            camObj.put("position", posArr);
            JSONArray targetArr = new JSONArray();
            targetArr.put(cameraTarget[0]).put(cameraTarget[1]).put(cameraTarget[2]);
            camObj.put("target", targetArr);
            root.put("camera", camObj);

            JSONObject lightObj = new JSONObject();
            lightObj.put("useVolumetrics", useVolumetrics);
            lightObj.put("volumetricDensity", volumetricDensity);
            lightObj.put("sunElevation", sunElevation);
            lightObj.put("sunAzimuth", sunAzimuth);
            lightObj.put("sunIntensity", sunIntensity);
            lightObj.put("ambientColorHex", ambientColorHex);
            root.put("lighting", lightObj);

            JSONObject palObj = new JSONObject();
            palObj.put("primaryColorHex", primaryColorHex);
            palObj.put("secondaryColorHex", secondaryColorHex);
            palObj.put("accentColorHex", accentColorHex);
            root.put("palette", palObj);

            JSONObject seedsObj = new JSONObject();
            seedsObj.put("seedTerrain", seedTerrain);
            seedsObj.put("seedHero", seedHero);
            seedsObj.put("seedVegetation", seedVegetation);
            seedsObj.put("seedLighting", seedLighting);
            root.put("seeds", seedsObj);
        } catch (Exception ignored) {}
        return root;
    }

    public boolean isLiveAiGenerated() { 
        return isLiveAiGenerated; 
    }

    public String getGenerationSource() { 
        return generationSource; 
    }

    public void markAsOfflineFallback(String reason) {
        this.isLiveAiGenerated = false;
        this.generationSource = "OFFLINE_FALLBACK: " + reason;
    }

    public String getSceneType() { 
        return sceneType; 
    }

    public void setSceneType(String sceneType) { 
        this.sceneType = sceneType; 
    }

    public String getMood() { 
        return mood; 
    }

    public void setMood(String mood) { 
        this.mood = mood; 
    }

    public String getVisualStyleNotes() { 
        return visualStyleNotes; 
    }

    public void setVisualStyleNotes(String visualStyleNotes) { 
        this.visualStyleNotes = visualStyleNotes; 
    }

    public float getFocalLengthMm() { 
        return focalLengthMm; 
    }

    public void setFocalLengthMm(float focalLengthMm) { 
        this.focalLengthMm = focalLengthMm; 
    }

    public float getApertureFStop() { 
        return apertureFStop; 
    }

    public void setApertureFStop(float apertureFStop) { 
        this.apertureFStop = apertureFStop; 
    }

    public float getFocusDistance() { 
        return focusDistance; 
    }

    public void setFocusDistance(float focusDistance) { 
        this.focusDistance = focusDistance; 
    }

    public float[] getCameraPosition() { 
        return cameraPosition; 
    }

    public void setCameraPosition(float x, float y, float z) { 
        this.cameraPosition = new float[] { x, y, z }; 
    }

    public float[] getCameraTarget() { 
        return cameraTarget; 
    }

    public void setCameraTarget(float x, float y, float z) { 
        this.cameraTarget = new float[] { x, y, z }; 
    }

    public boolean isUseVolumetrics() { 
        return useVolumetrics; 
    }

    public void setUseVolumetrics(boolean useVolumetrics) { 
        this.useVolumetrics = useVolumetrics; 
    }

    public float getVolumetricDensity() { 
        return volumetricDensity; 
    }

    public void setVolumetricDensity(float volumetricDensity) { 
        this.volumetricDensity = volumetricDensity; 
    }

    public float getSunElevation() { 
        return sunElevation; 
    }

    public void setSunElevation(float sunElevation) { 
        this.sunElevation = sunElevation; 
    }

    public float getSunAzimuth() { 
        return sunAzimuth; 
    }

    public void setSunAzimuth(float sunAzimuth) { 
        this.sunAzimuth = sunAzimuth; 
    }

    public float getSunIntensity() { 
        return sunIntensity; 
    }

    public void setSunIntensity(float sunIntensity) { 
        this.sunIntensity = sunIntensity; 
    }

    public String getAmbientColorHex() { 
        return ambientColorHex; 
    }

    public void setAmbientColorHex(String ambientColorHex) { 
        this.ambientColorHex = ambientColorHex; 
    }

    public String getPrimaryColorHex() { 
        return primaryColorHex; 
    }

    public void setPrimaryColorHex(String primaryColorHex) { 
        this.primaryColorHex = primaryColorHex; 
    }

    public String getSecondaryColorHex() { 
        return secondaryColorHex; 
    }

    public void setSecondaryColorHex(String secondaryColorHex) { 
        this.secondaryColorHex = secondaryColorHex; 
    }

    public String getAccentColorHex() { 
        return accentColorHex; 
    }

    public void setAccentColorHex(String accentColorHex) { 
        this.accentColorHex = accentColorHex; 
    }

    public List<String> getRequiredMaterials() { 
        return requiredMaterials; 
    }

    public int getSeedTerrain() { 
        return seedTerrain; 
    }

    public void setSeedTerrain(int seedTerrain) { 
        this.seedTerrain = seedTerrain; 
    }

    public int getSeedHero() { 
        return seedHero; 
    }

    public void setSeedHero(int seedHero) { 
        this.seedHero = seedHero; 
    }

    public int getSeedVegetation() { 
        return seedVegetation; 
    }

    public void setSeedVegetation(int seedVegetation) { 
        this.seedVegetation = seedVegetation; 
    }

    public int getSeedLighting() { 
        return seedLighting; 
    }

    public void setSeedLighting(int seedLighting) { 
        this.seedLighting = seedLighting; 
    }
}