package com.example.engine;

import android.graphics.Color;

public class Light {
    public enum Type { DIRECTIONAL, POINT, SPOT, AMBIENT }

    private String id;
    private Type type;
    private float[] position = new float[] { 5f, 10f, 5f };
    private float[] direction = new float[] { 0f, -1f, 0f };
    private float[] colorRGB = new float[] { 1f, 1f, 1f };
    private float intensity = 1.0f;

    // Attenuation & Spot Parameters
    private float constantAttenuation = 1.0f;
    private float linearAttenuation = 0.09f;
    private float quadraticAttenuation = 0.032f;
    private float spotCutoffDegrees = 45.0f;

    public Light(String id, Type type) {
        this.id = id;
        this.type = type != null ? type : Type.DIRECTIONAL;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public float[] getPosition() { return position; }
    public float[] getDirection() { return direction; }
    public float[] getColorRGB() { return colorRGB; }
    public float getIntensity() { return intensity; }

    public float getConstantAttenuation() { return constantAttenuation; }
    public float getLinearAttenuation() { return linearAttenuation; }
    public float getQuadraticAttenuation() { return quadraticAttenuation; }
    public float getSpotCutoffDegrees() { return spotCutoffDegrees; }

    public void setPosition(float x, float y, float z) {
        position[0] = x; position[1] = y; position[2] = z;
    }

    public void setDirection(float dx, float dy, float dz) {
        direction[0] = dx; direction[1] = dy; direction[2] = dz;
    }

    public void setColor(float r, float g, float b) {
        colorRGB[0] = Math.max(0f, Math.min(1f, r));
        colorRGB[1] = Math.max(0f, Math.min(1f, g));
        colorRGB[2] = Math.max(0f, Math.min(1f, b));
    }

    public void setColorHex(String hexColor) {
        if (hexColor != null && !hexColor.isEmpty()) {
            try {
                int c = Color.parseColor(hexColor.startsWith("#") ? hexColor : "#" + hexColor);
                colorRGB[0] = Color.red(c) / 255f;
                colorRGB[1] = Color.green(c) / 255f;
                colorRGB[2] = Color.blue(c) / 255f;
            } catch (Exception ignored) {}
        }
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(0.0f, intensity);
    }

    public void setAttenuation(float constant, float linear, float quadratic) {
        this.constantAttenuation = Math.max(0.0f, constant);
        this.linearAttenuation = Math.max(0.0f, linear);
        this.quadraticAttenuation = Math.max(0.0f, quadratic);
    }

    public void setSpotCutoffDegrees(float cutoff) {
        this.spotCutoffDegrees = Math.max(1.0f, Math.min(180.0f, cutoff));
    }

    public Light cloneLight(String newId) {
        Light copy = new Light(newId, this.type);
        copy.setPosition(position[0], position[1], position[2]);
        copy.setDirection(direction[0], direction[1], direction[2]);
        copy.setColor(colorRGB[0], colorRGB[1], colorRGB[2]);
        copy.setIntensity(this.intensity);
        copy.setAttenuation(constantAttenuation, linearAttenuation, quadraticAttenuation);
        copy.setSpotCutoffDegrees(spotCutoffDegrees);
        return copy;
    }
}