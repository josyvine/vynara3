package com.example.ai.agents;

import com.example.ai.protocol.AIDirectorSpec;
import com.example.utils.VynaraLogger;

public class BlenderWorkerAgent {

    public static class WorkerScripts {
        public final String heroScript;
        public final String environmentScript;
        public final String lightingAndRenderScript;
        public final String compositeMasterScript;

        public WorkerScripts(String hero, String env, String light, String master) {
            this.heroScript = hero;
            this.environmentScript = env;
            this.lightingAndRenderScript = light;
            this.compositeMasterScript = master;
        }
    }

    /**
     * Synthesizes specialized, modular Python scripts governed by the Director's Spec.
     */
    public static WorkerScripts generateModularScripts(String userPrompt, AIDirectorSpec spec, String assetId) {
        VynaraLogger.system("BlenderWorkerAgent: Spawning modular worker scripts for asset [" + assetId + "]");

        String w1 = buildWorker1HeroScript(userPrompt, spec);
        String w2 = buildWorker2EnvironmentScript(spec);
        String w3 = buildWorker3LightingAndRenderScript(spec);

        // Assemble modular scripts into an executable master script
        StringBuilder master = new StringBuilder();
        master.append("# ==========================================\n");
        master.append("# Vynara Autonomous 3D Studio - Master Build\n");
        master.append("# Source: ").append(spec.getGenerationSource()).append("\n");
        master.append("# Scene: ").append(spec.getSceneType()).append(" | Mood: ").append(spec.getMood()).append("\n");
        master.append("# ==========================================\n\n");
        master.append("import bpy, os, math, random\n");
        master.append("import addon_utils\n\n");
        master.append("try:\n");
        master.append("    addon_utils.enable('archimesh')\n");
        master.append("    addon_utils.enable('rigify')\n");
        master.append("except Exception as e:\n");
        master.append("    print(f'Addon activation note: {e}')\n\n");
        master.append("os.makedirs('output', exist_ok=True)\n\n");
        master.append("# Clean scene completely\n");
        master.append("bpy.ops.object.select_all(action='SELECT')\n");
        master.append("bpy.ops.object.delete(use_global=False)\n\n");

        master.append("# --- WORKER 1: HERO STRUCTURE ---\n");
        master.append(w1).append("\n\n");

        master.append("# --- WORKER 2: ENVIRONMENT & FOLIAGE ---\n");
        master.append(w2).append("\n\n");

        master.append("# --- WORKER 3: ATMOSPHERE, LIGHTING & RENDER ---\n");
        master.append(w3).append("\n");

        return new WorkerScripts(w1, w2, w3, master.toString());
    }

    private static String buildWorker1HeroScript(String prompt, AIDirectorSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("random.seed(").append(spec.getSeedHero()).append(")\n");
        sb.append("# Hero Primary Material\n");
        sb.append("mat_hero = bpy.data.materials.new('Mat_Hero_Primary')\n");
        sb.append("mat_hero.use_nodes = True\n");
        sb.append("bsdf_h = mat_hero.node_tree.nodes.get('Principled BSDF')\n");
        sb.append("if bsdf_h:\n");
        float[] rgb = hexToRgb(spec.getPrimaryColorHex());
        sb.append("    bsdf_h.inputs['Base Color'].default_value = (").append(rgb[0]).append(", ").append(rgb[1]).append(", ").append(rgb[2]).append(", 1.0)\n");
        sb.append("    bsdf_h.inputs['Roughness'].default_value = 0.35\n");
        sb.append("    bsdf_h.inputs['Metallic'].default_value = 0.2\n\n");

        String p = prompt.toLowerCase();
        if (p.contains("sofa") || p.contains("couch") || p.contains("chair")) {
            sb.append("# Procedural Sofa Base & Cushions\n");
            sb.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 0.35))\n");
            sb.append("base = bpy.context.active_object\n");
            sb.append("base.name = 'Sofa_Base'\n");
            sb.append("base.scale = (2.6, 1.1, 0.25)\n");
            sb.append("bpy.ops.object.transform_apply(scale=True)\n");
            sb.append("base.data.materials.append(mat_hero)\n\n");

