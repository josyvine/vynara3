package com.example.knowledge;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeEntry {
    private String id;
    private String name;
    private String category; // PHYSICAL_OBJECT, CHARACTER, ANIMAL, ARCHITECTURE, ENVIRONMENT, VEHICLE, FURNITURE
    private String proceduralGeneratorType;
    private float defaultHeight = 1.0f;
    private float defaultWidth = 1.0f;
    private float defaultDepth = 1.0f;
    private String designStyle = "REALISTIC";
    
    private final List<String> components;
    private final List<String> requiredCapabilities;
    private final List<String> defaultMaterials;

    public KnowledgeEntry(String id, String name, String category) {
        this.id = id != null ? id : "entry";
        this.name = name != null ? name : "Domain Concept";
        this.category = category != null ? category : "PHYSICAL_OBJECT";
        this.components = new ArrayList<>();
        this.requiredCapabilities = new ArrayList<>();
        this.defaultMaterials = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getProceduralGeneratorType() { return proceduralGeneratorType; }
    public float getDefaultHeight() { return defaultHeight; }
    public float getDefaultWidth() { return defaultWidth; }
    public float getDefaultDepth() { return defaultDepth; }
    public String getDesignStyle() { return designStyle; }
    
    public List<String> getComponents() { return components; }
    public List<String> getRequiredCapabilities() { return requiredCapabilities; }
    public List<String> getDefaultMaterials() { return defaultMaterials; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    
    public KnowledgeEntry setProceduralGeneratorType(String generatorType) {
        this.proceduralGeneratorType = generatorType;
        return this;
    }

    public KnowledgeEntry setDefaultDimensions(float width, float height, float depth) {
        this.defaultWidth = Math.max(0.01f, width);
        this.defaultHeight = Math.max(0.01f, height);
        this.defaultDepth = Math.max(0.01f, depth);
        return this;
    }

    public KnowledgeEntry setDesignStyle(String style) {
        this.designStyle = style != null ? style.toUpperCase() : "REALISTIC";
        return this;
    }

    public KnowledgeEntry addComponent(String comp) {
        if (comp != null && !comp.trim().isEmpty() && !components.contains(comp)) {
            components.add(comp.trim());
        }
        return this;
    }

    public KnowledgeEntry addCapability(String cap) {
        if (cap != null && !cap.trim().isEmpty() && !requiredCapabilities.contains(cap)) {
            requiredCapabilities.add(cap.trim());
        }
        return this;
    }

    public KnowledgeEntry addMaterial(String mat) {
        if (mat != null && !mat.trim().isEmpty() && !defaultMaterials.contains(mat)) {
            defaultMaterials.add(mat.trim());
        }
        return this;
    }

    public KnowledgeEntry cloneEntry() {
        KnowledgeEntry copy = new KnowledgeEntry(this.id, this.name, this.category);
        copy.setProceduralGeneratorType(this.proceduralGeneratorType);
        copy.setDefaultDimensions(this.defaultWidth, this.defaultHeight, this.defaultDepth);
        copy.setDesignStyle(this.designStyle);
        
        for (String c : this.components) {
            copy.addComponent(c);
        }
        for (String cap : this.requiredCapabilities) {
            copy.addCapability(cap);
        }
        for (String m : this.defaultMaterials) {
            copy.addMaterial(m);
        }
        return copy;
    }
}