package com.example.ui;

public class ProjectItem {
    private String id;
    private String name;
    private String prompt;
    private String status;
    private String info;

    public ProjectItem(String id, String name, String prompt, String status, String info) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.status = status;
        this.info = info;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrompt() { return prompt; }
    public String getStatus() { return status; }
    public String getInfo() { return info; }
}
