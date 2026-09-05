package com.example.ai.protocol;

import org.json.JSONObject;

public class AIMaterialSpecification {
    private String materialName;
    private String colorHex;
    private float metallic;
    private float roughness;
    private float opacity;

    public AIMaterialSpecification() {
        this.materialName = "Material";
        this.colorHex = "#A0A5BD";
        this.metallic = 0.1f;
        this.roughness = 0.5f;
        this.opacity = 1.0f;
    }

    public static AIMaterialSpecification fromJson(JSONObject json) {
        AIMaterialSpecification spec = new AIMaterialSpecification();
        if (json == null) return spec;

        spec.materialName = json.optString("materialName", "Material");
        spec.colorHex = json.optString("colorHex", "#A0A5BD");
        spec.metallic = (float) json.optDouble("metallic", 0.1f);
        spec.roughness = (float) json.optDouble("roughness", 0.5f);
        spec.opacity = (float) json.optDouble("opacity", 1.0f);

        return spec;
    }

    public String getMaterialName() { return materialName; }
    public String getColorHex() { return colorHex; }
    public float getMetallic() { return metallic; }
    public float getRoughness() { return roughness; }
    public float getOpacity() { return opacity; }

    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    
    public void setMetallic(float metallic) { 
        this.metallic = Math.max(0.0f, Math.min(1.0f, metallic)); 
    }
    
    public void setRoughness(float roughness) { 
        this.roughness = Math.max(0.0f, Math.min(1.0f, roughness)); 
    }
    
    public void setOpacity(float opacity) { 
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity)); 
    }
}