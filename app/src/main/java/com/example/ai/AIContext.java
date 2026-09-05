package com.example.ai;

import com.example.engine.Light;
import com.example.engine.Material;
import com.example.engine.Scene;
import com.example.engine.SceneObject;

import org.json.JSONArray;
import org.json.JSONObject;

public class AIContext {

    public static String buildSceneContextJson(Scene scene) {
        if (scene == null) return "{}";

        try {
            JSONObject root = new JSONObject();
            root.put("sceneId", scene.getId());
            root.put("sceneName", scene.getName());
            root.put("totalTriangles", scene.getTotalTriangleCount());

            // Serialize 3D Scene Objects & Hierarchy
            JSONArray objectsArr = new JSONArray();
            for (SceneObject obj : scene.getObjects()) {
                objectsArr.put(serializeSceneObject(obj));
            }
            root.put("objects", objectsArr);

            return root.toString(2);
            
        } catch (Exception e) {
            return "{\"error\":\"Context serialization failed: " + e.getMessage() + "\"}";
        }
    }

    private static JSONObject serializeSceneObject(SceneObject obj) throws Exception {
        JSONObject objJson = new JSONObject();
        if (obj == null) return objJson;

        objJson.put("id", obj.getId());
        objJson.put("name", obj.getName());
        objJson.put("semanticType", obj.getSemanticType());
        objJson.put("visible", obj.isVisible());
        objJson.put("selected", obj.isSelected());

        // Full TRS Transform Serialization
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

        // PBR Material Serialization
        if (obj.getMaterial() != null) {
            Material mat = obj.getMaterial();
            JSONObject matJson = new JSONObject();
            matJson.put("id", mat.getId());
            matJson.put("name", mat.getName());
            matJson.put("metallic", mat.getMetallic());
            matJson.put("roughness", mat.getRoughness());
            
            float[] rgba = mat.getBaseColorRGBA();
            if (rgba != null && rgba.length >= 4) {
                JSONArray colorArr = new JSONArray();
                colorArr.put(rgba[0]); colorArr.put(rgba[1]); colorArr.put(rgba[2]); colorArr.put(rgba[3]);
                matJson.put("baseColorRGBA", colorArr);
            }
            objJson.put("material", matJson);
        }

        // Mesh Statistics
        if (obj.getMesh() != null) {
            JSONObject meshJson = new JSONObject();
            meshJson.put("vertexCount", obj.getMesh().getVertexCount());
            meshJson.put("triangleCount", obj.getMesh().getTriangleCount());
            objJson.put("mesh", meshJson);
        }

        // Recursive Child Hierarchy Serialization
        if (obj.getChildren() != null && !obj.getChildren().isEmpty()) {
            JSONArray childrenArr = new JSONArray();
            for (SceneObject child : obj.getChildren()) {
                childrenArr.put(serializeSceneObject(child));
            }
            objJson.put("children", childrenArr);
        }

        return objJson;
    }
}