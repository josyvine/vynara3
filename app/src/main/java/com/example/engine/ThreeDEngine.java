package com.example.engine;

public class ThreeDEngine {
    private final SceneManager sceneManager;
    private final MaterialManager materialManager;
    private final LightManager lightManager;
    private final CameraManager cameraManager;

    public ThreeDEngine() {
        this.sceneManager = new SceneManager();
        this.materialManager = new MaterialManager();
        this.lightManager = new LightManager();
        this.cameraManager = new CameraManager();
    }

    /**
     * Phase 1 Alignment: Injection constructor allowing ThreeDEngine to share 
     * managers with the unified ProjectRuntime singleton.
     */
    public ThreeDEngine(SceneManager sceneManager, MaterialManager materialManager, 
                         LightManager lightManager, CameraManager cameraManager) {
        this.sceneManager = sceneManager != null ? sceneManager : new SceneManager();
        this.materialManager = materialManager != null ? materialManager : new MaterialManager();
        this.lightManager = lightManager != null ? lightManager : new LightManager();
        this.cameraManager = cameraManager != null ? cameraManager : new CameraManager();
    }

    public SceneManager getSceneManager() { return sceneManager; }
    public MaterialManager getMaterialManager() { return materialManager; }
    public LightManager getLightManager() { return lightManager; }
    public CameraManager getCameraManager() { return cameraManager; }

    public SceneObject createPrimitive(String type, float width, float height, float depth) {
        String primitiveType = type != null ? type.toLowerCase() : "cube";
        float w = width > 0 ? width : 1.5f;
        float h = height > 0 ? height : 1.5f;
        float d = depth > 0 ? depth : 1.5f;

        Mesh mesh = createPrimitiveMesh(primitiveType, w, h, d);

        Material mat = materialManager.getMaterial("mat_default");
        if (mat == null) {
            mat = new Material("mat_default", "Default PBR", 0.8f, 0.8f, 0.8f, 1.0f);
            materialManager.addMaterial(mat);
        }

        String id = "obj_" + primitiveType + "_" + System.currentTimeMillis();
        SceneObject obj = new SceneObject(id, primitiveType.toUpperCase(), "PRIMITIVE", mesh, mat);
        sceneManager.getActiveScene().addObject(obj);
        return obj;
    }

    /**
     * Phase 4 & 23 Alignment: Constructs procedural structural nodes.
     */
    public SceneObject createProceduralStructure(String structureType, String name) {
        String type = structureType != null ? structureType.toLowerCase() : "sofa";
        String objName = name != null && !name.isEmpty() ? name : structureType.toUpperCase();
        String id = "struct_" + type + "_" + System.currentTimeMillis();

        float w = 2.0f, h = 1.0f, d = 2.0f;
        if ("sofa".equalsIgnoreCase(type) || "couch".equalsIgnoreCase(type)) {
            w = 2.2f; h = 0.9f; d = 1.0f;
        } else if ("table".equalsIgnoreCase(type) || "desk".equalsIgnoreCase(type)) {
            w = 1.6f; h = 0.8f; d = 1.0f;
        } else if ("house".equalsIgnoreCase(type) || "villa".equalsIgnoreCase(type) || "building".equalsIgnoreCase(type)) {
            w = 4.0f; h = 3.0f; d = 4.0f;
        } else if ("pool".equalsIgnoreCase(type)) {
            w = 3.5f; h = 0.5f; d = 2.5f;
        } else if ("tree".equalsIgnoreCase(type) || "plant".equalsIgnoreCase(type)) {
            w = 1.5f; h = 3.0f; d = 1.5f;
        }

        Mesh mesh = createPrimitiveMesh("cube", w, h, d);
        Material mat = materialManager.getMaterial("mat_default");
        if (mat == null) {
            mat = new Material("mat_default", "Default PBR", 0.8f, 0.8f, 0.8f, 1.0f);
            materialManager.addMaterial(mat);
        }

        SceneObject rootObject = new SceneObject(id, objName, "STRUCTURE", mesh, mat);
        sceneManager.getActiveScene().addObject(rootObject);
        return rootObject;
    }

    private Mesh createPrimitiveMesh(String type, float width, float height, float depth) {
        float hw = width / 2.0f;
        float hh = height / 2.0f;
        float hd = depth / 2.0f;

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