            sb.append("for idx, px in enumerate([-0.65, 0.65]):\n");
            sb.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(px, 0.12, 0.58))\n");
            sb.append("    cushion = bpy.context.active_object\n");
            sb.append("    cushion.name = f'Seat_Cushion_{idx}'\n");
            sb.append("    cushion.scale = (1.18, 0.85, 0.26)\n");
            sb.append("    bpy.ops.object.transform_apply(scale=True)\n");
            sb.append("    mod = cushion.modifiers.new('Bevel', 'BEVEL')\n");
            sb.append("    mod.width = 0.08\n");
            sb.append("    cushion.data.materials.append(mat_hero)\n");
        } else if (p.contains("superhero") || p.contains("character")) {
            sb.append("# Hero Character Anatomy\n");
            sb.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 2.3))\n");
            sb.append("chest = bpy.context.active_object\n");
            sb.append("chest.name = 'Hero_Chest'\n");
            sb.append("chest.scale = (0.7, 0.4, 0.6)\n");
            sb.append("bpy.ops.object.transform_apply(scale=True)\n");
            sb.append("chest.data.materials.append(mat_hero)\n\n");

            sb.append("bpy.ops.mesh.primitive_uv_sphere_add(radius=0.25, location=(0, 0, 2.85))\n");
            sb.append("head = bpy.context.active_object\n");
            sb.append("head.name = 'Hero_Head'\n");
            sb.append("head.data.materials.append(mat_hero)\n");
        } else {
            // Architectural Structure / Villa / Nature Hero
            sb.append("# Main Architectural Foundation & Pavilion\n");
            sb.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 1.8))\n");
            sb.append("main_struct = bpy.context.active_object\n");
            sb.append("main_struct.name = 'Hero_Structure'\n");
            sb.append("main_struct.scale = (14.0, 10.0, 3.6)\n");
            sb.append("bpy.ops.object.transform_apply(scale=True)\n");
            sb.append("mod = main_struct.modifiers.new('Bevel', 'BEVEL')\n");
            sb.append("mod.width = 0.06\n");
            sb.append("main_struct.data.materials.append(mat_hero)\n");
        }
        return sb.toString();
    }

    private static String buildWorker2EnvironmentScript(AIDirectorSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("random.seed(").append(spec.getSeedVegetation()).append(")\n");
        sb.append("# Detail & Foliage Material\n");
        sb.append("mat_env = bpy.data.materials.new('Mat_Foliage_Detail')\n");
        sb.append("mat_env.use_nodes = True\n");
        sb.append("bsdf_e = mat_env.node_tree.nodes.get('Principled BSDF')\n");
        sb.append("if bsdf_e:\n");
        float[] rgb = hexToRgb(spec.getSecondaryColorHex());
        sb.append("    bsdf_e.inputs['Base Color'].default_value = (").append(rgb[0]).append(", ").append(rgb[1]).append(", ").append(rgb[2]).append(", 1.0)\n");
        sb.append("    bsdf_e.inputs['Roughness'].default_value = 0.65\n\n");

        sb.append("# Procedural Ground Terrain with Elevation\n");
        sb.append("bpy.ops.mesh.primitive_plane_add(size=32, location=(0, 0, 0))\n");
        sb.append("terrain = bpy.context.active_object\n");
        sb.append("terrain.name = 'Ground_Terrain'\n");
        sb.append("terrain.data.materials.append(mat_env)\n\n");

        sb.append("# Procedural Curved Vegetation / Vines (Bezier Technique)\n");
        sb.append("curve_data = bpy.data.curves.new('VineCurve', type='CURVE')\n");
        sb.append("curve_data.dimensions = '3D'\n");
        sb.append("curve_data.bevel_depth = 0.04\n");
        sb.append("polyline = curve_data.splines.new('BEZIER')\n");
        sb.append("polyline.bezier_points.add(3)\n");
        sb.append("points = polyline.bezier_points\n");
        sb.append("points[0].co = (-2.0, -1.0, 0.1)\n");
        sb.append("points[1].co = (-1.5, 0.2, 1.2)\n");
        sb.append("points[2].co = (-1.8, 0.8, 2.4)\n");
        sb.append("points[3].co = (-1.2, 1.4, 3.2)\n");
        sb.append("for p in points: p.handle_left_type = 'AUTO'; p.handle_right_type = 'AUTO'\n");
        sb.append("vine_obj = bpy.data.objects.new('Procedural_Vines', curve_data)\n");
        sb.append("bpy.context.collection.objects.link(vine_obj)\n");
        sb.append("vine_obj.data.materials.append(mat_env)\n");
        return sb.toString();
    }

    private static String buildWorker3LightingAndRenderScript(AIDirectorSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("random.seed(").append(spec.getSeedLighting()).append(")\n");

        // Camera setup with exact Focal Length and Depth of Field (f/1.8)
        sb.append("# Cinematic Camera Setup\n");
        sb.append("cam_data = bpy.data.cameras.new('CinematicCamera')\n");
        sb.append("cam_data.lens = ").append(spec.getFocalLengthMm()).append("\n");
        sb.append("cam_data.dof.use_dof = True\n");
        sb.append("cam_data.dof.focus_distance = ").append(spec.getFocusDistance()).append("\n");
        sb.append("cam_data.dof.aperture_fstop = ").append(spec.getApertureFStop()).append("\n");
        sb.append("cam_obj = bpy.data.objects.new('Camera', cam_data)\n");
        sb.append("bpy.context.collection.objects.link(cam_obj)\n");
        sb.append("bpy.context.scene.camera = cam_obj\n");
        sb.append("cam_obj.location = (").append(spec.getCameraPosition()[0]).append(", ").append(spec.getCameraPosition()[1]).append(", ").append(spec.getCameraPosition()[2]).append(")\n");
        sb.append("cam_obj.rotation_euler = (math.radians(72), 0, 0)\n\n");

        // Sun light with exact elevation/azimuth
        sb.append("# Natural Sunlight\n");
        sb.append("sun_data = bpy.data.lights.new('Sun', type='SUN')\n");
        sb.append("sun_data.energy = ").append(spec.getSunIntensity()).append("\n");
        sb.append("sun_obj = bpy.data.objects.new('SunLight', sun_data)\n");
        sb.append("bpy.context.collection.objects.link(sun_obj)\n");
        sb.append("sun_obj.rotation_euler = (math.radians(").append(spec.getSunElevation()).append("), 0, math.radians(").append(spec.getSunAzimuth()).append("))\n\n");

        // Volumetric mist (God rays)
        if (spec.isUseVolumetrics()) {
            sb.append("# Volumetric Atmospheric Mist (God Rays)\n");
            sb.append("world = bpy.context.scene.world\n");
            sb.append("if world is None:\n");
            sb.append("    world = bpy.data.worlds.new('World')\n");
            sb.append("    bpy.context.scene.world = world\n");
            sb.append("world.use_nodes = True\n");
            sb.append("wnodes = world.node_tree.nodes\n");
            sb.append("wlinks = world.node_tree.links\n");
            sb.append("vol_node = wnodes.new('ShaderNodeVolumePrincipled')\n");
            sb.append("vol_node.inputs['Density'].default_value = ").append(spec.getVolumetricDensity()).append("\n");
            sb.append("w_output = wnodes.get('World Output')\n");
            sb.append("if w_output:\n");
            sb.append("    wlinks.new(vol_node.outputs['Volume'], w_output.inputs['Volume'])\n\n");
        }

        // Render still image (PNG) + Export 3D Mesh (GLB)
        sb.append("# Dual Render Deliverables\n");
        sb.append("bpy.context.scene.render.resolution_x = 1280\n");
        sb.append("bpy.context.scene.render.resolution_y = 720\n");
        sb.append("bpy.context.scene.render.filepath = 'output/render.png'\n");
        sb.append("try:\n");
        sb.append("    bpy.ops.render.render(write_still=True)\n");
        sb.append("except Exception as re: print(f'Render still warning: {re}')\n\n");
        sb.append("bpy.ops.export_scene.gltf(filepath='output/model.glb', export_format='GLB')\n");

        return sb.toString();
    }

    private static float[] hexToRgb(String hex) {
        if (hex == null || hex.isEmpty()) return new float[] { 0.5f, 0.5f, 0.5f };
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            int c = (int) Long.parseLong(h, 16);
            float r = ((c >> 16) & 0xFF) / 255.0f;
            float g = ((c >> 8) & 0xFF) / 255.0f;
            float b = (c & 0xFF) / 255.0f;
            return new float[] { r, g, b };
        } catch (Exception e) {
            return new float[] { 0.5f, 0.5f, 0.5f };
        }
    }
}