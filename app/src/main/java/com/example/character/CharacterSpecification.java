package com.example.character;

public class CharacterSpecification {
    private String species; // HUMANOID, DOG, BIRD, QUADRUPED, CREATURE
    private String name;
    private float height = 1.8f;
    private String style = "REALISTIC"; // REALISTIC, SUPERHERO, CARTOON, LOW_POLY
    
    // Anatomical Proportions
    private float shoulderWidth = 0.45f;
    private float limbLengthRatio = 1.0f;
    private float headSizeRatio = 1.0f;
    private float bodyBuildFactor = 1.0f; // 0.8 thin/slender, 1.0 average, 1.3 heroic/muscular
    private String skinColorHex = "#E0AC69";

    private boolean isRigRequired = true;
    private boolean isAnimationRequired = true;

    public CharacterSpecification(String species, String name) {
        this.species = species != null ? species.toUpperCase() : "HUMANOID";
        this.name = name != null ? name : "Character";
    }

    public String getSpecies() { return species; }
    public String getName() { return name; }
    public float getHeight() { return height; }
    public String getStyle() { return style; }
    public float getShoulderWidth() { return shoulderWidth; }
    public float getLimbLengthRatio() { return limbLengthRatio; }
    public float getHeadSizeRatio() { return headSizeRatio; }
    public float getBodyBuildFactor() { return bodyBuildFactor; }
    public String getSkinColorHex() { return skinColorHex; }

    public boolean isRigRequired() { return isRigRequired; }
    public boolean isAnimationRequired() { return isAnimationRequired; }

    public CharacterSpecification setHeight(float height) { 
        this.height = Math.max(0.2f, height); 
        return this; 
    }

    public CharacterSpecification setStyle(String style) { 
        this.style = style != null ? style.toUpperCase() : "REALISTIC"; 
        return this; 
    }

    public CharacterSpecification setShoulderWidth(float shoulderWidth) {
        this.shoulderWidth = Math.max(0.1f, shoulderWidth);
        return this;
    }

    public CharacterSpecification setLimbLengthRatio(float ratio) {
        this.limbLengthRatio = Math.max(0.5f, Math.min(2.0f, ratio));
        return this;
    }

    public CharacterSpecification setHeadSizeRatio(float ratio) {
        this.headSizeRatio = Math.max(0.5f, Math.min(2.0f, ratio));
        return this;
    }

    public CharacterSpecification setBodyBuildFactor(float factor) {
        this.bodyBuildFactor = Math.max(0.5f, Math.min(2.5f, factor));
        return this;
    }

    public CharacterSpecification setSkinColorHex(String skinColorHex) {
        if (skinColorHex != null && !skinColorHex.isEmpty()) {
            this.skinColorHex = skinColorHex;
        }
        return this;
    }

    public CharacterSpecification setRigRequired(boolean required) {
        this.isRigRequired = required;
        return this;
    }

    public CharacterSpecification setAnimationRequired(boolean required) {
        this.isAnimationRequired = required;
        return this;
    }
}