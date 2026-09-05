package com.example.project.serialization;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.Mesh;
import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.project.Project;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProjectDeserializer {

    public static class DeserializedResult {
        public Project project;
        public Scene scene;
    }

    /**
     * Phase 14 Alignment: Deserializes project package JSON string, 
     * reconstructing project metadata, PBR materials, and parent-child scene graph node links.
     */
    public static DeserializedResult deserialize(String jsonStr, MaterialManager matMgr) {
        if (jsonStr == null || jsonStr.trim().isEmpty() || matMgr == null) return null;

        try {
            JSONObject root = new JSONObject(jsonStr);

            // 1. Reconstruct Project Metadata
            JSONObject meta = root.optJSONObject("projectMetadata");
            if (meta == null) return null;

            String projId = meta.optString("id");
            String title = meta.optString("title");
            String type = meta.optString("type");
            String status = meta.optString("status");
            int polyCount = meta.optInt("polyCount", 0);
            long lastModified = meta.optLong("lastModifiedMs", System.currentTimeMillis());

            Project project = new Project(projId, title, type, status, polyCount);
            project.setUserPrompt(meta.optString("userPrompt", ""));
            project.setStyle(meta.optString("style", "Photorealistic"));
            project.setTargetEngine(meta.optString("targetEngine", "OpenGL ES / GLTF"));
            project.setLastModifiedMs(lastModified);

            // 2. Reconstruct Scene Metadata
            JSONObject sceneMeta = root.optJSONObject("sceneMetadata");
            String sceneId = sceneMeta != null ? sceneMeta.optString("id", "scene_default") : "scene_default";
            String sceneName = sceneMeta != null ? sceneMeta.optString("name", "Scene") : "Scene";
            Scene scene = new Scene(sceneId, sceneName);

            // 3. Reconstruct & Register PBR Materials into MaterialManager
            JSONArray matArr = root.optJSONArray("materials");
            if (matArr != null) {
                for (int i = 0; i < matArr.length(); i++) {
                    JSONObject matJson = matArr.optJSONObject(i);
                    if (matJson == null) continue;

                    String matId = matJson.optString("id");
                    String matName = matJson.optString("name");
                    
                    JSONArray rgbaArr = matJson.optJSONArray("baseColorRGBA");
                    float r = 0.8f, g = 0.8f, b = 0.8f, a = 1.0f;
                    if (rgbaArr != null && rgbaArr.length() >= 4) {
                        r = (float) rgbaArr.optDouble(0, r);
                        g = (float) rgbaArr.optDouble(1, g);
                        b = (float) rgbaArr.optDouble(2, b);
                        a = (float) rgbaArr.optDouble(3, a);
                    }

                    Material mat = new Material(matId, matName, r, g, b, a);
                    mat.setMetallic((float) matJson.optDouble("metallic", 0.1f));
                    mat.setRoughness((float) matJson.optDouble("roughness", 0.5f));
                    mat.setOpacity((float) matJson.optDouble("opacity", 1.0f));
                    mat.setAmbientOcclusion((float) matJson.optDouble("ambientOcclusion", 1.0f));

                    JSONArray emissiveArr = matJson.optJSONArray("emissionRGB");
                    if (emissiveArr != null && emissiveArr.length() >= 3) {
                        float er = (float) emissiveArr.optDouble(0);
                        float eg = (float) emissiveArr.optDouble(1);
                        float eb = (float) emissiveArr.optDouble(2);
                        float intensity = (float) matJson.optDouble("emissionIntensity", 0.0f);
                        mat.setEmission(er, eg, eb, intensity);
                    }

                    // Restore custom PBR maps if they exist
                    if (!matJson.isNull("albedoTextureMap")) mat.setAlbedoTextureMap(matJson.optString("albedoTextureMap"));
                    if (!matJson.isNull("normalMap")) mat.setNormalMap(matJson.optString("normalMap"));
                    if (!matJson.isNull("roughnessMap")) mat.setRoughnessMap(matJson.optString("roughnessMap"));
                    if (!matJson.isNull("metallicMap")) mat.setMetallicMap(matJson.optString("metallicMap"));
                    if (!matJson.isNull("aoMap")) mat.setAoMap(matJson.optString("aoMap"));

                    matMgr.addMaterial(mat);
                }
            }

            // 4. Reconstruct Scene Nodes & Hierarchy Maps
            JSONArray nodesArr = root.optJSONArray("sceneNodes");
            if (nodesArr != null) {
                Map<String, SceneObject> nodeMap = new HashMap<>();
                Map<String, String> parentLinkMap = new HashMap<>();

                // First pass: Instantiate objects, restore transforms and materials
                for (int i = 0; i < nodesArr.length(); i++) {
                    JSONObject nodeJson = nodesArr.optJSONObject(i);
                    if (nodeJson == null) continue;

                    String objId = nodeJson.optString("id");
                    String objName = nodeJson.optString("name");
                    String semType = nodeJson.optString("semanticType", "PRIMITIVE");
                    boolean visible = nodeJson.optBoolean("visible", true);

                    Material boundMat = null;
                    String boundMatId = nodeJson.optString("materialId", null);
                    if (boundMatId != null && !boundMatId.equals("null")) {
                        boundMat = matMgr.getMaterial(boundMatId);
                    }

                    // Recover original bounding box dimensions from serialized metadata
                    float width = 1.5f, height = 1.5f, depth = 1.5f;
                    JSONObject meshMeta = nodeJson.optJSONObject("meshMetadata");
                    if (meshMeta != null) {
                        JSONArray minArr = meshMeta.optJSONArray("minBounds");
                        JSONArray maxArr = meshMeta.optJSONArray("maxBounds");
                        if (minArr != null && maxArr != null && minArr.length() >= 3 && maxArr.length() >= 3) {
                            width = (float) (maxArr.optDouble(0) - minArr.optDouble(0));
                            height = (float) (maxArr.optDouble(1) - minArr.optDouble(1));
                            depth = (float) (maxArr.optDouble(2) - minArr.optDouble(2));
                        }
                    }

                    Mesh rebuiltMesh = createBoxMesh(width > 0 ? width : 1.5f, height > 0 ? height : 1.5f, depth > 0 ? depth : 1.5f);

                    SceneObject obj = new SceneObject(objId, objName, semType, rebuiltMesh, boundMat);
                    obj.setVisible(visible);

                    // Restore TRS Transform
                    JSONObject tJson = nodeJson.optJSONObject("transform");
                    if (tJson != null) {
                        obj.getTransform().setPosition(
                                (float) tJson.optDouble("px", 0f),
                                (float) tJson.optDouble("py", 0f),
                                (float) tJson.optDouble("pz", 0f)
                        );
                        obj.getTransform().setRotation(
                                (float) tJson.optDouble("rx", 0f),
                                (float) tJson.optDouble("ry", 0f),
                                (float) tJson.optDouble("rz", 0f)
                        );
                        obj.getTransform().setScale(
                                (float) tJson.optDouble("sx", 1f),
                                (float) tJson.optDouble("sy", 1f),
                                (float) tJson.optDouble("sz", 1f)
                        );
                    }

                    nodeMap.put(objId, obj);

                    String parentId = nodeJson.optString("parentId", null);
                    if (parentId != null && !parentId.equals("null")) {
                        parentLinkMap.put(objId, parentId);
                    }
                }

                // Second pass: Re-establish parent-child links in the hierarchy
                for (SceneObject obj : nodeMap.values()) {
                    String parentId = parentLinkMap.get(obj.getId());
                    if (parentId != null) {
                        SceneObject parentObj = nodeMap.get(parentId);
                        if (parentObj != null) {
                            parentObj.addChild(obj);
                        }
                    } else {
                        scene.addObject(obj); // Root scene object nodes
                    }
                }
            }

            DeserializedResult result = new DeserializedResult();
            result.project = project;
            result.scene = scene;
            return result;

        } catch (Exception e) {
            return null;
        }
    }

    private static Mesh createBoxMesh(float width, float height, float depth) {
        float hw = width / 2.0f, hh = height / 2.0f, hd = depth / 2.0f;
        float[] positions = new float[]{
            -hw, -hh,  hd,   hw, -hh,  hd,   hw,  hh,  hd,  -hw,  hh,  hd, // Front
            -hw, -hh, -hd,  -hw,  hh, -hd,   hw,  hh, -hd,   hw, -hh, -hd, // Back
            -hw,  hh, -hd,  -hw,  hh,  hd,   hw,  hh,  hd,   hw,  hh, -hd, // Top
            -hw, -hh, -hd,   hw, -hh, -hd,   hw, -hh,  hd,  -hw, -hh,  hd, // Bottom
             hw, -hh, -hd,   hw,  hh, -hd,   hw,  hh,  hd,   hw, -hh,  hd, // Right
            -hw, -hh, -hd,  -hw, -hh,  hd,  -hw,  hh,  hd,  -hw,  hh, -hd  // Left
        };
        float[] normals = new float[]{
             0,  0,  1,   0,  0,  1,   0,  0,  1,   0,  0,  1,
             0,  0, -1,   0,  0, -1,   0,  0, -1,   0,  0, -1,
             0,  1,  0,   0,  1,  0,   0,  1,  0,   0,  1,  0,
             0, -1,  0,   0, -1,  0,   0, -1,  0,   0, -1,  0,
             1,  0,  0,   1,  0,  0,   1,  0,  0,   1,  0,  0,
            -1,  0,  0,  -1,  0,  0,  -1,  0,  0,  -1,  0,  0
        };
        float[] uvs = new float[]{
            0,0, 1,0, 1,1, 0,1,
            1,0, 1,1, 0,1, 0,0,
            0,1, 0,0, 1,0, 1,1,
            1,1, 0,1, 0,0, 1,0,
            1,0, 1,1, 0,1, 0,0,
            0,0, 1,0, 1,1, 0,1
        };
        short[] indices = new short[]{
             0,  1,  2,   0,  2,  3,
             4,  5,  6,   4,  6,  7,
             8,  9, 10,   8, 10, 11,
            12, 13, 14,  12, 14, 15,
            16, 17, 18,  16, 18, 19,
            20, 21, 22,  20, 22, 23
        };
        return new Mesh(positions, normals, uvs, indices);
    }
}