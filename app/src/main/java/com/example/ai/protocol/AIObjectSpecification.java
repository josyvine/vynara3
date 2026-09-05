package com.example.ai.protocol;

import org.json.JSONObject;

public class AIObjectSpecification {
    private String name;
    private String type;
    private float width;
    private float height;
    private float depth;
    private String colorHex;

    public AIObjectSpecification() {
        this.name = "Object";
        this.type = "cube";
        this.width = 1.0f;
        this.height = 1.0f;
        this.depth = 1.0f;
        this.colorHex = "#A0A5BD";
    }

    public static AIObjectSpecification fromJson(JSONObject json) {
        AIObjectSpecification spec = new AIObjectSpecification();
        if (json == null) return spec;

        spec.name = json.optString("name", "Object");
        spec.type = json.optString("type", "cube");
        spec.width = (float) json.optDouble("width", 1.0f);
        spec.height = (float) json.optDouble("height", 1.0f);
        spec.depth = (float) json.optDouble("depth", 1.0f);
        spec.colorHex = json.optString("colorHex", "#A0A5BD");

        return spec;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getDepth() { return depth; }
    public String getColorHex() { return colorHex; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }
    public void setDepth(float depth) { this.depth = depth; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}