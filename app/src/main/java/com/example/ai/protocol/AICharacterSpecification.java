package com.example.ai.protocol;

import org.json.JSONObject;

public class AICharacterSpecification {
    private String species;
    private String name;
    private float height;
    private String style;
    private boolean isRigRequired;
    private boolean isAnimationRequired;

    public AICharacterSpecification() {
        this.species = "HUMANOID";
        this.name = "Character";
        this.height = 1.8f;
        this.style = "REALISTIC";
        this.isRigRequired = true;
        this.isAnimationRequired = true;
    }

    public static AICharacterSpecification fromJson(JSONObject json) {
        AICharacterSpecification spec = new AICharacterSpecification();
        if (json == null) return spec;

        spec.species = json.optString("species", "HUMANOID").toUpperCase();
        spec.name = json.optString("name", "Character");
        spec.height = (float) json.optDouble("height", 1.8f);
        spec.style = json.optString("style", "REALISTIC").toUpperCase();
        spec.isRigRequired = json.optBoolean("isRigRequired", true);
        spec.isAnimationRequired = json.optBoolean("isAnimationRequired", true);

        return spec;
    }

    public String getSpecies() { return species; }
    public String getName() { return name; }
    public float getHeight() { return height; }
    public String getStyle() { return style; }
    public boolean isRigRequired() { return isRigRequired; }
    public boolean isAnimationRequired() { return isAnimationRequired; }

    public void setSpecies(String species) { this.species = species; }
    public void setName(String name) { this.name = name; }
    public void setHeight(float height) { this.height = height; }
    public void setStyle(String style) { this.style = style; }
    public void setRigRequired(boolean rigRequired) { isRigRequired = rigRequired; }
    public void setAnimationRequired(boolean animationRequired) { isAnimationRequired = animationRequired; }
}