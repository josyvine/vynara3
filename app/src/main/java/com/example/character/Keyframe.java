package com.example.character;

public class Keyframe {
    private float timestampSeconds;
    private float[] translation = new float[3];
    private float[] rotationDegrees = new float[3];
    private float[] scale = new float[] { 1f, 1f, 1f };

    public Keyframe(float time, float tx, float ty, float tz, float rx, float ry, float rz) {
        this(time, tx, ty, tz, rx, ry, rz, 1f, 1f, 1f);
    }

    public Keyframe(float time, float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) {
        this.timestampSeconds = Math.max(0.0f, time);
        this.translation[0] = tx; this.translation[1] = ty; this.translation[2] = tz;
        this.rotationDegrees[0] = rx; this.rotationDegrees[1] = ry; this.rotationDegrees[2] = rz;
        this.scale[0] = sx; this.scale[1] = sy; this.scale[2] = sz;
    }

    public float getTimestampSeconds() { return timestampSeconds; }
    public float[] getTranslation() { return translation; }
    public float[] getRotationDegrees() { return rotationDegrees; }
    public float[] getScale() { return scale; }

    public void setTimestampSeconds(float timestamp) {
        this.timestampSeconds = Math.max(0.0f, timestamp);
    }

    public void setTranslation(float tx, float ty, float tz) {
        this.translation[0] = tx; this.translation[1] = ty; this.translation[2] = tz;
    }

    public void setRotationDegrees(float rx, float ry, float rz) {
        this.rotationDegrees[0] = rx; this.rotationDegrees[1] = ry; this.rotationDegrees[2] = rz;
    }

    public void setScale(float sx, float sy, float sz) {
        this.scale[0] = sx; this.scale[1] = sy; this.scale[2] = sz;
    }

    public Keyframe cloneKeyframe() {
        return new Keyframe(timestampSeconds, 
                translation[0], translation[1], translation[2], 
                rotationDegrees[0], rotationDegrees[1], rotationDegrees[2], 
                scale[0], scale[1], scale[2]);
    }
}