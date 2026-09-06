package com.example.ai;

import com.example.ai.agents.BlenderWorkerAgent;
import com.example.ai.protocol.AIDirectorSpec;
import com.example.ai.protocol.AIToolCall;
import com.example.ai.protocol.AIProductionPlan;
import com.example.ai.protocol.AIProductionRequest;
import com.example.ai.validation.PlanValidator;
import com.example.knowledge.KnowledgeEntry;
import com.example.knowledge.KnowledgeManager;
import com.example.tasks.ProductionPlan;
import com.example.tasks.TaskGraph;
import com.example.tasks.TaskNode;
import com.example.tools.ToolOperation;
import com.example.tools.ToolRegistry;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PromptInterpreter {
    private final KnowledgeManager knowledgeManager;

    public PromptInterpreter(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    public ProductionPlan createProductionPlan(String userPrompt, String style, String targetEngine) {
        return createProductionPlan(userPrompt, style, targetEngine, new ArrayList<>());
    }

    /**
     * Dynamic offline fallback generator.
     * Uses KnowledgeManager multi-concept extraction to generate tasks for ALL detected entities
     * in the prompt (e.g., house + pool + sofa + tree) using an optimized parallel dependency tree.
     */
    public ProductionPlan createProductionPlan(String userPrompt, String style, String targetEngine, List<String> referenceImageUris) {
        List<KnowledgeEntry> matchedKnowledge = knowledgeManager.retrieveAllKnowledgeForPrompt(userPrompt);
        KnowledgeEntry primaryKnowledge = matchedKnowledge.get(0);

        String projectName = extractProjectName(userPrompt, primaryKnowledge.getCategory());
        ProductionPlan plan = new ProductionPlan(projectName, userPrompt, primaryKnowledge.getCategory(), primaryKnowledge, referenceImageUris);
        TaskGraph graph = plan.getTaskGraph();

        int taskCounter = 1;
        
        // Track the geometries created so far to link modifiers and lights cleanly in parallel
        List<String> geometryTaskIds = new ArrayList<>();
        List<String> leafTaskIds = new ArrayList<>();

        // Step 0A: Auto-Clear Canvas Task (Ensures previous scenes don't stack up)
        String clearTaskId = "task_" + taskCounter++;
        TaskNode clearNode = new TaskNode(clearTaskId, "Clearing Canvas", "Resetting viewport scene nodes",
                new ToolOperation("scene.clear"));
        graph.addTask(clearNode);

        // Step 0B: Reference Image Analysis Task if images are attached
        String referenceTaskId = null;
        if (plan.hasReferenceImages()) {
            referenceTaskId = "task_" + taskCounter++;
            ToolOperation refOp = new ToolOperation("image.process_reference")
                    .setParam("count", plan.getReferenceImageUris().size())
                    .setParam("uris", plan.getReferenceImageUris());
            TaskNode t0 = new TaskNode(referenceTaskId, "Processing Reference Images",
                    "Ingesting " + plan.getReferenceImageUris().size() + " visual reference image(s) for Director Spec", refOp);
            t0.addDependency(clearTaskId);
            // Pre-complete this task: Gemini Vision already ingested images in Phase 1
            t0.setStatus(TaskNode.Status.COMPLETED);
            graph.addTask(t0);
        }

        String initialDependency = (referenceTaskId != null) ? referenceTaskId : clearTaskId;
        boolean isBlenderNative = targetEngine != null && targetEngine.toLowerCase().contains("blender");

        if (isBlenderNative) {
            // Target Engine is Blender Native -> Generate Cloud Blender Task Node
            String cloudTaskId = "task_" + taskCounter++;
            String assetId = "asset_blender_" + System.currentTimeMillis();

            // 1. Establish Director Spec & Modular Worker Scripts
            AIDirectorSpec spec = new AIDirectorSpec();
            String lowerPrompt = (userPrompt != null) ? userPrompt.toLowerCase().trim() : "";

            // Collision-free demo preset detection (Ensuring 'village' NEVER matches 'villa')
            boolean isVillageDemo = lowerPrompt.contains("high-detail tropical village environment") 
                    || lowerPrompt.contains("tropical village") 
                    || lowerPrompt.contains("village");

            boolean isVillaDemo = !isVillageDemo && (
                    lowerPrompt.contains("realistic modern villa with a swimming pool") 
                    || lowerPrompt.contains("modern villa & pool")
                    || lowerPrompt.contains("modern villa & swimming pool")
                    || lowerPrompt.contains("modern villa")
                    || (lowerPrompt.contains("villa") && !lowerPrompt.contains("sofa")));

            boolean isSofaDemo = lowerPrompt.contains("modern luxury leather sofa") 
                    || lowerPrompt.contains("leather sofa") 
                    || (lowerPrompt.contains("sofa") && !lowerPrompt.contains("villa") && !lowerPrompt.contains("chair") && !lowerPrompt.contains("lounge"));

            boolean isSuperheroDemo = lowerPrompt.contains("stylized rigged superhero character") 
                    || lowerPrompt.contains("rigged superhero") 
                    || (lowerPrompt.contains("superhero") && !lowerPrompt.contains("villa"));

            boolean isDogDemo = lowerPrompt.contains("animated quadruped dog model") 
                    || lowerPrompt.contains("animated dog") 
                    || (lowerPrompt.contains("dog") && !lowerPrompt.contains("villa"));

            if (isVillaDemo) {
                spec.setSceneType("modern_architecture");
                spec.setMood("twilight_golden_hour");
                spec.setFocalLengthMm(35.0f);
            } else if (isSofaDemo) {
                spec.setSceneType("luxury_furniture");
                spec.setMood("studio_commercial");
                spec.setFocalLengthMm(65.0f);
                spec.setUseVolumetrics(false);
            } else if (isSuperheroDemo) {
                spec.setSceneType("stylized_character");
                spec.setMood("heroic_dramatic");
                spec.setFocalLengthMm(50.0f);
            } else if (isDogDemo) {
                spec.setSceneType("stylized_animal");
                spec.setMood("outdoor_daylight");
                spec.setFocalLengthMm(50.0f);
            } else if (isVillageDemo) {
                spec.setSceneType("tropical_village");
                spec.setMood("sunny_coastal");
                spec.setFocalLengthMm(35.0f);
            } else {
                spec.setSceneType("custom_dynamic");
                spec.setMood("cinematic_photoreal");
                spec.setFocalLengthMm(50.0f);
            }

            // Generate modular worker sub-scripts via BlenderWorkerAgent
            BlenderWorkerAgent.WorkerScripts modularScripts = 
                    BlenderWorkerAgent.generateModularScripts(userPrompt, spec, assetId);

            // 2. Build Procedural Scene Script (Full multi-material procedural definitions for verified demo presets)
            StringBuilder defaultBpyScript = new StringBuilder();
            defaultBpyScript.append("import bpy\n");
            defaultBpyScript.append("import os\n");
            defaultBpyScript.append("import math\n");
            defaultBpyScript.append("import random\n");
            defaultBpyScript.append("import addon_utils\n\n");
            defaultBpyScript.append("try:\n");
            defaultBpyScript.append("    addon_utils.enable('archimesh')\n");
            defaultBpyScript.append("    addon_utils.enable('rigify')\n");
            defaultBpyScript.append("except Exception as e:\n");
            defaultBpyScript.append("    print(f'Addon activation note: {e}')\n\n");
            defaultBpyScript.append("os.makedirs('output', exist_ok=True)\n\n");
            defaultBpyScript.append("# Clear all initial default scene objects\n");
            defaultBpyScript.append("bpy.ops.object.select_all(action='SELECT')\n");
            defaultBpyScript.append("bpy.ops.object.delete(use_global=False)\n\n");

            if (isSuperheroDemo) {
                defaultBpyScript.append("# --- High-Detail Procedural Rigged Superhero Character ---\n");
                defaultBpyScript.append("mat_suit = bpy.data.materials.new(name='Suit_Material')\n");
                defaultBpyScript.append("mat_suit.use_nodes = True\n");
                defaultBpyScript.append("bsdf_suit = mat_suit.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bsdf_suit:\n");
                defaultBpyScript.append("    bsdf_suit.inputs['Base Color'].default_value = (0.05, 0.2, 0.75, 1.0)\n");
                defaultBpyScript.append("    bsdf_suit.inputs['Metallic'].default_value = 0.3\n");
                defaultBpyScript.append("    bsdf_suit.inputs['Roughness'].default_value = 0.35\n\n");

                defaultBpyScript.append("mat_armor = bpy.data.materials.new(name='Armor_Gold')\n");
                defaultBpyScript.append("mat_armor.use_nodes = True\n");
                defaultBpyScript.append("bsdf_armor = mat_armor.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bsdf_armor:\n");
                defaultBpyScript.append("    bsdf_armor.inputs['Base Color'].default_value = (0.9, 0.7, 0.1, 1.0)\n");
                defaultBpyScript.append("    bsdf_armor.inputs['Metallic'].default_value = 0.8\n");
                defaultBpyScript.append("    bsdf_armor.inputs['Roughness'].default_value = 0.2\n\n");

                defaultBpyScript.append("# Torso / Chest\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 2.3))\n");
                defaultBpyScript.append("chest = bpy.context.active_object\n");
                defaultBpyScript.append("chest.name = 'Hero_Chest'\n");
                defaultBpyScript.append("chest.scale = (0.7, 0.4, 0.6)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("chest.data.materials.append(mat_suit)\n\n");

                defaultBpyScript.append("# Pelvis / Waist\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 1.8))\n");
                defaultBpyScript.append("pelvis = bpy.context.active_object\n");
                defaultBpyScript.append("pelvis.name = 'Hero_Pelvis'\n");
                defaultBpyScript.append("pelvis.scale = (0.55, 0.35, 0.4)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("pelvis.data.materials.append(mat_suit)\n\n");

                defaultBpyScript.append("# Head & Helmet\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_uv_sphere_add(radius=0.25, location=(0, 0, 2.85))\n");
                defaultBpyScript.append("head = bpy.context.active_object\n");
                defaultBpyScript.append("head.name = 'Hero_Head'\n");
                defaultBpyScript.append("head.data.materials.append(mat_suit)\n\n");

                defaultBpyScript.append("# Heroic Emblem Armor\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.08, location=(0, -0.22, 2.35))\n");
                defaultBpyScript.append("emblem = bpy.context.active_object\n");
                defaultBpyScript.append("emblem.rotation_euler = (1.5708, 0, 0)\n");
                defaultBpyScript.append("emblem.name = 'Hero_Emblem'\n");
                defaultBpyScript.append("emblem.data.materials.append(mat_armor)\n\n");

                defaultBpyScript.append("# Arms & Forearms\n");
                defaultBpyScript.append("for side, x in [('L', -0.5), ('R', 0.5)]:\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.12, depth=0.5, location=(x, 0, 2.3))\n");
                defaultBpyScript.append("    u_arm = bpy.context.active_object\n");
                defaultBpyScript.append("    u_arm.name = 'UpperArm_' + side\n");
                defaultBpyScript.append("    u_arm.data.materials.append(mat_suit)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.1, depth=0.45, location=(x * 1.1, 0, 1.8))\n");
                defaultBpyScript.append("    f_arm = bpy.context.active_object\n");
                defaultBpyScript.append("    f_arm.name = 'ForeArm_' + side\n");
                defaultBpyScript.append("    f_arm.data.materials.append(mat_suit)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.1, location=(x * 1.1, 0, 1.5))\n");
                defaultBpyScript.append("    hand = bpy.context.active_object\n");
                defaultBpyScript.append("    hand.name = 'Hand_' + side\n");
                defaultBpyScript.append("    hand.data.materials.append(mat_armor)\n\n");

                defaultBpyScript.append("# Legs & Boots\n");
                defaultBpyScript.append("for side, x in [('L', -0.22), ('R', 0.22)]:\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.14, depth=0.6, location=(x, 0, 1.3))\n");
                defaultBpyScript.append("    thigh = bpy.context.active_object\n");
                defaultBpyScript.append("    thigh.name = 'Thigh_' + side\n");
                defaultBpyScript.append("    thigh.data.materials.append(mat_suit)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.12, depth=0.6, location=(x, 0, 0.7))\n");
                defaultBpyScript.append("    calf = bpy.context.active_object\n");
                defaultBpyScript.append("    calf.name = 'Calf_' + side\n");
                defaultBpyScript.append("    calf.data.materials.append(mat_suit)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(x, -0.08, 0.15))\n");
                defaultBpyScript.append("    boot = bpy.context.active_object\n");
                defaultBpyScript.append("    boot.name = 'Boot_' + side\n");
                defaultBpyScript.append("    boot.scale = (0.16, 0.35, 0.2)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    boot.data.materials.append(mat_armor)\n\n");

                defaultBpyScript.append("# Join Hero Mesh Parts\n");
                defaultBpyScript.append("bpy.ops.object.select_all(action='DESELECT')\n");
                defaultBpyScript.append("for obj in bpy.data.objects:\n");
                defaultBpyScript.append("    if obj.type == 'MESH':\n");
                defaultBpyScript.append("        obj.select_set(True)\n");
                defaultBpyScript.append("bpy.context.view_layer.objects.active = chest\n");
                defaultBpyScript.append("bpy.ops.object.join()\n");
                defaultBpyScript.append("hero_mesh = bpy.context.active_object\n");
                defaultBpyScript.append("hero_mesh.name = 'Superhero_Mesh'\n\n");

                defaultBpyScript.append("# Build Skeletal Armature Rig with Context Safeguards\n");
                defaultBpyScript.append("bpy.ops.object.armature_add(location=(0, 0, 1.8))\n");
                defaultBpyScript.append("arm_obj = bpy.context.active_object\n");
                defaultBpyScript.append("arm_obj.name = 'Superhero_Rig'\n");
                defaultBpyScript.append("try:\n");
                defaultBpyScript.append("    bpy.context.view_layer.objects.active = arm_obj\n");
                defaultBpyScript.append("    bpy.ops.object.mode_set(mode='EDIT')\n");
                defaultBpyScript.append("    ebones = arm_obj.data.edit_bones\n");
                defaultBpyScript.append("    root_bone = ebones[0]\n");
                defaultBpyScript.append("    root_bone.name = 'Pelvis'\n");
                defaultBpyScript.append("    root_bone.head = (0, 0, 1.8)\n");
                defaultBpyScript.append("    root_bone.tail = (0, 0, 2.3)\n");
                defaultBpyScript.append("    spine = ebones.new('Spine')\n");
                defaultBpyScript.append("    spine.head = (0, 0, 2.3)\n");
                defaultBpyScript.append("    spine.tail = (0, 0, 2.85)\n");
                defaultBpyScript.append("    spine.parent = root_bone\n");
                defaultBpyScript.append("    bpy.ops.object.mode_set(mode='OBJECT')\n");
                defaultBpyScript.append("except Exception as be:\n");
                defaultBpyScript.append("    print(f'Armature setup note: {be}')\n\n");

                defaultBpyScript.append("# Safe Auto-Parenting\n");
                defaultBpyScript.append("try:\n");
                defaultBpyScript.append("    bpy.ops.object.select_all(action='DESELECT')\n");
                defaultBpyScript.append("    hero_mesh.select_set(True)\n");
                defaultBpyScript.append("    arm_obj.select_set(True)\n");
                defaultBpyScript.append("    bpy.context.view_layer.objects.active = arm_obj\n");
                defaultBpyScript.append("    bpy.ops.object.parent_set(type='ARMATURE_AUTO')\n");
                defaultBpyScript.append("except Exception as pe:\n");
                defaultBpyScript.append("    print(f'Parenting note: {pe}')\n");
                defaultBpyScript.append("    hero_mesh.parent = arm_obj\n\n");

            } else if (isDogDemo) {
                defaultBpyScript.append("# --- High-Detail Procedural Rigged Canine Quadruped ---\n");
                defaultBpyScript.append("mat_fur = bpy.data.materials.new(name='Dog_Fur')\n");
                defaultBpyScript.append("mat_fur.use_nodes = True\n");
                defaultBpyScript.append("bsdf_fur = mat_fur.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bsdf_fur:\n");
                defaultBpyScript.append("    bsdf_fur.inputs['Base Color'].default_value = (0.55, 0.32, 0.15, 1.0)\n");
                defaultBpyScript.append("    bsdf_fur.inputs['Roughness'].default_value = 0.85\n\n");

                defaultBpyScript.append("# Dog Torso / Ribcage\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 1.0))\n");
                defaultBpyScript.append("body = bpy.context.active_object\n");
                defaultBpyScript.append("body.name = 'Dog_Body'\n");
                defaultBpyScript.append("body.scale = (0.5, 1.2, 0.55)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("body.data.materials.append(mat_fur)\n\n");

                defaultBpyScript.append("# Neck and Head\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, -0.65, 1.35))\n");
                defaultBpyScript.append("head = bpy.context.active_object\n");
                defaultBpyScript.append("head.name = 'Dog_Head'\n");
                defaultBpyScript.append("head.scale = (0.35, 0.45, 0.35)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("head.data.materials.append(mat_fur)\n\n");

                defaultBpyScript.append("# Snout\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, -0.95, 1.25))\n");
                defaultBpyScript.append("snout = bpy.context.active_object\n");
                defaultBpyScript.append("snout.name = 'Dog_Snout'\n");
                defaultBpyScript.append("snout.scale = (0.22, 0.3, 0.2)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("snout.data.materials.append(mat_fur)\n\n");

                defaultBpyScript.append("# 4 Legs & Paws\n");
                defaultBpyScript.append("leg_positions = [('FL', -0.25, -0.45), ('FR', 0.25, -0.45), ('BL', -0.25, 0.45), ('BR', 0.25, 0.45)]\n");
                defaultBpyScript.append("for name, lx, ly in leg_positions:\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=0.8, location=(lx, ly, 0.5))\n");
                defaultBpyScript.append("    leg = bpy.context.active_object\n");
                defaultBpyScript.append("    leg.name = 'Leg_' + name\n");
                defaultBpyScript.append("    leg.data.materials.append(mat_fur)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(lx, ly - 0.05, 0.1))\n");
                defaultBpyScript.append("    paw = bpy.context.active_object\n");
                defaultBpyScript.append("    paw.name = 'Paw_' + name\n");
                defaultBpyScript.append("    paw.scale = (0.12, 0.18, 0.1)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    paw.data.materials.append(mat_fur)\n\n");

                defaultBpyScript.append("# Tail\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cylinder_add(radius=0.05, depth=0.6, location=(0, 0.75, 1.2))\n");
                defaultBpyScript.append("tail = bpy.context.active_object\n");
                defaultBpyScript.append("tail.rotation_euler = (0.7, 0, 0)\n");
                defaultBpyScript.append("tail.name = 'Dog_Tail'\n");
                defaultBpyScript.append("tail.data.materials.append(mat_fur)\n\n");

                defaultBpyScript.append("# Join Canine Mesh\n");
                defaultBpyScript.append("bpy.ops.object.select_all(action='DESELECT')\n");
                defaultBpyScript.append("for obj in bpy.data.objects:\n");
                defaultBpyScript.append("    if obj.type == 'MESH':\n");
                defaultBpyScript.append("        obj.select_set(True)\n");
                defaultBpyScript.append("bpy.context.view_layer.objects.active = body\n");
                defaultBpyScript.append("bpy.ops.object.join()\n");
                defaultBpyScript.append("dog_mesh = bpy.context.active_object\n");
                defaultBpyScript.append("dog_mesh.name = 'Canine_Mesh'\n\n");

                defaultBpyScript.append("# Skeletal Armature Rig with Context Safeguards\n");
                defaultBpyScript.append("bpy.ops.object.armature_add(location=(0, 0, 1.0))\n");
                defaultBpyScript.append("arm_obj = bpy.context.active_object\n");
                defaultBpyScript.append("arm_obj.name = 'Canine_Rig'\n");
                defaultBpyScript.append("try:\n");
                defaultBpyScript.append("    bpy.context.view_layer.objects.active = arm_obj\n");
                defaultBpyScript.append("    bpy.ops.object.mode_set(mode='EDIT')\n");
                defaultBpyScript.append("    ebones = arm_obj.data.edit_bones\n");
                defaultBpyScript.append("    spine = ebones[0]\n");
                defaultBpyScript.append("    spine.name = 'Spine'\n");
                defaultBpyScript.append("    spine.head = (0, 0.5, 1.0)\n");
                defaultBpyScript.append("    spine.tail = (0, -0.5, 1.0)\n");
                defaultBpyScript.append("    bpy.ops.object.mode_set(mode='OBJECT')\n");
                defaultBpyScript.append("except Exception as ce:\n");
                defaultBpyScript.append("    print(f'Canine armature note: {ce}')\n\n");

                defaultBpyScript.append("# Safe Auto-Parenting\n");
                defaultBpyScript.append("try:\n");
                defaultBpyScript.append("    bpy.ops.object.select_all(action='DESELECT')\n");
                defaultBpyScript.append("    dog_mesh.select_set(True)\n");
                defaultBpyScript.append("    arm_obj.select_set(True)\n");
                defaultBpyScript.append("    bpy.context.view_layer.objects.active = arm_obj\n");
                defaultBpyScript.append("    bpy.ops.object.parent_set(type='ARMATURE_AUTO')\n");
                defaultBpyScript.append("except Exception as dpe:\n");
                defaultBpyScript.append("    print(f'Dog parenting note: {dpe}')\n");
                defaultBpyScript.append("    dog_mesh.parent = arm_obj\n\n");

            } else if (isVillaDemo) {
                defaultBpyScript.append("# --- High-Detail Architectural Modern Villa & Pool ---\n");
                // Rich PBR Materials
                defaultBpyScript.append("mat_stucco = bpy.data.materials.new('Villa_WarmStucco')\n");
                defaultBpyScript.append("mat_stucco.use_nodes = True\n");
                defaultBpyScript.append("bs_s = mat_stucco.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_s: bs_s.inputs['Base Color'].default_value = (0.88, 0.86, 0.82, 1.0); bs_s.inputs['Roughness'].default_value = 0.45\n\n");

                defaultBpyScript.append("mat_wood = bpy.data.materials.new('Timber_DarkTeak')\n");
                defaultBpyScript.append("mat_wood.use_nodes = True\n");
                defaultBpyScript.append("bs_w = mat_wood.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_w: bs_w.inputs['Base Color'].default_value = (0.32, 0.18, 0.09, 1.0); bs_w.inputs['Roughness'].default_value = 0.55\n\n");

                defaultBpyScript.append("mat_stone = bpy.data.materials.new('Dark_Basalt_Stone')\n");
                defaultBpyScript.append("mat_stone.use_nodes = True\n");
                defaultBpyScript.append("bs_st = mat_stone.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_st: bs_st.inputs['Base Color'].default_value = (0.12, 0.13, 0.15, 1.0); bs_st.inputs['Roughness'].default_value = 0.35\n\n");

                defaultBpyScript.append("mat_glass = bpy.data.materials.new('Architectural_Glass')\n");
                defaultBpyScript.append("mat_glass.use_nodes = True\n");
                defaultBpyScript.append("bs_g = mat_glass.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_g:\n");
                defaultBpyScript.append("    bs_g.inputs['Base Color'].default_value = (0.85, 0.92, 1.0, 0.2)\n");
                defaultBpyScript.append("    if 'Transmission Weight' in bs_g.inputs: bs_g.inputs['Transmission Weight'].default_value = 0.95\n");
                defaultBpyScript.append("    elif 'Transmission' in bs_g.inputs: bs_g.inputs['Transmission'].default_value = 0.95\n");
                defaultBpyScript.append("    bs_g.inputs['Roughness'].default_value = 0.02\n\n");

                defaultBpyScript.append("mat_frame = bpy.data.materials.new('Black_Aluminum_Frames')\n");
                defaultBpyScript.append("mat_frame.use_nodes = True\n");
                defaultBpyScript.append("bs_f = mat_frame.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_f: bs_f.inputs['Base Color'].default_value = (0.05, 0.05, 0.06, 1.0); bs_f.inputs['Metallic'].default_value = 0.85; bs_f.inputs['Roughness'].default_value = 0.2\n\n");

                defaultBpyScript.append("mat_water = bpy.data.materials.new('Pool_Turquoise_Water')\n");
                defaultBpyScript.append("mat_water.use_nodes = True\n");
                defaultBpyScript.append("bs_wt = mat_water.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_wt:\n");
                defaultBpyScript.append("    bs_wt.inputs['Base Color'].default_value = (0.02, 0.55, 0.75, 0.8)\n");
                defaultBpyScript.append("    bs_wt.inputs['Roughness'].default_value = 0.05\n");
                defaultBpyScript.append("    if 'Transmission Weight' in bs_wt.inputs: bs_wt.inputs['Transmission Weight'].default_value = 0.95\n\n");

                defaultBpyScript.append("mat_warm_led = bpy.data.materials.new('Warm_Roof_LED')\n");
                defaultBpyScript.append("mat_warm_led.use_nodes = True\n");
                defaultBpyScript.append("bs_wl = mat_warm_led.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs_wl:\n");
                defaultBpyScript.append("    bs_wl.inputs['Base Color'].default_value = (1.0, 0.75, 0.4, 1.0)\n");
                defaultBpyScript.append("    if 'Emission Color' in bs_wl.inputs: bs_wl.inputs['Emission Color'].default_value = (1.0, 0.75, 0.4, 1.0); bs_wl.inputs['Emission Strength'].default_value = 15.0\n");
                defaultBpyScript.append("    elif 'Emission' in bs_wl.inputs: bs_wl.inputs['Emission'].default_value = (1.0, 0.75, 0.4, 1.0)\n\n");

                // Main Architectural Foundations
                defaultBpyScript.append("# 1. Dark Basalt Pool Patio Base\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, -0.2))\n");
                defaultBpyScript.append("patio = bpy.context.active_object\n");
                defaultBpyScript.append("patio.name = 'Basalt_Patio_Base'\n");
                defaultBpyScript.append("patio.scale = (26, 20, 0.4)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("patio.data.materials.append(mat_stone)\n\n");

                defaultBpyScript.append("# 2. Warm Wooden Pool Deck Terrace\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(-2, -1, 0.1))\n");
                defaultBpyScript.append("wood_deck = bpy.context.active_object\n");
                defaultBpyScript.append("wood_deck.name = 'Warm_Timber_Deck'\n");
                defaultBpyScript.append("wood_deck.scale = (14, 16, 0.2)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("wood_deck.data.materials.append(mat_wood)\n\n");

                defaultBpyScript.append("# 3. Ground Floor Stucco Pavilion with Bevel\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(-3, 2, 1.8))\n");
                defaultBpyScript.append("ground_pav = bpy.context.active_object\n");
                defaultBpyScript.append("ground_pav.name = 'Ground_Living_Pavilion'\n");
                defaultBpyScript.append("ground_pav.scale = (12, 9, 3.2)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("bev_g = ground_pav.modifiers.new('Bevel', 'BEVEL')\n");
                defaultBpyScript.append("bev_g.width = 0.05\n");
                defaultBpyScript.append("ground_pav.data.materials.append(mat_stucco)\n\n");

                defaultBpyScript.append("# 4. Cantilevered Upper Suite Floor\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(-1, 3.2, 4.6))\n");
                defaultBpyScript.append("upper_suite = bpy.context.active_object\n");
                defaultBpyScript.append("upper_suite.name = 'Upper_Cantilever_Suite'\n");
                defaultBpyScript.append("upper_suite.scale = (11, 8.5, 2.4)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("bev_u = upper_suite.modifiers.new('Bevel', 'BEVEL')\n");
                defaultBpyScript.append("bev_u.width = 0.05\n");
                defaultBpyScript.append("upper_suite.data.materials.append(mat_stucco)\n\n");

                defaultBpyScript.append("# 5. Vertical Timber Wood Slats (Facade Louvers)\n");
                defaultBpyScript.append("for s_idx in range(16):\n");
                defaultBpyScript.append("    sx = -5.5 + (s_idx * 0.45)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(sx, -1.08, 4.6))\n");
                defaultBpyScript.append("    slat = bpy.context.active_object\n");
                defaultBpyScript.append("    slat.name = f'Timber_Slat_{s_idx}'\n");
                defaultBpyScript.append("    slat.scale = (0.12, 0.22, 2.3)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    slat.data.materials.append(mat_wood)\n\n");

                defaultBpyScript.append("# 6. Black Aluminum Window Frames & Glass Curtain Facade\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(2.98, -2.48, 1.8))\n");
                defaultBpyScript.append("w_frame = bpy.context.active_object\n");
                defaultBpyScript.append("w_frame.name = 'Window_Frame_Border'\n");
                defaultBpyScript.append("w_frame.scale = (0.1, 7.8, 2.8)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("w_frame.data.materials.append(mat_frame)\n\n");

                defaultBpyScript.append("bpy.ops.mesh.primitive_plane_add(size=1, location=(3.02, -2.48, 1.8))\n");
                defaultBpyScript.append("glass = bpy.context.active_object\n");
                defaultBpyScript.append("glass.name = 'Glass_Curtain_Wall'\n");
                defaultBpyScript.append("glass.rotation_euler = (0, 1.5708, 0)\n");
                defaultBpyScript.append("glass.scale = (2.7, 7.6, 1.0)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("glass.data.materials.append(mat_glass)\n\n");

                defaultBpyScript.append("# 7. Upper Balcony Glass Railing\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(-1, -1.02, 3.8))\n");
                defaultBpyScript.append("railing = bpy.context.active_object\n");
                defaultBpyScript.append("railing.name = 'Balcony_Glass_Railing'\n");
                defaultBpyScript.append("railing.scale = (10.8, 0.08, 0.9)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("railing.data.materials.append(mat_glass)\n\n");

                defaultBpyScript.append("# 8. Recessed Warm LED Strip Under Roof Overhang\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(-1, -1.05, 5.82))\n");
                defaultBpyScript.append("roof_led = bpy.context.active_object\n");
                defaultBpyScript.append("roof_led.name = 'Warm_Roof_LED_Strip'\n");
                defaultBpyScript.append("roof_led.scale = (11.2, 0.15, 0.06)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("roof_led.data.materials.append(mat_warm_led)\n\n");

                defaultBpyScript.append("# 9. Recessed Swimming Pool Basin & Turquoise Water\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(6.5, 0, -0.4))\n");
                defaultBpyScript.append("pool_basin = bpy.context.active_object\n");
                defaultBpyScript.append("pool_basin.name = 'Pool_Basin'\n");
                defaultBpyScript.append("pool_basin.scale = (6.2, 13.0, 1.0)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("pool_basin.data.materials.append(mat_stone)\n\n");

                defaultBpyScript.append("bpy.ops.mesh.primitive_plane_add(size=1, location=(6.5, 0, 0.02))\n");
                defaultBpyScript.append("water = bpy.context.active_object\n");
                defaultBpyScript.append("water.name = 'Pool_Water_Surface'\n");
                defaultBpyScript.append("water.scale = (5.8, 12.6, 1.0)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("water.data.materials.append(mat_water)\n\n");

                defaultBpyScript.append("# 10. Poolside Woven Sun Loungers\n");
                defaultBpyScript.append("for l_idx, ly in enumerate([-3.5, 1.5]):\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(2.2, ly, 0.35))\n");
                defaultBpyScript.append("    lounger = bpy.context.active_object\n");
                defaultBpyScript.append("    lounger.name = f'Sun_Lounger_{l_idx}'\n");
                defaultBpyScript.append("    lounger.scale = (1.2, 2.4, 0.3)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    bev_l = lounger.modifiers.new('Bevel', 'BEVEL')\n");
                defaultBpyScript.append("    bev_l.width = 0.06\n");
                defaultBpyScript.append("    lounger.data.materials.append(mat_wood)\n\n");

            } else if (isSofaDemo) {
                defaultBpyScript.append("# --- High-End Luxury Leather Sofa ---\n");
                defaultBpyScript.append("mat_leather = bpy.data.materials.new('Brown_Leather')\n");
                defaultBpyScript.append("mat_leather.use_nodes = True\n");
                defaultBpyScript.append("bl = mat_leather.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bl:\n");
                defaultBpyScript.append("    bl.inputs['Base Color'].default_value = (0.22, 0.12, 0.06, 1.0)\n");
                defaultBpyScript.append("    bl.inputs['Roughness'].default_value = 0.45\n\n");

                defaultBpyScript.append("mat_metal = bpy.data.materials.new('Polished_Chrome')\n");
                defaultBpyScript.append("mat_metal.use_nodes = True\n");
                defaultBpyScript.append("bm = mat_metal.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bm:\n");
                defaultBpyScript.append("    bm.inputs['Base Color'].default_value = (0.85, 0.85, 0.88, 1.0)\n");
                defaultBpyScript.append("    bm.inputs['Metallic'].default_value = 0.95\n");
                defaultBpyScript.append("    bm.inputs['Roughness'].default_value = 0.1\n\n");

                defaultBpyScript.append("# Base Plinth\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 0.35))\n");
                defaultBpyScript.append("base = bpy.context.active_object\n");
                defaultBpyScript.append("base.name = 'Sofa_Base'\n");
                defaultBpyScript.append("base.scale = (2.6, 1.1, 0.25)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("base.data.materials.append(mat_leather)\n\n");

                defaultBpyScript.append("# Dual Cushioned Backrests\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_cube_add(size=1, location=(0, -0.42, 0.9))\n");
                defaultBpyScript.append("back = bpy.context.active_object\n");
                defaultBpyScript.append("back.name = 'Sofa_Backrest'\n");
                defaultBpyScript.append("back.scale = (2.5, 0.25, 0.85)\n");
                defaultBpyScript.append("bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("mod_b = back.modifiers.new('Bevel', 'BEVEL')\n");
                defaultBpyScript.append("mod_b.width = 0.08\n");
                defaultBpyScript.append("back.data.materials.append(mat_leather)\n\n");

                defaultBpyScript.append("# Plump Deep Seat Cushions\n");
                defaultBpyScript.append("for idx, px in enumerate([-0.65, 0.65]):\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(px, 0.12, 0.58))\n");
                defaultBpyScript.append("    cushion = bpy.context.active_object\n");
                defaultBpyScript.append("    cushion.name = 'Seat_Cushion_' + str(idx)\n");
                defaultBpyScript.append("    cushion.scale = (1.18, 0.85, 0.26)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    c_mod = cushion.modifiers.new('Bevel', 'BEVEL')\n");
                defaultBpyScript.append("    c_mod.width = 0.07\n");
                defaultBpyScript.append("    cushion.data.materials.append(mat_leather)\n\n");

                defaultBpyScript.append("# Sculpted Armrests\n");
                defaultBpyScript.append("for side, sx in [('L', -1.35), ('R', 1.35)]:\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(sx, 0, 0.7))\n");
                defaultBpyScript.append("    arm = bpy.context.active_object\n");
                defaultBpyScript.append("    arm.name = 'Armrest_' + side\n");
                defaultBpyScript.append("    arm.scale = (0.24, 1.15, 0.6)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    arm.data.materials.append(mat_leather)\n\n");

                defaultBpyScript.append("# Polished Metal Legs\n");
                defaultBpyScript.append("leg_coords = [(-1.25, -0.45), (1.25, -0.45), (-1.25, 0.45), (1.25, 0.45)]\n");
                defaultBpyScript.append("for lx, ly in leg_coords:\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.035, depth=0.25, location=(lx, ly, 0.125))\n");
                defaultBpyScript.append("    leg = bpy.context.active_object\n");
                defaultBpyScript.append("    leg.name = 'Sofa_Leg'\n");
                defaultBpyScript.append("    leg.data.materials.append(mat_metal)\n\n");

            } else if (isVillageDemo) {
                defaultBpyScript.append("# --- Procedural Detailed Tropical Village & Palm Beach ---\n");
                defaultBpyScript.append("mat_sand = bpy.data.materials.new('Sand_Terrain')\n");
                defaultBpyScript.append("mat_sand.use_nodes = True\n");
                defaultBpyScript.append("bs = mat_sand.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bs: bs.inputs['Base Color'].default_value = (0.86, 0.78, 0.58, 1.0)\n\n");

                defaultBpyScript.append("mat_wood = bpy.data.materials.new('Timber_Wood')\n");
                defaultBpyScript.append("mat_wood.use_nodes = True\n");
                defaultBpyScript.append("bw = mat_wood.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bw: bw.inputs['Base Color'].default_value = (0.42, 0.28, 0.16, 1.0)\n\n");

                defaultBpyScript.append("mat_thatch = bpy.data.materials.new('Thatch_Roof')\n");
                defaultBpyScript.append("mat_thatch.use_nodes = True\n");
                defaultBpyScript.append("bt = mat_thatch.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bt: bt.inputs['Base Color'].default_value = (0.6, 0.52, 0.3, 1.0)\n\n");

                defaultBpyScript.append("# Expansive Coastline Island Terrain\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_plane_add(size=36, location=(0, 0, 0))\n");
                defaultBpyScript.append("terrain = bpy.context.active_object\n");
                defaultBpyScript.append("terrain.name = 'Sand_Shoreline'\n");
                defaultBpyScript.append("terrain.data.materials.append(mat_sand)\n\n");

                defaultBpyScript.append("# Ocean Shore Water\n");
                defaultBpyScript.append("bpy.ops.mesh.primitive_plane_add(size=36, location=(0, -18, -0.1))\n");
                defaultBpyScript.append("ocean = bpy.context.active_object\n");
                defaultBpyScript.append("ocean.name = 'Ocean_Water'\n");
                defaultBpyScript.append("mat_ocean = bpy.data.materials.new('Ocean_Mat')\n");
                defaultBpyScript.append("mat_ocean.use_nodes = True\n");
                defaultBpyScript.append("bo = mat_ocean.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("if bo: bo.inputs['Base Color'].default_value = (0.05, 0.6, 0.75, 0.8)\n");
                defaultBpyScript.append("ocean.data.materials.append(mat_ocean)\n\n");

                defaultBpyScript.append("# Elevated Wooden Stilt Huts\n");
                defaultBpyScript.append("hut_clusters = [(-6, 2), (5, 4), (0, 8)]\n");
                defaultBpyScript.append("for idx, (hx, hy) in enumerate(hut_clusters):\n");
                defaultBpyScript.append("    for sx, sy in [(-1.5, -1.5), (1.5, -1.5), (-1.5, 1.5), (1.5, 1.5)]:\n");
                defaultBpyScript.append("        bpy.ops.mesh.primitive_cylinder_add(radius=0.1, depth=1.4, location=(hx + sx, hy + sy, 0.7))\n");
                defaultBpyScript.append("        stilt = bpy.context.active_object\n");
                defaultBpyScript.append("        stilt.data.materials.append(mat_wood)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(hx, hy, 1.4))\n");
                defaultBpyScript.append("    h_deck = bpy.context.active_object\n");
                defaultBpyScript.append("    h_deck.scale = (3.6, 3.6, 0.2)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    h_deck.data.materials.append(mat_wood)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cube_add(size=1, location=(hx, hy, 2.5))\n");
                defaultBpyScript.append("    h_wall = bpy.context.active_object\n");
                defaultBpyScript.append("    h_wall.scale = (3.0, 3.0, 2.0)\n");
                defaultBpyScript.append("    bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("    h_wall.data.materials.append(mat_wood)\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cone_add(radius1=2.6, depth=1.8, location=(hx, hy, 4.2))\n");
                defaultBpyScript.append("    roof = bpy.context.active_object\n");
                defaultBpyScript.append("    roof.name = 'Thatched_Roof_' + str(idx)\n");
                defaultBpyScript.append("    roof.data.materials.append(mat_thatch)\n\n");

                defaultBpyScript.append("# Curved Palm Trees with Canopy\n");
                defaultBpyScript.append("tree_locations = [(-11, -3), (10, 0), (-3, -5), (8, -6)]\n");
                defaultBpyScript.append("for tx, ty in tree_locations:\n");
                defaultBpyScript.append("    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=4.8, location=(tx, ty, 2.4))\n");
                defaultBpyScript.append("    trunk = bpy.context.active_object\n");
                defaultBpyScript.append("    trunk.rotation_euler = (random.uniform(-0.15, 0.15), random.uniform(-0.15, 0.15), 0)\n");
                defaultBpyScript.append("    trunk.data.materials.append(mat_wood)\n");
                defaultBpyScript.append("    for r_angle in [0, 1.05, 2.09, 3.14, 4.19, 5.24]:\n");
                defaultBpyScript.append("        bpy.ops.mesh.primitive_plane_add(size=1, location=(tx, ty, 4.8))\n");
                defaultBpyScript.append("        leaf = bpy.context.active_object\n");
                defaultBpyScript.append("        leaf.scale = (0.5, 2.2, 1.0)\n");
                defaultBpyScript.append("        leaf.rotation_euler = (0.35, 0, r_angle)\n");
                defaultBpyScript.append("        bpy.ops.object.transform_apply(scale=True)\n");
                defaultBpyScript.append("        mat_leaf = bpy.data.materials.new('Palm_Leaf')\n");
                defaultBpyScript.append("        mat_leaf.use_nodes = True\n");
                defaultBpyScript.append("        bleaf = mat_leaf.node_tree.nodes.get('Principled BSDF')\n");
                defaultBpyScript.append("        if bleaf: bleaf.inputs['Base Color'].default_value = (0.15, 0.55, 0.18, 1.0)\n");
                defaultBpyScript.append("        leaf.data.materials.append(mat_leaf)\n\n");

            } else {
                defaultBpyScript.append("# --- Dynamic AI Asset Initializer ---\n");
                defaultBpyScript.append("# Contextual asset initialization (Real-time code authored by Dynamic Script Writer)\n");
            }

            defaultBpyScript.append("# Cinematic Sun & Fill Lighting\n");
            defaultBpyScript.append("bpy.ops.object.light_add(type='SUN', location=(8, -8, 14))\n");
            defaultBpyScript.append("sun = bpy.context.active_object\n");
            defaultBpyScript.append("sun.data.energy = 4.5\n");
            defaultBpyScript.append("bpy.ops.object.light_add(type='POINT', location=(-6, 6, 8))\n");
            defaultBpyScript.append("fill = bpy.context.active_object\n");
            defaultBpyScript.append("fill.data.energy = 800.0\n\n");

            // Setup Scene Camera for Preview Rendering
            defaultBpyScript.append("# Setup Scene Camera for Preview Rendering\n");
            defaultBpyScript.append("try:\n");
            defaultBpyScript.append("    if not bpy.context.scene.camera:\n");
            defaultBpyScript.append("        cam_data = bpy.data.cameras.new('SceneCamera')\n");
            defaultBpyScript.append("        cam_data.lens = 38.0\n");
            defaultBpyScript.append("        cam_obj = bpy.data.objects.new('Camera', cam_data)\n");
            defaultBpyScript.append("        bpy.context.collection.objects.link(cam_obj)\n");
            defaultBpyScript.append("        bpy.context.scene.camera = cam_obj\n");
            defaultBpyScript.append("        cam_obj.location = (0, -22, 10)\n");
            defaultBpyScript.append("        cam_obj.rotation_euler = (math.radians(65), 0, 0)\n");
            defaultBpyScript.append("except Exception as ce: print(f'Camera setup note: {ce}')\n\n");

            // 1. ALWAYS Export 3D GLTF Model FIRST (Guarantees model.glb exists on disk)
            defaultBpyScript.append("# Step 1: Export Interactive 3D Model (First Priority)\n");
            defaultBpyScript.append("try:\n");
            defaultBpyScript.append("    bpy.ops.export_scene.gltf(filepath='output/model.glb', export_format='GLB')\n");
            defaultBpyScript.append("    print('3D GLTF Export Successful: output/model.glb')\n");
            defaultBpyScript.append("except Exception as ge: print(f'GLTF export warning: {ge}')\n\n");

            // 2. Render Still Preview Image using Headless-Safe Cycles CPU Engine
            defaultBpyScript.append("# Step 2: Render Photorealistic Still Preview Image via CPU Cycles\n");
            defaultBpyScript.append("try:\n");
            defaultBpyScript.append("    if bpy.context.scene.camera:\n");
            defaultBpyScript.append("        bpy.context.scene.render.engine = 'CYCLES'\n");
            defaultBpyScript.append("        bpy.context.scene.cycles.device = 'CPU'\n");
            defaultBpyScript.append("        bpy.context.scene.cycles.samples = 16\n");
            defaultBpyScript.append("        bpy.context.scene.render.resolution_x = 1280\n");
            defaultBpyScript.append("        bpy.context.scene.render.resolution_y = 720\n");
            defaultBpyScript.append("        bpy.context.scene.render.filepath = 'output/render.png'\n");
            defaultBpyScript.append("        bpy.ops.render.render(write_still=True)\n");
            defaultBpyScript.append("        print('Cycles preview render complete: output/render.png')\n");
            defaultBpyScript.append("    else:\n");
            defaultBpyScript.append("        print('Skipping preview render: No active camera.')\n");
            defaultBpyScript.append("except Exception as re: print(f'Preview render note: {re}')\n");

            // Build ToolOperation incorporating both your procedural script AND the modular sub-scripts
            ToolOperation cloudOp = new ToolOperation("blender.cloud_generate")
                    .setParam("prompt", userPrompt)
                    .setParam("assetId", assetId)
                    .setParam("bpyScript", defaultBpyScript.toString())
                    .setParam("w1HeroScript", modularScripts.heroScript)
                    .setParam("w2EnvScript", modularScripts.environmentScript)
                    .setParam("w3LightScript", modularScripts.lightingAndRenderScript)
                    .setParam("compositeMasterScript", modularScripts.compositeMasterScript)
                    .setParam("seedHero", spec.getSeedHero())
                    .setParam("seedVegetation", spec.getSeedVegetation())
                    .setParam("seedLighting", spec.getSeedLighting())
                    .setParam("directorSpec", spec.toJson().toString());

            TaskNode cloudNode = new TaskNode(cloudTaskId, "blender.cloud_generate",
                    "Execute procedural Blender script to sculpt 3D asset via headless worker",
                    cloudOp);

            cloudNode.addDependency(initialDependency);
            graph.addTask(cloudNode);
            geometryTaskIds.add(cloudTaskId);
            leafTaskIds.add(cloudTaskId);

        } else {
            // Generate local procedural creation steps for EVERY concept detected in the prompt
            for (KnowledgeEntry entry : matchedKnowledge) {
                String cat = entry.getCategory();
                String conceptId = entry.getId();

                if ("CHARACTER".equalsIgnoreCase(cat)) {
                    String tMeshId = "task_" + taskCounter++;
                    TaskNode tMesh = new TaskNode(tMeshId, "Generating " + entry.getName() + " Mesh", "Tool: character.create_humanoid",
                            new ToolOperation("character.create_humanoid").setParam("name", entry.getName()).setParam("style", style).setParam("height", 1.8f));
                    tMesh.addDependency(initialDependency);
                    graph.addTask(tMesh);
                    geometryTaskIds.add(tMeshId);

                    String tBindId = "task_" + taskCounter++;
                    TaskNode tBind = new TaskNode(tBindId, "Binding Skeleton & Skin Weights", "Tool: skeleton.bind",
                            new ToolOperation("skeleton.bind"));
                    tBind.addDependency(tMeshId);
                    graph.addTask(tBind);

                    String tRigId = "task_" + taskCounter++;
                    TaskNode tRig = new TaskNode(tRigId, "Configuring IK Limb Controllers", "Tool: rig.create_ik",
                            new ToolOperation("rig.create_ik").setParam("limb", "left_arm"));
                    tRig.addDependency(tBindId);
                    graph.addTask(tRig);

                    String tAnimId = "task_" + taskCounter++;
                    String clipName = userPrompt.toLowerCase().contains("run") ? "run" : (userPrompt.toLowerCase().contains("jump") ? "jump" : "walk");
                    TaskNode tAnim = new TaskNode(tAnimId, "Applying Animation Clip (" + clipName + ")", "Tool: animation.create_clip",
                            new ToolOperation("animation.create_clip").setParam("clipName", clipName));
                    tAnim.addDependency(tRigId);
                    graph.addTask(tAnim);

                    leafTaskIds.add(tAnimId);

                } else if ("ANIMAL".equalsIgnoreCase(cat)) {
                    String species = conceptId.contains("bird") ? "bird" : "dog";
                    String tCreatureId = "task_" + taskCounter++;
                    TaskNode tCreature = new TaskNode(tCreatureId, "Generating " + entry.getName() + " Anatomy", "Tool: character.create_creature",
                            new ToolOperation("character.create_creature").setParam("species", species).setParam("name", entry.getName()));
                    tCreature.addDependency(initialDependency);
                    graph.addTask(tCreature);
                    geometryTaskIds.add(tCreatureId);

                    String tAnimId = "task_" + taskCounter++;
                    TaskNode tAnim = new TaskNode(tAnimId, "Applying Locomotion Animation", "Tool: animation.create_clip",
                            new ToolOperation("animation.create_clip").setParam("clipName", "walk"));
                    tAnim.addDependency(tCreatureId);
                    graph.addTask(tAnim);

                    leafTaskIds.add(tAnimId);

                } else {
                    // Procedural Architecture, Furniture, Environment, or Vehicle
                    String tStructId = "task_" + taskCounter++;
                    TaskNode tStruct = new TaskNode(tStructId, "Building " + entry.getName(), "Tool: geometry.create_procedural",
                            new ToolOperation("geometry.create_procedural").setParam("type", conceptId).setParam("name", entry.getName()));
                    tStruct.addDependency(initialDependency);
                    graph.addTask(tStruct);
                    geometryTaskIds.add(tStructId);
                    
                    leafTaskIds.add(tStructId);
                }
            }
        }

        // Add Lighting Setup (Depends on all physical geometry structures being generated)
        String tLightId = "task_" + taskCounter++;
        TaskNode tLight = new TaskNode(tLightId, "Configuring Scene Lighting", "Tool: scene.add_light",
                new ToolOperation("scene.add_light").setParam("type", "directional").setParam("intensity", 1.2f).setParam("colorHex", "#FFF4E0"));
        for (String geomId : geometryTaskIds) {
            tLight.addDependency(geomId);
        }
        graph.addTask(tLight);

        // Add Validation Check Step (Must run strictly after all modifiers, shapes, and lighting tasks are completed)
        String tValidId = "task_" + taskCounter;
        TaskNode tValid = new TaskNode(tValidId, "Inspecting Mesh & Scene Integrity", "Tool: validation.check_mesh",
                new ToolOperation("validation.check_mesh"));
        for (String leafId : leafTaskIds) {
            tValid.addDependency(leafId);
        }
        tValid.addDependency(tLightId);
        graph.addTask(tValid);

        return plan;
    }

    /**
     * Converts Gemini's structured JSON output into an executable TaskGraph.
     * Generates a fully parallelized Directed Acyclic Graph (DAG) using explicit and semantic rules.
     */
    public ProductionPlan convertStructuredPlanToExecutablePlan(AIProductionRequest request, AIProductionPlan structuredPlan) {
        if (structuredPlan == null || request == null) {
            return createProductionPlan(request != null ? request.getUserPrompt() : "", "Photorealistic", "OpenGL ES / GLTF");
        }

        KnowledgeEntry knowledge = knowledgeManager.retrieveKnowledgeForPrompt(request.getUserPrompt());
        String projectName = extractProjectName(request.getUserPrompt(), structuredPlan.getIntent());
        ProductionPlan plan = new ProductionPlan(projectName, request.getUserPrompt(), structuredPlan.getIntent(), knowledge, request.getReferenceImageUris());
        TaskGraph graph = plan.getTaskGraph();

        List<AIToolCall> toolCalls = structuredPlan.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return createProductionPlan(request.getUserPrompt(), request.getStyle(), request.getTargetEngine(), request.getReferenceImageUris());
        }

        // Initial Canvas Reset Root Task
        String rootClearId = "task_ai_root_clear";
        TaskNode rootClearNode = new TaskNode(rootClearId, "Resetting Scene Canvas", "Tool: scene.clear",
                new ToolOperation("scene.clear"));
        graph.addTask(rootClearNode);

        // ENFORCE CONTRACT: Run raw Gemini tool calls through PlanValidator to resolve capabilities
        ToolRegistry toolRegistry = new ToolRegistry();
        PlanValidator planValidator = new PlanValidator(toolRegistry);
        List<AIToolCall> validatedToolCalls = planValidator.validateAndMap(toolCalls);

        // Keep track of the last geometry creation task to build local modifiers dependency mapping
        String lastGeometryTaskId = null;
        Map<String, TaskNode> taskNodeMap = new HashMap<>();

        for (int i = 0; i < validatedToolCalls.size(); i++) {
            AIToolCall call = validatedToolCalls.get(i);
            String defaultTaskId = "task_ai_" + (i + 1);
            
            ToolOperation op = new ToolOperation(call.getToolId());
            if (call.getParameters() != null) {
                for (Map.Entry<String, Object> entry : call.getParameters().entrySet()) {
                    op.setParam(entry.getKey(), entry.getValue());
                }
            }

            // Read explicit custom ID from AI, fallback to default sequential ID
            String taskId = op.getStringParam("id", defaultTaskId);

            String desc = call.getDescription() != null && !call.getDescription().isEmpty()
                    ? call.getDescription()
                    : "Executing tool: " + call.getToolId();

            TaskNode node = new TaskNode(taskId, call.getToolId(), desc, op);
            taskNodeMap.put(taskId, node);
            
            List<String> dependencies = new ArrayList<>();

            // A. Look for explicit JSON dependencies defined by Gemini
            Object dependsOnObj = op.getParam("dependsOn", null);
            if (dependsOnObj instanceof String) {
                dependencies.add(((String) dependsOnObj).trim());
            } else if (dependsOnObj instanceof List) {
                for (Object item : (List<?>) dependsOnObj) {
                    if (item != null) dependencies.add(item.toString().trim());
                }
            } else if (dependsOnObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) dependsOnObj;
                for (int j = 0; j < arr.length(); j++) {
                    dependencies.add(arr.optString(j).trim());
                }
            }

            // B. Semantic Fallback: Map local dependencies if Gemini left "dependsOn" empty
            if (dependencies.isEmpty()) {
                String toolId = call.getToolId().toLowerCase();
                
                // Identify modification, skeleton, rig, animation, validation, and export tasks
                boolean isModifier = toolId.contains("material.") ||
                                     toolId.contains("skeleton.") ||
                                     toolId.contains("rig.") ||
                                     toolId.contains("animation.") ||
                                     toolId.contains("geometry.transform.") ||
                                     toolId.contains("validation.check_mesh") ||
                                     toolId.contains("export.gltf") ||
                                     toolId.contains("project.save");

                if (isModifier) {
                    if (lastGeometryTaskId != null) {
                        dependencies.add(lastGeometryTaskId);
                    } else {
                        dependencies.add(rootClearId);
                    }
                } else {
                    // Geometry or character creation tool: always depends on the canvas clear
                    dependencies.add(rootClearId);
                    boolean isGeometryCreator = toolId.contains("geometry.create_") ||
                                                toolId.contains("character.create_") ||
                                                toolId.contains("blender.cloud_generate");
                    if (isGeometryCreator) {
                        lastGeometryTaskId = taskId;
                    }
                }
            }

            // Apply calculated dependencies to the task node
            for (String depId : dependencies) {
                if (!depId.equals(taskId)) {
                    node.addDependency(depId);
                }
            }
            
            graph.addTask(node);
        }

        // C. Link the final quality validation check strictly to all leaf nodes (terminal branches) of the graph
        Set<String> dependencyTargets = new HashSet<>();
        for (TaskNode node : graph.getAllNodes()) {
            dependencyTargets.addAll(node.getDependencyTaskIds());
        }

        String finalValidationId = "task_ai_validation";
        TaskNode validationNode = new TaskNode(finalValidationId, "validation.check_mesh",
                "Inspecting Generated Scene Integrity", new ToolOperation("validation.check_mesh"));

        for (TaskNode node : graph.getAllNodes()) {
            if (!dependencyTargets.contains(node.getId()) && !node.getId().equals(rootClearId)) {
                validationNode.addDependency(node.getId());
            }
        }

        // Safety fallback if no leaf branches were identified
        if (validationNode.getDependencyTaskIds().isEmpty() && !graph.getAllNodes().isEmpty()) {
            validationNode.addDependency(graph.getAllNodes().get(graph.getAllNodes().size() - 1).getId());
        }

        graph.addTask(validationNode);

        return plan;
    }

    private String extractProjectName(String prompt, String category) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "3D Project";
        }
        String p = prompt.trim();
        if (p.length() > 28) {
            return p.substring(0, 25) + "...";
        }
        return p;
    }
}