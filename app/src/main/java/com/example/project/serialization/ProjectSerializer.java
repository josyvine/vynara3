package com.example.project.serialization;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.project.Project;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectSerializer {

    /**
     * Phase 14 Alignment: Serializes the active Project metadata, Scene graph hierarchy nodes,
     * flat scene objects, and PBR materials into a structured, persistent JSON string.
     */
    public static String serialize(Project project, Scene scene, MaterialManager matMgr) {
        if (project == null || scene == null) return "{}";

        try {
            JSONObject root = new JSONObject();

            // 1. Serialize Project Metadata
            JSONObject meta = new JSONObject();
            meta.put("id", project.getId());
            meta.put("title", project.getTitle());
            meta.put("userPrompt", project.getUserPrompt());
            meta.put("type", project.getType());
            meta.put("status", project.getStatus());
            meta.put("polyCount", project.getPolyCount());
            meta.put("lastModifiedMs", project.getLastModifiedMs());
            meta.put("style", project.getStyle());
            meta.put("targetEngine", project.getTargetEngine());
            root.put("projectMetadata", meta);

            // 2. Serialize Scene Metadata
            JSONObject sceneJson = new JSONObject();
            sceneJson.put("id", scene.getId());
            sceneJson.put("name", scene.getName());
            root.put("sceneMetadata", sceneJson);

            // 3. Serialize All PBR Materials (Flat array registration for lookup map)
            JSONArray matArr = new JSONArray();
            if (matMgr != null) {
                for (Material mat : matMgr.getAllMaterials().values()) {
                    if (mat == null) continue;
                    JSONObject matJson = new JSONObject();
                    matJson.put("id", mat.getId());
                    matJson.put("name", mat.getName());
                    matJson.put("metallic", mat.getMetallic());
                    matJson.put("roughness", mat.getRoughness());
                    matJson.put("opacity", mat.getOpacity());
                    matJson.put("ambientOcclusion", mat.getAmbientOcclusion());
                    matJson.put("emissionIntensity", mat.getEmissionIntensity());
                    
                    float[] rgb = mat.getEmissionRGB();
                    JSONArray rgbArr = new JSONArray();
                    rgbArr.put(rgb[0]); rgbArr.put(rgb[1]); rgbArr.put(rgb[2]);
                    matJson.put("emissionRGB", rgbArr);

                    float[] rgba = mat.getBaseColorRGBA();
                    JSONArray rgbaArr = new JSONArray();
                    rgbaArr.put(rgba[0]); rgbaArr.put(rgba[1]); rgbaArr.put(rgba[2]); rgbaArr.put(rgba[3]);
                    matJson.put("baseColorRGBA", rgbaArr);

                    // PBR Texture Maps URIs
                    matJson.put("albedoTextureMap", mat.getAlbedoTextureMap() != null ? mat.getAlbedoTextureMap() : JSONObject.NULL);
                    matJson.put("normalMap", mat.getNormalMap() != null ? mat.getNormalMap() : JSONObject.NULL);
                    matJson.put("roughnessMap", mat.getRoughnessMap() != null ? mat.getRoughnessMap() : JSONObject.NULL);
                    matJson.put("metallicMap", mat.getMetallicMap() != null ? mat.getMetallicMap() : JSONObject.NULL);
                    matJson.put("aoMap", mat.getAoMap() != null ? mat.getAoMap() : JSONObject.NULL);

                    matArr.put(matJson);
                }
            }
            root.put("materials", matArr);

            // 4. Serialize Flat Scene Graph Nodes with Parent ID links
            JSONArray nodesArr = new JSONArray();
            for (SceneObject obj : scene.getFlatObjectList()) {
                if (obj == null) continue;
                JSONObject objJson = new JSONObject();
                objJson.put("id", obj.getId());
                objJson.put("name", obj.getName());
                objJson.put("semanticType", obj.getSemanticType());
                objJson.put("visible", obj.isVisible());

                // Hierarchy Reference Link
                if (obj.getParent() != null) {
                    objJson.put("parentId", obj.getParent().getId());
                } else {
                    objJson.put("parentId", JSONObject.NULL);
                }

                // Transform TRS values
                if (obj.getTransform() != null) {
                    JSONObject tJson = new JSONObject();
                    tJson.put("px", obj.getTransform().getPx());
                    tJson.put("py", obj.getTransform().getPy());
                    tJson.put("pz", obj.getTransform().getPz());
                    
                    tJson.put("rx", obj.getTransform().getRx());
                    tJson.put("ry", obj.getTransform().getRy());
                    tJson.put("rz", obj.getTransform().getRz());

                    tJson.put("sx", obj.getTransform().getSx());
                    tJson.put("sy", obj.getTransform().getSy());
                    tJson.put("sz", obj.getTransform().getSz());

                    objJson.put("transform", tJson);
                }

                // Material Reference Link
                if (obj.getMaterial() != null) {
                    objJson.put("materialId", obj.getMaterial().getId());
                } else {
                    objJson.put("materialId", JSONObject.NULL);
                }

                // Mesh Metadata and Boundary Bounds
                if (obj.getMesh() != null) {
                    JSONObject meshJson = new JSONObject();
                    meshJson.put("vertexCount", obj.getMesh().getVertexCount());
                    meshJson.put("triangleCount", obj.getMesh().getTriangleCount());
                    
                    JSONArray minArr = new JSONArray();
                    float[] minBounds = obj.getMesh().getMinBounds();
                    minArr.put(minBounds[0]); minArr.put(minBounds[1]); minArr.put(minBounds[2]);
                    meshJson.put("minBounds", minArr);

                    JSONArray maxArr = new JSONArray();
                    float[] maxBounds = obj.getMesh().getMaxBounds();
                    maxArr.put(maxBounds[0]); maxArr.put(maxBounds[1]); maxArr.put(maxBounds[2]);
                    meshJson.put("maxBounds", maxArr);

                    objJson.put("meshMetadata", meshJson);
                }

                nodesArr.put(objJson);
            }
            root.put("sceneNodes", nodesArr);

            return root.toString(2);

        } catch (Exception e) {
            return "{\"error\":\"Serialization failed: " + e.getMessage() + "\"}";
        }
    }
}