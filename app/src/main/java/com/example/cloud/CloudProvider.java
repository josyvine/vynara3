package com.example.cloud;

public enum CloudProvider {
    LOCAL("local", "Local Engine (On-Device)"),
    GITHUB_ACTIONS("github_actions", "GitHub Actions (BYOC)"),
    HUGGING_FACE("hugging_face", "Hugging Face Space (Serverless)");

    private final String id;
    private final String displayName;

    CloudProvider(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CloudProvider fromId(String id) {
        if (id == null) {
            return LOCAL;
        }
        for (CloudProvider provider : values()) {
            if (provider.id.equalsIgnoreCase(id) || provider.name().equalsIgnoreCase(id)) {
                return provider;
            }
        }
        return LOCAL;
    }

    public boolean isCloud() {
        return this != LOCAL;
    }
}