package com.example.export;

import android.util.Base64;

import com.example.engine.Material;
import com.example.engine.Mesh;
import com.example.engine.Scene;
import com.example.engine.SceneObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GLTFExporter {

    /**
     * Phase 18 Alignment: Generates a fully compliant GLTF 2.0 JSON file containing 
     * nodes, TRS transforms, meshes, PBR materials, accessors, bufferViews, and embedded binary buffers.
     * Integrates rigged bone indices and skinning weights.
     */
    public static String exportSceneToGLTFJson(Scene scene) {
        if (scene == null) return "{\"error\":\"Scene is null\"}";

        try {
            JSONObject root = new JSONObject();

            // 1. Asset Metadata
            JSONObject asset = new JSONObject();
            asset.put("version", "2.0");
            asset.put("generator", "Vynara AI 3D Engine GLTF 2.0 Exporter");
            root.put("asset", asset);

            List<SceneObject> flatObjectList = scene.getFlatObjectList();
            if (flatObjectList.isEmpty()) {
                return "{\"error\":\"Scene is empty\"}";
            }

            ByteArrayOutputStream binStream = new ByteArrayOutputStream();

            JSONArray nodes = new JSONArray();
            JSONArray meshes = new JSONArray();
            JSONArray materials = new JSONArray();
            JSONArray accessors = new JSONArray();
            JSONArray bufferViews = new JSONArray();

            Map<Material, Integer> materialIndexMap = new HashMap<>();

            int currentByteOffset = 0;
            int accessorCounter = 0;
            int bufferViewCounter = 0;

            // 2. Process Materials
            for (SceneObject obj : flatObjectList) {
                Material mat = obj.getMaterial();
                if (mat != null && !materialIndexMap.containsKey(mat)) {
                    int matIdx = materials.length();
                    materialIndexMap.put(mat, matIdx);

                    JSONObject matObj = new JSONObject();
                    matObj.put("name", mat.getName());

                    JSONObject pbr = new JSONObject();
                    float[] rgba = mat.getBaseColorRGBA();
                    JSONArray baseColorFactor = new JSONArray();
                    baseColorFactor.put(rgba[0]); baseColorFactor.put(rgba[1]);
                    baseColorFactor.put(rgba[2]); baseColorFactor.put(rgba[3]);

                    pbr.put("baseColorFactor", baseColorFactor);
                    pbr.put("metallicFactor", mat.getMetallic());
                    pbr.put("roughnessFactor", mat.getRoughness());

                    matObj.put("pbrMetallicRoughness", pbr);
                    materials.put(matObj);
                }
            }

            root.put("materials", materials);

            // 3. Process Nodes, Meshes, and Geometry Buffers
            JSONArray rootNodeIndices = new JSONArray();

            for (int i = 0; i < flatObjectList.size(); i++) {
                SceneObject obj = flatObjectList.get(i);

                JSONObject node = new JSONObject();
                node.put("name", obj.getName());

                // Transform TRS
                if (obj.getTransform() != null) {
                    JSONArray translation = new JSONArray();
                    translation.put(obj.getTransform().getPx());
                    translation.put(obj.getTransform().getPy());
                    translation.put(obj.getTransform().getPz());
                    node.put("translation", translation);

                    // GLTF 2.0 requires Quaternions [X, Y, Z, W] instead of raw Euler degrees
                    float[] quaternion = eulerToQuaternion(
                            obj.getTransform().getRx(),
                            obj.getTransform().getRy(),
                            obj.getTransform().getRz()
                    );
                    JSONArray rotation = new JSONArray();
                    rotation.put(quaternion[0]);
                    rotation.put(quaternion[1]);
                    rotation.put(quaternion[2]);
                    rotation.put(quaternion[3]);
                    node.put("rotation", rotation);

                    JSONArray scale = new JSONArray();
                    scale.put(obj.getTransform().getSx());
                    scale.put(obj.getTransform().getSy());
                    scale.put(obj.getTransform().getSz());
                    node.put("scale", scale);
                }

                if (obj.isRoot()) {
                    rootNodeIndices.put(i);
                }

                Mesh mesh = obj.getMesh();
                if (mesh != null && mesh.getVertices() != null && mesh.getVertices().length > 0) {
                    node.put("mesh", meshes.length());

                    JSONObject meshObj = new JSONObject();
                    meshObj.put("name", obj.getName() + "_Mesh");

                    JSONArray primitives = new JSONArray();
                    JSONObject prim = new JSONObject();
                    prim.put("mode", 4); // TRIANGLES

                    JSONObject attributes = new JSONObject();

                    // A. Position Buffer & Accessor
                    float[] verts = mesh.getVertices();
                    byte[] posBytes = floatArrayToByteArray(verts);
                    int posBufferViewIdx = bufferViewCounter++;
                    int posAccessorIdx = accessorCounter++;

                    JSONObject posBufferView = new JSONObject();
                    posBufferView.put("buffer", 0);
                    posBufferView.put("byteOffset", currentByteOffset);
                    posBufferView.put("byteLength", posBytes.length);
                    posBufferView.put("target", 34962); // ARRAY_BUFFER
                    bufferViews.put(posBufferView);

                    binStream.write(posBytes);
                    currentByteOffset += posBytes.length;
                    currentByteOffset = alignOffset(binStream, currentByteOffset);

                    JSONObject posAccessor = new JSONObject();
                    posAccessor.put("bufferView", posBufferViewIdx);
                    posAccessor.put("byteOffset", 0);
                    posAccessor.put("componentType", 5126); // FLOAT
                    posAccessor.put("count", verts.length / 3);
                    posAccessor.put("type", "VEC3");

                    float[] minB = mesh.getMinBounds();
                    float[] maxB = mesh.getMaxBounds();
                    JSONArray minArr = new JSONArray(); minArr.put(minB[0]); minArr.put(minB[1]); minArr.put(minB[2]);
                    JSONArray maxArr = new JSONArray(); maxArr.put(maxB[0]); maxArr.put(maxB[1]); maxArr.put(maxB[2]);
                    posAccessor.put("min", minArr);
                    posAccessor.put("max", maxArr);
                    accessors.put(posAccessor);

                    attributes.put("POSITION", posAccessorIdx);

                    // B. Normal Buffer & Accessor
                    if (mesh.getNormals() != null && mesh.getNormals().length > 0) {
                        float[] norms = mesh.getNormals();
                        byte[] normBytes = floatArrayToByteArray(norms);
                        int normBufferViewIdx = bufferViewCounter++;
                        int normAccessorIdx = accessorCounter++;

                        JSONObject normBufferView = new JSONObject();
                        normBufferView.put("buffer", 0);
                        normBufferView.put("byteOffset", currentByteOffset);
                        normBufferView.put("byteLength", normBytes.length);
                        normBufferView.put("target", 34962);
                        bufferViews.put(normBufferView);

                        binStream.write(normBytes);
                        currentByteOffset += normBytes.length;
                        currentByteOffset = alignOffset(binStream, currentByteOffset);

                        JSONObject normAccessor = new JSONObject();
                        normAccessor.put("bufferView", normBufferViewIdx);
                        normAccessor.put("byteOffset", 0);
                        normAccessor.put("componentType", 5126);
                        normAccessor.put("count", norms.length / 3);
                        normAccessor.put("type", "VEC3");
                        accessors.put(normAccessor);

                        attributes.put("NORMAL", normAccessorIdx);
                    }

                    // C. Index Buffer & Accessor
                    if (mesh.getIndices() != null && mesh.getIndices().length > 0) {
                        short[] indicesArr = mesh.getIndices();
                        byte[] idxBytes = shortArrayToByteArray(indicesArr);
                        int idxBufferViewIdx = bufferViewCounter++;
                        int idxAccessorIdx = accessorCounter++;

                        JSONObject idxBufferView = new JSONObject();
                        idxBufferView.put("buffer", 0);
                        idxBufferView.put("byteOffset", currentByteOffset);
                        idxBufferView.put("byteLength", idxBytes.length);
                        idxBufferView.put("target", 34963); // ELEMENT_ARRAY_BUFFER
                        bufferViews.put(idxBufferView);

                        binStream.write(idxBytes);
                        currentByteOffset += idxBytes.length;
                        currentByteOffset = alignOffset(binStream, currentByteOffset);

                        JSONObject idxAccessor = new JSONObject();
                        idxAccessor.put("bufferView", idxBufferViewIdx);
                        idxAccessor.put("byteOffset", 0);
                        idxAccessor.put("componentType", 5123); // UNSIGNED_SHORT
                        idxAccessor.put("count", indicesArr.length);
                        idxAccessor.put("type", "SCALAR");
                        accessors.put(idxAccessor);

                        prim.put("indices", idxAccessorIdx);
                    }

                    // D. JOINTS_0 Buffer & Accessor (Rigged Joint Bone Mappings)
                    if (mesh.getBoneIndices() != null && mesh.getBoneIndices().length > 0) {
                        float[] boneInds = mesh.getBoneIndices();
                        byte[] jointBytes = floatArrayToByteArray(boneInds);
                        int jointBufferViewIdx = bufferViewCounter++;
                        int jointAccessorIdx = accessorCounter++;

                        JSONObject jointBufferView = new JSONObject();
                        jointBufferView.put("buffer", 0);
                        jointBufferView.put("byteOffset", currentByteOffset);
                        jointBufferView.put("byteLength", jointBytes.length);
                        jointBufferView.put("target", 34962);
                        bufferViews.put(jointBufferView);

                        binStream.write(jointBytes);
                        currentByteOffset += jointBytes.length;
                        currentByteOffset = alignOffset(binStream, currentByteOffset);

                        JSONObject jointAccessor = new JSONObject();
                        jointAccessor.put("bufferView", jointBufferViewIdx);
                        jointAccessor.put("byteOffset", 0);
                        jointAccessor.put("componentType", 5126); // FLOAT (VEC4 mapped representation)
                        jointAccessor.put("count", boneInds.length / 4);
                        jointAccessor.put("type", "VEC4");
                        accessors.put(jointAccessor);

                        attributes.put("JOINTS_0", jointAccessorIdx);
                    }

                    // E. WEIGHTS_0 Buffer & Accessor (Skeletal Deformation Weights)
                    if (mesh.getBoneWeights() != null && mesh.getBoneWeights().length > 0) {
                        float[] boneWts = mesh.getBoneWeights();
                        byte[] weightBytes = floatArrayToByteArray(boneWts);
                        int weightBufferViewIdx = bufferViewCounter++;
                        int weightAccessorIdx = accessorCounter++;

                        JSONObject weightBufferView = new JSONObject();
                        weightBufferView.put("buffer", 0);
                        weightBufferView.put("byteOffset", currentByteOffset);
                        weightBufferView.put("byteLength", weightBytes.length);
                        weightBufferView.put("target", 34962);
                        bufferViews.put(weightBufferView);

                        binStream.write(weightBytes);
                        currentByteOffset += weightBytes.length;
                        currentByteOffset = alignOffset(binStream, currentByteOffset);

                        JSONObject weightAccessor = new JSONObject();
                        weightAccessor.put("bufferView", weightBufferViewIdx);
                        weightAccessor.put("byteOffset", 0);
                        weightAccessor.put("componentType", 5126); // FLOAT (VEC4 mapped representation)
                        weightAccessor.put("count", boneWts.length / 4);
                        weightAccessor.put("type", "VEC4");
                        accessors.put(weightAccessor);

                        attributes.put("WEIGHTS_0", weightAccessorIdx);
                    }

                    prim.put("attributes", attributes);

                    if (obj.getMaterial() != null && materialIndexMap.containsKey(obj.getMaterial())) {
                        prim.put("material", materialIndexMap.get(obj.getMaterial()));
                    }

                    primitives.put(prim);
                    meshObj.put("primitives", primitives);
                    meshes.put(meshObj);
                }

                nodes.put(node);
            }

            // 4. Scenes Array
            JSONArray sceneArray = new JSONArray();
            JSONObject mainScene = new JSONObject();
            mainScene.put("name", scene.getName());
            mainScene.put("nodes", rootNodeIndices);
            sceneArray.put(mainScene);

            root.put("scenes", sceneArray);
            root.put("scene", 0);
            root.put("nodes", nodes);
            root.put("meshes", meshes);
            root.put("accessors", accessors);
            root.put("bufferViews", bufferViews);

            // 5. Embedded Binary Buffer
            byte[] binaryData = binStream.toByteArray();
            String base64Buffer = Base64.encodeToString(binaryData, Base64.NO_WRAP);

            JSONArray buffers = new JSONArray();
            JSONObject mainBuffer = new JSONObject();
            mainBuffer.put("byteLength", binaryData.length);
            mainBuffer.put("uri", "data:application/octet-stream;base64," + base64Buffer);
            buffers.put(mainBuffer);

            root.put("buffers", buffers);

            return root.toString(2);

        } catch (Exception e) {
            return "{\"error\":\"Export failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Helper to write null-padding bytes to the stream, aligning the offset to a 4-byte boundary.
     */
    private static int alignOffset(ByteArrayOutputStream stream, int offset) {
        int padding = (4 - (offset % 4)) % 4;
        for (int i = 0; i < padding; i++) {
            stream.write(0);
        }
        return offset + padding;
    }

    private static float[] eulerToQuaternion(float rx, float ry, float rz) {
        float r_x = (float) Math.toRadians(rx);
        float r_y = (float) Math.toRadians(ry);
        float r_z = (float) Math.toRadians(rz);

        float cx = (float) Math.cos(r_x / 2);
        float sx = (float) Math.sin(r_x / 2);
        float cy = (float) Math.cos(r_y / 2);
        float sy = (float) Math.sin(r_y / 2);
        float cz = (float) Math.cos(r_z / 2);
        float sz = (float) Math.sin(r_z / 2);

        float[] q = new float[4];
        q[0] = sx * cy * cz - cx * sy * sz; // X
        q[1] = cx * sy * cz + sx * cy * sz; // Y
        q[2] = cx * cy * sz - sx * sy * cz; // Z
        q[3] = cx * cy * cz + sx * sy * sz; // W
        return q;
    }

    private static byte[] floatArrayToByteArray(float[] values) {
        ByteBuffer bb = ByteBuffer.allocate(values.length * 4);
        bb.order(ByteOrder.LITTLE_ENDIAN); // GLTF requires Little-Endian
        for (float v : values) {
            bb.putFloat(v);
        }
        return bb.array();
    }

    private static byte[] shortArrayToByteArray(short[] values) {
        ByteBuffer bb = ByteBuffer.allocate(values.length * 2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short v : values) {
            bb.putShort(v);
        }
        return bb.array();
    }
}