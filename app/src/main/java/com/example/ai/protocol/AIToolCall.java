package com.example.ai.protocol;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AIToolCall {
    private String toolId;
    private String description;
    private final Map<String, Object> parameters = new HashMap<>();

    public AIToolCall() {
    }

    public AIToolCall(String toolId, String description) {
        this.toolId = toolId;
        this.description = description;
    }

    /**
     * Phase 2 Alignment: Parses a single tool instruction step from Gemini JSON.
     */
    public static AIToolCall fromJson(JSONObject json) {
        if (json == null) return null;

        String id = json.optString("toolId", "");
        String desc = json.optString("description", "");
        AIToolCall call = new AIToolCall(id, desc);

        JSONObject paramsObj = json.optJSONObject("parameters");
        if (paramsObj != null) {
            Iterator<String> keys = paramsObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = paramsObj.opt(key);
                if (val != null) {
                    call.parameters.put(key, val);
                }
            }
        }

        return call;
    }

    public String getToolId() { return toolId; }
    public String getDescription() { return description; }
    public Map<String, Object> getParameters() { return parameters; }

    public void setToolId(String toolId) { this.toolId = toolId; }
    public void setDescription(String description) { this.description = description; }
}