package com.example.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, ToolDefinition> registeredTools = new HashMap<>();

    public ToolRegistry() {
        registerCoreTools();
    }

    private void registerCoreTools() {
        // Geometry Primitives
        register(new ToolDefinition("geometry.create_primitive", "Create Primitive", "GEOMETRY",
                "Creates a primitive 3D mesh (cube, sphere, cylinder, cone, plane, torus).", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("type", "STRING", true, "cube, sphere, cylinder, cone, plane, torus")
                .addParam("width", "FLOAT", false, "Width dimension")
                .addParam("height", "FLOAT", false, "Height dimension")
                .addParam("depth", "FLOAT", false, "Depth dimension")
                .addParam("radius", "FLOAT", false, "Radius size"));

        // Geometry Procedural Structures
        register(new ToolDefinition("geometry.create_procedural", "Create Procedural Structure", "GEOMETRY",
                "Generates procedural 3D models (house, villa, sofa, table, tree, car, pool, room).", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("type", "STRING", true, "house, villa, sofa, table, tree, car, pool, room")
                .addParam("name", "STRING", false, "Display name for object")
                .addParam("style", "STRING", false, "realistic, stylized, modern, low_poly"));

        // Scene Graph Transformations & Node Controls
        register(new ToolDefinition("geometry.transform.translate", "Translate Object", "GEOMETRY",
                "Translates 3D position of target object.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", true, "Target object ID")
                .addParam("x", "FLOAT", true, "X translation")
                .addParam("y", "FLOAT", true, "Y translation")
                .addParam("z", "FLOAT", true, "Z translation"));

        register(new ToolDefinition("geometry.transform.rotate", "Rotate Object", "GEOMETRY",
                "Rotates target object along pitch/yaw/roll axes.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", true, "Target object ID")
                .addParam("x", "FLOAT", true, "Pitch angle degrees")
                .addParam("y", "FLOAT", true, "Yaw angle degrees")
                .addParam("z", "FLOAT", true, "Roll angle degrees"));

        register(new ToolDefinition("geometry.transform.scale", "Scale Object", "GEOMETRY",
                "Scales target object.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", true, "Target object ID")
                .addParam("scaleX", "FLOAT", true, "X scale factor")
                .addParam("scaleY", "FLOAT", true, "Y scale factor")
                .addParam("scaleZ", "FLOAT", true, "Z scale factor"));

        register(new ToolDefinition("geometry.delete_object", "Delete Selected Object", "GEOMETRY",
                "Deletes target object from scene graph hierarchy.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", false, "Target object ID"));

        register(new ToolDefinition("geometry.duplicate_object", "Duplicate Object", "GEOMETRY",
                "Duplicates target node and sub-mesh tree.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", false, "Target object ID"));

        // PBR Material Shading & Aliases
        register(new ToolDefinition("material.set_properties", "Set Material Properties", "MATERIAL",
                "Sets PBR color, metallic, roughness, and opacity of object material.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", true, "Target object ID")
                .addParam("colorHex", "STRING", false, "Base color hex string like #FF0000 or #1A2B3C")
                .addParam("metallic", "FLOAT", false, "Metallic factor 0.0 to 1.0")
                .addParam("roughness", "FLOAT", false, "Roughness factor 0.0 to 1.0")
                .addParam("opacity", "FLOAT", false, "Opacity factor 0.0 to 1.0"));

        register(new ToolDefinition("material.apply", "Apply Material", "MATERIAL",
                "Applies material properties to scene node.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", false, "Target object ID")
                .addParam("materialId", "STRING", false, "Material ID"));

        register(new ToolDefinition("material.create", "Create Material", "MATERIAL",
                "Creates a new material definition.", ToolDefinition.AvailabilityState.AVAILABLE));

        // Characters & Creatures
        register(new ToolDefinition("character.create_humanoid", "Create Humanoid Character", "CHARACTER",
                "Generates a 3D humanoid character with anatomy, mesh, skeleton, and rig.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("name", "STRING", false, "Character name")
                .addParam("height", "FLOAT", false, "Height in meters")
                .addParam("style", "STRING", false, "realistic, superhero, cartoon, low_poly"));

        register(new ToolDefinition("character.create_creature", "Create Creature / Animal", "CHARACTER",
                "Generates a 3D animal or creature (dog, bird, quadruped, creature).", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("species", "STRING", true, "dog, bird, quadruped, creature")
                .addParam("name", "STRING", false, "Creature name"));

        // Skeleton & Rigging
        register(new ToolDefinition("skeleton.bind", "Bind Skeleton & Calculate Weights", "SKELETON",
                "Binds skeleton bone hierarchy to mesh and calculates normalized skin vertex weights.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("characterId", "STRING", true, "Target character ID"));

        register(new ToolDefinition("rig.create_ik", "Create IK Controller", "RIG",
                "Creates Inverse Kinematics solver chain for target limbs (hands, feet, head).", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("characterId", "STRING", true, "Target character ID")
                .addParam("limb", "STRING", true, "left_arm, right_arm, left_leg, right_leg"));

        // Animation Controls
        register(new ToolDefinition("animation.create_clip", "Create & Apply Animation Clip", "ANIMATION",
                "Applies keyframed or procedural motion clip (walk, run, idle, crouch, fly, wave).", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("characterId", "STRING", true, "Target character ID")
                .addParam("clipName", "STRING", true, "walk, run, idle, crouch, fly, wave"));

        // Cloud Compute & Blender MCP
        register(new ToolDefinition("blender.cloud_generate", "Cloud Blender Generator", "CLOUD",
                "Executes a Blender Python (bpy) script on a persistent cloud runner (GitHub Actions / Hugging Face) and streams GLB.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("prompt", "STRING", true, "Description of 3D asset to generate")
                .addParam("bpyScript", "STRING", false, "Generated Blender Python script")
                .addParam("assetId", "STRING", false, "Target asset identifier"));

        register(new ToolDefinition("blender.generate", "Blender Generator Alias", "CLOUD",
                "Alias for blender.cloud_generate.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("prompt", "STRING", false, "Description of 3D asset to generate")
                .addParam("bpyScript", "STRING", false, "Generated Blender Python script"));

        register(new ToolDefinition("rig.auto_rig_cloud", "Cloud Auto-Rigging Engine", "CLOUD",
                "Uploads a static 3D mesh to cloud Blender to generate a Rigify humanoid skeleton and automatic skin weights.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", true, "Target mesh object ID")
                .addParam("rigType", "STRING", false, "humanoid, quadruped, bird"));

        // Asset On-Demand Streaming
        register(new ToolDefinition("asset.fetch_and_spawn", "Fetch & Spawn Remote Asset", "ASSET",
                "Downloads on-demand GLB asset from cloud/CDN, caches locally, and instantiates into scene.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("assetId", "STRING", true, "Unique asset ID or catalog name")
                .addParam("url", "STRING", false, "Direct download URL for GLB file")
                .addParam("posX", "FLOAT", false, "X position")
                .addParam("posY", "FLOAT", false, "Y position")
                .addParam("posZ", "FLOAT", false, "Z position"));

        // Lighting & Viewport Camera
        register(new ToolDefinition("scene.add_light", "Add Light Source", "LIGHTING",
                "Adds directional, point, or spot light to active scene.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("type", "STRING", true, "directional, point, spot, ambient")
                .addParam("colorHex", "STRING", false, "Light color hex")
                .addParam("intensity", "FLOAT", false, "Light brightness intensity"));

        register(new ToolDefinition("scene.set_camera", "Set Camera Viewpoint", "CAMERA",
                "Positions camera target and framing.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("posX", "FLOAT", false, "Camera X position")
                .addParam("posY", "FLOAT", false, "Camera Y position")
                .addParam("posZ", "FLOAT", false, "Camera Z position")
                .addParam("targetX", "FLOAT", false, "Look target X")
                .addParam("targetY", "FLOAT", false, "Look target Y")
                .addParam("targetZ", "FLOAT", false, "Look target Z"));

        register(new ToolDefinition("scene.clear", "Clear Scene", "GEOMETRY",
                "Clears all objects from current active scene graph.", ToolDefinition.AvailabilityState.AVAILABLE));

        register(new ToolDefinition("scene.add_node", "Add Node to Scene", "GEOMETRY",
                "Adds a node to the active scene graph.", ToolDefinition.AvailabilityState.AVAILABLE));

        // Transactions
        register(new ToolDefinition("transaction.undo", "Undo Transaction", "TRANSACTION",
                "Reverts the last scene operation.", ToolDefinition.AvailabilityState.AVAILABLE));

        register(new ToolDefinition("transaction.redo", "Redo Transaction", "TRANSACTION",
                "Re-applies the last undone scene operation.", ToolDefinition.AvailabilityState.AVAILABLE));

        // Validation & Export Persistence
        register(new ToolDefinition("validation.check_mesh", "Validate Mesh & Rig", "VALIDATION",
                "Inspects topology, vertex normals, skin weights, and bounding boxes.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("objectId", "STRING", false, "Target object ID or null for entire scene"));

        register(new ToolDefinition("export.gltf", "Export Scene to GLTF/GLB", "EXPORT",
                "Exports active 3D scene geometry, materials, and hierarchy to GLTF/GLB.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("filename", "STRING", false, "Export filename"));

        register(new ToolDefinition("project.save", "Save Project to Disk", "STORAGE",
                "Persists active scene graph and assets to local storage.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("projectId", "STRING", false, "Project identifier"));

        register(new ToolDefinition("project.load", "Load Project from Disk", "STORAGE",
                "Loads project state from local storage.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("projectId", "STRING", false, "Project identifier"));

        register(new ToolDefinition("project.create", "Create New Project", "STORAGE",
                "Initializes a new empty project.", ToolDefinition.AvailabilityState.AVAILABLE)
                .addParam("name", "STRING", false, "Project name"));
    }

    public void register(ToolDefinition tool) {
        if (tool != null && tool.getId() != null) {
            registeredTools.put(tool.getId(), tool);
        }
    }

    public ToolDefinition getTool(String id) {
        if (id == null) return null;
        return registeredTools.get(id);
    }

    public boolean isToolAvailable(String id) {
        ToolDefinition t = getTool(id);
        return t != null && t.isAvailable();
    }

    public List<ToolDefinition> getToolsByCategory(String category) {
        List<ToolDefinition> categoryTools = new ArrayList<>();
        if (category == null) return categoryTools;

        for (ToolDefinition tool : registeredTools.values()) {
            if (category.equalsIgnoreCase(tool.getCategory())) {
                categoryTools.add(tool);
            }
        }
        return categoryTools;
    }

    public Map<String, ToolDefinition> getRegisteredTools() {
        return registeredTools;
    }
}