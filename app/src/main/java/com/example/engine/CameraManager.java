package com.example.engine;

public class CameraManager {
    private final Camera activeCamera;
    private float orbitAngleYaw = 0f;
    private float orbitAnglePitch = 20f;
    private float orbitDistance = 10f;
    private float targetOffsetX = 0f;
    private float targetOffsetY = 1f;
    private float targetOffsetZ = 0f;

    public CameraManager() {
        this.activeCamera = new Camera();
        updateOrbitCamera();
    }

    public void orbit(float deltaYaw, float deltaPitch) {
        orbitAngleYaw += deltaYaw;
        orbitAnglePitch += deltaPitch;

        if (orbitAnglePitch > 89f) orbitAnglePitch = 89f;
        if (orbitAnglePitch < -89f) orbitAnglePitch = -89f;

        updateOrbitCamera();
    }

    public void zoom(float factor) {
        orbitDistance *= factor;
        if (orbitDistance < 0.5f) orbitDistance = 0.5f;
        if (orbitDistance > 100f) orbitDistance = 100f;

        updateOrbitCamera();
    }

    /**
     * Phase 16 Alignment: Pans the camera target point across 3D viewport axes.
     */
    public void pan(float deltaX, float deltaY) {
        double yawRad = Math.toRadians(orbitAngleYaw);
        
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);

        targetOffsetX += (deltaX * cos - deltaY * sin) * (orbitDistance * 0.05f);
        targetOffsetZ += (deltaX * sin + deltaY * cos) * (orbitDistance * 0.05f);

        updateOrbitCamera();
    }

    public void setOrbitAngles(float yaw, float pitch) {
        this.orbitAngleYaw = yaw;
        this.orbitAnglePitch = Math.max(-89f, Math.min(89f, pitch));
        updateOrbitCamera();
    }

    public void setOrbitDistance(float distance) {
        this.orbitDistance = Math.max(0.5f, Math.min(100f, distance));
        updateOrbitCamera();
    }

    // Viewport View Presets
    public void setFrontView() {
        setOrbitAngles(0f, 0f);
    }

    public void setSideView() {
        setOrbitAngles(90f, 0f);
    }

    public void setTopView() {
        setOrbitAngles(0f, 89f);
    }

    public void resetToDefaultView() {
        this.targetOffsetX = 0f;
        this.targetOffsetY = 1f;
        this.targetOffsetZ = 0f;
        this.orbitAngleYaw = 0f;
        this.orbitAnglePitch = 20f;
        this.orbitDistance = 10f;
        updateOrbitCamera();
    }

    public void updateOrbitCamera() {
        double yawRad = Math.toRadians(orbitAngleYaw);
        double pitchRad = Math.toRadians(orbitAnglePitch);

        float x = (float) (orbitDistance * Math.cos(pitchRad) * Math.sin(yawRad)) + targetOffsetX;
        float y = (float) (orbitDistance * Math.sin(pitchRad)) + targetOffsetY;
        float z = (float) (orbitDistance * Math.cos(pitchRad) * Math.cos(yawRad)) + targetOffsetZ;

        activeCamera.setTarget(targetOffsetX, targetOffsetY, targetOffsetZ);
        activeCamera.setEye(x, y, z);
    }

    public Camera getActiveCamera() { return activeCamera; }
    public float getOrbitAngleYaw() { return orbitAngleYaw; }
    public float getOrbitAnglePitch() { return orbitAnglePitch; }
    public float getOrbitDistance() { return orbitDistance; }
}