package com.example.engine;

import android.opengl.Matrix;

public class Transform {
    private float px = 0f, py = 0f, pz = 0f;
    private float rx = 0f, ry = 0f, rz = 0f;
    private float sx = 1f, sy = 1f, sz = 1f;

    private final float[] modelMatrix = new float[16];
    private final float[] worldMatrix = new float[16];
    private boolean isDirty = true;

    public Transform() {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(worldMatrix, 0);
    }

    public void setPosition(float x, float y, float z) {
        this.px = x; this.py = y; this.pz = z;
        this.isDirty = true;
    }

    public void setRotation(float xDegrees, float yDegrees, float zDegrees) {
        this.rx = xDegrees; this.ry = yDegrees; this.rz = zDegrees;
        this.isDirty = true;
    }

    public void setScale(float x, float y, float z) {
        this.sx = x; this.sy = y; this.sz = z;
        this.isDirty = true;
    }

    // Relative Incremental Transformations
    public void translate(float dx, float dy, float dz) {
        this.px += dx; this.py += dy; this.pz += dz;
        this.isDirty = true;
    }

    public void rotate(float drx, float dry, float drz) {
        this.rx += drx; this.ry += dry; this.rz += drz;
        this.isDirty = true;
    }

    public void scaleBy(float dsx, float dsy, float dsz) {
        this.sx *= dsx; this.sy *= dsy; this.sz *= dsz;
        this.isDirty = true;
    }

    public void reset() {
        this.px = 0f; this.py = 0f; this.pz = 0f;
        this.rx = 0f; this.ry = 0f; this.rz = 0f;
        this.sx = 1f; this.sy = 1f; this.sz = 1f;
        this.isDirty = true;
    }

    public float getPx() { return px; }
    public float getPy() { return py; }
    public float getPz() { return pz; }
    public float getRx() { return rx; }
    public float getRy() { return ry; }
    public float getRz() { return rz; }
    public float getSx() { return sx; }
    public float getSy() { return sy; }
    public float getSz() { return sz; }

    public float[] getModelMatrix() {
        if (isDirty) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, px, py, pz);
            Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f);
            Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f);
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz);
            isDirty = false;
        }
        return modelMatrix;
    }

    /**
     * Phase 15 Alignment: Computes the global world matrix by multiplying
     * parent node's world transform matrix with this node's local model matrix.
     */
    public float[] getWorldMatrix(float[] parentWorldMatrix) {
        float[] localMat = getModelMatrix();
        if (parentWorldMatrix == null) {
            System.arraycopy(localMat, 0, worldMatrix, 0, 16);
        } else {
            Matrix.multiplyMM(worldMatrix, 0, parentWorldMatrix, 0, localMat, 0);
        }
        return worldMatrix;
    }

    public Transform cloneTransform() {
        Transform copy = new Transform();
        copy.setPosition(this.px, this.py, this.pz);
        copy.setRotation(this.rx, this.ry, this.rz);
        copy.setScale(this.sx, this.sy, this.sz);
        return copy;
    }
}