package com.example.ai;

public class AIModel {
    private String name;
    private String displayName;
    private String description;
    private boolean isAvailable;

    public AIModel(String name, String displayName, String description, boolean isAvailable) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.isAvailable = isAvailable;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isAvailable() { return isAvailable; }

    @Override
    public String toString() {
        return displayName != null ? displayName : name;
    }
}
