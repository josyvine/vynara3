package com.example.tools;

import java.util.HashMap;
import java.util.Map;

public class ToolOperation {
    private String toolId;
    private Map<String, Object> parameters;

    public ToolOperation(String toolId) {
        this.toolId = toolId;
        this.parameters = new HashMap<>();
    }

    public ToolOperation(String toolId, Map<String, Object> parameters) {
        this.toolId = toolId;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public String getToolId() { return toolId; }
    public Map<String, Object> getParameters() { return parameters; }

    public ToolOperation setParam(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    public Object getParam(String key, Object defaultValue) {
        if (parameters.containsKey(key)) {
            return parameters.get(key);
        }
        return defaultValue;
    }

    public String getStringParam(String key, String defaultValue) {
        Object val = parameters.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    public float getFloatParam(String key, float defaultValue) {
        Object val = parameters.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        } else if (val != null) {
            try { return Float.parseFloat(val.toString()); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public int getIntParam(String key, int defaultValue) {
        Object val = parameters.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return defaultValue;
    }
}
