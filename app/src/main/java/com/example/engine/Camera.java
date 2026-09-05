package com.example.engine;

import android.opengl.Matrix;

public class Camera {
    private float[] eye = new float[] { 0f, 4f, 8f };
    private float[] target = new float[] { 0f, 1f, 0f };
    private float[] up = new float[] { 0f, 1f, 0f };

    private float fov = 45f;
    private float near = 0.1f;
    private float far = 500f; // Extended far plane to prevent large scenes (villas, villages) from clipping
    private int viewportWidth = 1080;
    private int viewportHeight = 1920;

    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];

    public Camera() {
        updateViewMatrix();
        updateProjectionMatrix(viewportWidth, viewportHeight);
    }

    public void updateViewMatrix() {
        Matrix.setLookAtM(viewMatrix, 0, eye[0], eye[1], eye[2], target[0], target[1], target[2], up[0], up[1], up[2]);
        updateViewProjectionMatrix();
    }

    public void updateProjectionMatrix(int width, int height) {
        this.viewportWidth = width > 0 ? width : 1;
        this.viewportHeight = height > 0 ? height : 1;
        float aspect = (float) this.viewportWidth / (float) this.viewportHeight;
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, near, far);
        updateViewProjectionMatrix();
    }

    private void updateViewProjectionMatrix() {
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setEye(float x, float y, float z) {
        eye[0] = x; eye[1] = y; eye[2] = z;
        updateViewMatrix();
    }

    public void setTarget(float x, float y, float z) {
        target[0] = x; target[1] = y; target[2] = z;
        updateViewMatrix();
    }

    public void setUp(float x, float y, float z) {
        up[0] = x; up[1] = y; up[2] = z;
        updateViewMatrix();
    }

    public void setFov(float fovDegrees) {
        this.fov = Math.max(10f, Math.min(120f, fovDegrees));
        updateProjectionMatrix(viewportWidth, viewportHeight);
    }

    public void setClippingPlanes(float nearPlane, float farPlane) {
        this.near = Math.max(0.01f, nearPlane);
        this.far = Math.max(near + 1.0f, farPlane);
        updateProjectionMatrix(viewportWidth, viewportHeight);
    }

    /**
     * Modulates current camera distance toward or away from the target.
     * factor > 1 zooms in (decreases distance), factor < 1 zooms out.
     */
    public void zoom(float factor) {
        if (factor <= 0.001f) return;
        float currentDist = getDistance();
        float newDist = Math.max(1.0f, Math.min(300.0f, currentDist / factor));
        setDistance(newDist);
    }

    /**
     * Calculates the direct Euclidean distance between camera eye and look target.
     */
    public float getDistance() {
        float dx = eye[0] - target[0];
        float dy = eye[1] - target[1];
        float dz = eye[2] - target[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Scales the eye position relative to the target to achieve the specified distance.
     */
    public void setDistance(float newDistance) {
        float currentDist = getDistance();
        if (currentDist < 0.0001f) {
            eye[0] = target[0];
            eye[1] = target[1] + 4.0f;
            eye[2] = target[2] + newDistance;
            updateViewMatrix();
            return;
        }

        float ratio = newDistance / currentDist;
        eye[0] = target[0] + (eye[0] - target[0]) * ratio;
        eye[1] = target[1] + (eye[1] - target[1]) * ratio;
        eye[2] = target[2] + (eye[2] - target[2]) * ratio;
        updateViewMatrix();
    }

    /**
     * Performs a stable spherical orbit rotation around the look target without gimbal lock.
     */
    public void orbit(float deltaYaw, float deltaPitch) {
        float relX = eye[0] - target[0];
        float relY = eye[1] - target[1];
        float relZ = eye[2] - target[2];

        float radius = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        if (radius < 0.001f) radius = 5.0f;

        float yaw = (float) Math.atan2(relZ, relX);
        float pitch = (float) Math.asin(Math.max(-0.99f, Math.min(0.99f, relY / radius)));

        yaw += deltaYaw;
        pitch += deltaPitch;

        float maxPitch = 1.52f; // ~87 degrees vertical limit to avoid pole flip
        if (pitch > maxPitch) pitch = maxPitch;
        if (pitch < -maxPitch) pitch = -maxPitch;

        eye[0] = target[0] + radius * (float) (Math.cos(pitch) * Math.cos(yaw));
        eye[1] = target[1] + radius * (float) Math.sin(pitch);
        eye[2] = target[2] + radius * (float) (Math.cos(pitch) * Math.sin(yaw));

        updateViewMatrix();
    }

    /**
     * Translates both camera eye and look target across the viewport plane.
     */
    public void pan(float deltaX, float deltaY) {
        float[] forward = new float[] { target[0] - eye[0], target[1] - eye[1], target[2] - eye[2] };
        float fLen = (float) Math.sqrt(forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2]);
        if (fLen > 0.001f) {
            forward[0] /= fLen; forward[1] /= fLen; forward[2] /= fLen;
        }

        // Calculate right vector = normalize(cross(forward, up))
        float[] right = new float[] {
                forward[1] * up[2] - forward[2] * up[1],
                forward[2] * up[0] - forward[0] * up[2],
                forward[0] * up[1] - forward[1] * up[0]
        };
        float rLen = (float) Math.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
        if (rLen > 0.001f) {
            right[0] /= rLen; right[1] /= rLen; right[2] /= rLen;
        }

        eye[0] += right[0] * deltaX + up[0] * deltaY;
        eye[1] += right[1] * deltaX + up[1] * deltaY;
        eye[2] += right[2] * deltaX + up[2] * deltaY;

        target[0] += right[0] * deltaX + up[0] * deltaY;
        target[1] += right[1] * deltaX + up[1] * deltaY;
        target[2] += right[2] * deltaX + up[2] * deltaY;

        updateViewMatrix();
    }

    /**
     * Resets camera to standard viewport default perspective.
     */
    public void reset() {
        eye[0] = 0f; eye[1] = 4f; eye[2] = 8f;
        target[0] = 0f; target[1] = 1f; target[2] = 0f;
        up[0] = 0f; up[1] = 1f; up[2] = 0f;
        updateViewMatrix();
    }

    /**
     * Phase 16 Alignment: Frames camera eye and look target around a 3D bounding box.
     */
    public void frameBounds(float[] minBounds, float[] maxBounds) {
        if (minBounds == null || maxBounds == null || minBounds.length < 3 || maxBounds.length < 3) return;

        float centerX = (minBounds[0] + maxBounds[0]) / 2f;
        float centerY = (minBounds[1] + maxBounds[1]) / 2f;
        float centerZ = (minBounds[2] + maxBounds[2]) / 2f;

        float sizeX = maxBounds[0] - minBounds[0];
        float sizeY = maxBounds[1] - minBounds[1];
        float sizeZ = maxBounds[2] - minBounds[2];
        float maxExtent = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (maxExtent < 0.1f) maxExtent = 2.0f;

        float distance = (float) (maxExtent / Math.tan(Math.toRadians(fov / 2.0)));
        distance = Math.max(3.0f, distance * 1.5f);

        // Dynamically push far clipping plane if the framed scene requires greater distance
        if (distance * 2.5f > far) {
            setClippingPlanes(near, distance * 3.0f);
        }

        setTarget(centerX, centerY, centerZ);
        setEye(centerX, centerY + distance * 0.4f, centerZ + distance);
    }

    public float[] getEye() { return eye; }
    public float[] getTarget() { return target; }
    public float[] getUp() { return up; }
    public float getFov() { return fov; }
    public float getNear() { return near; }
    public float getFar() { return far; }
    
    public float[] getViewMatrix() { return viewMatrix; }
    public float[] getProjectionMatrix() { return projectionMatrix; }
    public float[] getViewProjectionMatrix() { return viewProjectionMatrix; }
}