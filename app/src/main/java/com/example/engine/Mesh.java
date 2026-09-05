package com.example.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Mesh {
    private float[] vertices;
    private float[] normals;
    private float[] texCoords;
    private float[] colors;
    private float[] boneWeights;
    private float[] boneIndices;
    private short[] indices;

    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer texBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer boneWeightBuffer;
    private FloatBuffer boneIndexBuffer;
    private ShortBuffer indexBuffer;

    // Bounding Box Dimensions
    private float[] minBounds = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
    private float[] maxBounds = new float[] { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

    public Mesh(float[] vertices, float[] normals, float[] texCoords, short[] indices) {
        this(vertices, normals, texCoords, null, null, null, indices);
    }

    public Mesh(float[] vertices, float[] normals, float[] texCoords, float[] colors, 
                float[] boneWeights, float[] boneIndices, short[] indices) {
        this.vertices = vertices;
        this.normals = normals;
        this.texCoords = texCoords;
        this.colors = colors;
        this.boneWeights = boneWeights;
        this.boneIndices = boneIndices;
        this.indices = indices;
        
        if (this.normals == null || this.normals.length == 0) {
            recalculateNormals();
        }
        
        calculateBounds();
        initBuffers();
    }

    private void initBuffers() {
        if (vertices != null && vertices.length > 0) {
            ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
            vbb.order(ByteOrder.nativeOrder());
            vertexBuffer = vbb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);
        }

        if (normals != null && normals.length > 0) {
            ByteBuffer nbb = ByteBuffer.allocateDirect(normals.length * 4);
            nbb.order(ByteOrder.nativeOrder());
            normalBuffer = nbb.asFloatBuffer();
            normalBuffer.put(normals);
            normalBuffer.position(0);
        }

        if (texCoords != null && texCoords.length > 0) {
            ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
            tbb.order(ByteOrder.nativeOrder());
            texBuffer = tbb.asFloatBuffer();
            texBuffer.put(texCoords);
            texBuffer.position(0);
        }

        if (colors != null && colors.length > 0) {
            ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
            cbb.order(ByteOrder.nativeOrder());
            colorBuffer = cbb.asFloatBuffer();
            colorBuffer.put(colors);
            colorBuffer.position(0);
        }

        if (boneWeights != null && boneWeights.length > 0) {
            ByteBuffer bwbb = ByteBuffer.allocateDirect(boneWeights.length * 4);
            bwbb.order(ByteOrder.nativeOrder());
            boneWeightBuffer = bwbb.asFloatBuffer();
            boneWeightBuffer.put(boneWeights);
            boneWeightBuffer.position(0);
        }

        if (boneIndices != null && boneIndices.length > 0) {
            ByteBuffer bibb = ByteBuffer.allocateDirect(boneIndices.length * 4);
            bibb.order(ByteOrder.nativeOrder());
            boneIndexBuffer = bibb.asFloatBuffer();
            boneIndexBuffer.put(boneIndices);
            boneIndexBuffer.position(0);
        }

        if (indices != null && indices.length > 0) {
            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asShortBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);
        }
    }

    /**
     * Dynamically binds raw skeletal float arrays and allocates Direct Native NIO 
     * buffers on the fly to support GPU-accelerated vertex skinning deformations.
     */
    public void setSkinningData(float[] boneWeights, float[] boneIndices) {
        this.boneWeights = boneWeights;
        this.boneIndices = boneIndices;

        if (boneWeights != null && boneWeights.length > 0) {
            ByteBuffer bwbb = ByteBuffer.allocateDirect(boneWeights.length * 4);
            bwbb.order(ByteOrder.nativeOrder());
            boneWeightBuffer = bwbb.asFloatBuffer();
            boneWeightBuffer.put(boneWeights);
            boneWeightBuffer.position(0);
        } else {
            boneWeightBuffer = null;
        }

        if (boneIndices != null && boneIndices.length > 0) {
            ByteBuffer bibb = ByteBuffer.allocateDirect(boneIndices.length * 4);
            bibb.order(ByteOrder.nativeOrder());
            boneIndexBuffer = bibb.asFloatBuffer();
            boneIndexBuffer.put(boneIndices);
            boneIndexBuffer.position(0);
        } else {
            boneIndexBuffer = null;
        }
    }

    public void recalculateNormals() {
        if (vertices == null || vertices.length < 9) return;

        normals = new float[vertices.length];
        
        if (indices != null && indices.length >= 3) {
            for (int i = 0; i < indices.length; i += 3) {
                int idx0 = indices[i] * 3;
                int idx1 = indices[i + 1] * 3;
                int idx2 = indices[i + 2] * 3;

                float ax = vertices[idx1] - vertices[idx0];
                float ay = vertices[idx1 + 1] - vertices[idx0 + 1];
                float az = vertices[idx1 + 2] - vertices[idx0 + 2];

                float bx = vertices[idx2] - vertices[idx0];
                float by = vertices[idx2 + 1] - vertices[idx0 + 1];
                float bz = vertices[idx2 + 2] - vertices[idx0 + 2];

                float nx = ay * bz - az * by;
                float ny = az * bx - ax * bz;
                float nz = ax * by - ay * bx;

                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) { nx /= len; ny /= len; nz /= len; }

                normals[idx0] = nx; normals[idx0 + 1] = ny; normals[idx0 + 2] = nz;
                normals[idx1] = nx; normals[idx1 + 1] = ny; normals[idx1 + 2] = nz;
                normals[idx2] = nx; normals[idx2 + 2] = nz;
            }
        } else {
            // Default flat normals facing Z
            for (int i = 0; i < normals.length; i += 3) {
                normals[i] = 0f; normals[i + 1] = 0f; normals[i + 2] = 1f;
            }
        }
    }

    private void calculateBounds() {
        if (vertices == null || vertices.length == 0) return;

        minBounds = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
        maxBounds = new float[] { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i], y = vertices[i + 1], z = vertices[i + 2];

            if (x < minBounds[0]) minBounds[0] = x;
            if (y < minBounds[1]) minBounds[1] = y;
            if (z < minBounds[2]) minBounds[2] = z;

            if (x > maxBounds[0]) maxBounds[0] = x;
            if (y > maxBounds[1]) maxBounds[1] = y;
            if (z > maxBounds[2]) maxBounds[2] = z;
        }
    }

    public float[] getVertices() { return vertices; }
    public float[] getNormals() { return normals; }
    public float[] getTexCoords() { return texCoords; }
    public float[] getColors() { return colors; }
    public float[] getBoneWeights() { return boneWeights; }
    public float[] getBoneIndices() { return boneIndices; }
    public short[] getIndices() { return indices; }

    public FloatBuffer getVertexBuffer() { return vertexBuffer; }
    public FloatBuffer getNormalBuffer() { return normalBuffer; }
    public FloatBuffer getTexBuffer() { return texBuffer; }
    public FloatBuffer getColorBuffer() { return colorBuffer; }
    public FloatBuffer getBoneWeightBuffer() { return boneWeightBuffer; }
    public FloatBuffer getBoneIndexBuffer() { return boneIndexBuffer; }
    public ShortBuffer getIndexBuffer() { return indexBuffer; }

    public float[] getMinBounds() { return minBounds; }
    public float[] getMaxBounds() { return maxBounds; }

    public int getVertexCount() {
        return vertices != null ? vertices.length / 3 : 0;
    }

    public int getTriangleCount() {
        return indices != null ? indices.length / 3 : getVertexCount() / 3;
    }
}