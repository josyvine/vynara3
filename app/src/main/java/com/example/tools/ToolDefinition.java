package com.example.tools;

import java.util.ArrayList;
import java.util.List;

public class ToolDefinition {
    public enum AvailabilityState { AVAILABLE, PLANNED, DISABLED, EXPERIMENTAL }

    private String id;
    private String name;
    private String category; // GEOMETRY, MATERIAL, CHARACTER, SKELETON, RIG, ANIMATION, LIGHTING, CAMERA, SCENE, VALIDATION
    private String description;
    private AvailabilityState state;
    private List<ToolParameter> parameters = new ArrayList<>();

    public ToolDefinition(String id, String name, String category, String description, AvailabilityState state) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.state = state;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public AvailabilityState getState() { return state; }
    public boolean isAvailable() { return state == AvailabilityState.AVAILABLE; }
    public List<ToolParameter> getParameters() { return parameters; }

    public ToolDefinition addParam(String paramName, String paramType, boolean required, String paramDesc) {
        parameters.add(new ToolParameter(paramName, paramType, required, paramDesc));
        return this;
    }
}
