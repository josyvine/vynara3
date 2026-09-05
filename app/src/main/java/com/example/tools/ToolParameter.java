package com.example.tools;

public class ToolParameter {
    private String name;
    private String type; // STRING, FLOAT, INT, BOOLEAN, MAP, VECTOR3
    private boolean required;
    private String description;

    public ToolParameter(String name, String type, boolean required, String description) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.description = description;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
    public String getDescription() { return description; }
}
