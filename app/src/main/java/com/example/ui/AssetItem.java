package com.example.ui;

public class AssetItem {
    private String id;
    private String name;
    private String category;
    private String icon;
    private String details;

    public AssetItem(String id, String name, String category, String icon, String details) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.icon = icon;
        this.details = details;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getIcon() { return icon; }
    public String getDetails() { return details; }
}
