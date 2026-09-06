package com.example.ai;

import com.example.ai.agents.BlenderWorkerAgent;
import com.example.ai.agents.DirectorAgent;
import com.example.ai.protocol.AIDirectorSpec;
import com.example.ai.protocol.AIProductionPlan;
import com.example.ai.protocol.AIProductionRequest;
import com.example.cloud.CloudProvider;
import com.example.knowledge.KnowledgeEntry;
import com.example.knowledge.KnowledgeManager;
import com.example.tasks.ProductionPlan;
import com.example.tasks.TaskNode;
import com.example.tools.ToolDefinition;
import com.example.tools.ToolOperation;
import com.example.tools.ToolParameter;
import com.example.tools.ToolRegistry;
import com.example.utils.VynaraLogger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AIOrchestrator {
    private final GeminiApiClient apiClient;
    private final ApiKeyManager apiKeyManager;
    private final KnowledgeManager knowledgeManager;
    private final PromptInterpreter promptInterpreter;
    private final DirectorAgent directorAgent;

    public AIOrchestrator(GeminiApiClient apiClient, ApiKeyManager apiKeyManager, KnowledgeManager knowledgeManager) {
        this.apiClient = apiClient;
        this.apiKeyManager = apiKeyManager;
        this.knowledgeManager = knowledgeManager;
        this.promptInterpreter = new PromptInterpreter(knowledgeManager);
        this.directorAgent = new DirectorAgent(apiClient, apiKeyManager);
    }

    public ProductionPlan planProduction(String userPrompt, String style, String targetEngine) {
        return planProduction(userPrompt, style, targetEngine, new ArrayList<>());
    }

    public ProductionPlan planProduction(String userPrompt, String style, String targetEngine, List<String> referenceImageUris) {
        return promptInterpreter.createProductionPlan(userPrompt, style, targetEngine, referenceImageUris);
    }

    /**
     * Checks if the user prompt is an exact match for one of the 5 curated Demo Presets.
     * Prevents keyword collision while guaranteeing the 5 showcase demos remain fully functional.
     */
    public static boolean isDemoPreset(String prompt) {
        if (prompt == null) return false;
        String p = prompt.trim().toLowerCase();
        return p.contains("realistic modern villa with a swimming pool")
                || p.contains("stylized rigged superhero character")
                || p.contains("animated quadruped dog model")
                || p.contains("modern luxury leather sofa")
                || p.contains("high-detail tropical village environment")
                || p.equals("modern villa & pool")
                || p.equals("modern villa & swimming pool")
                || p.equals("rigged superhero")
                || p.equals("animated dog")
                || p.equals("leather sofa")
                || p.equals("tropical village");
    }

    /**
     * Executes the Autonomous Production Pipeline:
     * Phase 1: DirectorAgent inspects prompt & visual references to formulate the spatial/visual contract.
     * Phase 2: If Demo Preset, uses the curated demo script. If Custom Prompt, dispatches live to Gemini
     *          to write 100% custom Blender Python (bpy) code from scratch.
     * Phase 3: Wraps the Python code with CPU-safe Cycles settings, camera rigs, and GLB export.
     */
    public void planProductionWithGemini(final AIProductionRequest request, final GeminiApiClient.ApiCallback<ProductionPlan> callback) {
        if (request == null || callback == null) return;

        if (!apiKeyManager.hasApiKey()) {
            String err = "Gemini API key is not configured in Settings. Generation halted.";
            VynaraLogger.e(err);
            callback.onError(err);
            return;
        }

        final boolean isBlenderNative = request.getTargetEngine() != null && 
                request.getTargetEngine().toLowerCase().contains("blender");

        VynaraLogger.system("AIOrchestrator: Initiating Phase 1 (Director Agent Specification)...");

        // Phase 1: Formulate Director Specification
        directorAgent.formulateDirectorSpec(
                request.getUserPrompt(),
                request.getStyle(),
                request.getReferenceImageUris(),
                new DirectorAgent.DirectorCallback() {
                    @Override
                    public void onSpecReady(final AIDirectorSpec directorSpec) {
                        VynaraLogger.system("AIOrchestrator: Phase 1 Complete. Formulating Phase 2 execution graph...");

                        if (isBlenderNative) {
                            final String assetId = "asset_" + System.currentTimeMillis();
                            final ProductionPlan plan = promptInterpreter.createProductionPlan(
                                    request.getUserPrompt(), request.getStyle(), request.getTargetEngine(), request.getReferenceImageUris());

                            // Check if this is an explicit demo preset without custom reference images
                            boolean isDemo = isDemoPreset(request.getUserPrompt()) && !request.hasReferenceImages();

                            if (isDemo) {
                                VynaraLogger.system("AIOrchestrator: Demo Preset recognized. Preserving curated demo production script.");
                                for (TaskNode node : plan.getTaskGraph().getAllNodes()) {
                                    if (node.getOperation() != null && 
                                            "blender.cloud_generate".equalsIgnoreCase(node.getOperation().getToolId())) {
                                        node.getOperation().setParam("assetId", assetId);
                                        node.getOperation().setParam("seedHero", directorSpec.getSeedHero());
                                        node.getOperation().setParam("seedVegetation", directorSpec.getSeedVegetation());
                                        node.getOperation().setParam("seedLighting", directorSpec.getSeedLighting());
                                        node.getOperation().setParam("directorSpec", directorSpec.toJson().toString());
                                    }
                                }
                                callback.onSuccess(plan);
                            } else {
                                // Phase 2: Dynamic AI Script Writer (Live Gemini Generation for custom creative prompts & reference images)
                                VynaraLogger.system("AIOrchestrator: Custom creative prompt detected. Engaging Phase 2 Dynamic AI Script Writer...");
                                dispatchDynamicScriptWriter(request.getUserPrompt(), request.getStyle(), directorSpec, new GeminiApiClient.ApiCallback<String>() {
                                    @Override
                                    public void onSuccess(String dynamicBpyCode) {
                                        // Phase 3: Local Safety Wrapper with CPU-Safe Settings and Standard GLB Export
                                        String finalMasterScript = wrapDynamicScriptWithSafety(dynamicBpyCode, directorSpec);

                                        for (TaskNode node : plan.getTaskGraph().getAllNodes()) {
                                            if (node.getOperation() != null && 
                                                    "blender.cloud_generate".equalsIgnoreCase(node.getOperation().getToolId())) {
                                                node.getOperation().setParam("assetId", assetId);
                                                node.getOperation().setParam("bpyScript", finalMasterScript);
                                                node.getOperation().setParam("w1HeroScript", dynamicBpyCode);
                                                node.getOperation().setParam("seedHero", directorSpec.getSeedHero());
                                                node.getOperation().setParam("seedVegetation", directorSpec.getSeedVegetation());
                                                node.getOperation().setParam("seedLighting", directorSpec.getSeedLighting());
                                                node.getOperation().setParam("directorSpec", directorSpec.toJson().toString());
                                            }
                                        }

                                        VynaraLogger.system("AIOrchestrator: Phase 3 Wrapper complete. Production plan ready for cloud dispatch.");
                                        callback.onSuccess(plan);
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        VynaraLogger.e("AIOrchestrator: Dynamic Script Writer failed: " + errorMessage);
                                        callback.onError(errorMessage);
                                    }
                                });
                            }
                        } else {
                            // Local OpenGL ES / GLTF execution path with Gemini planning
                            executeStructuredGeminiPlanning(request, directorSpec, callback);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        VynaraLogger.e("AIOrchestrator: Director Agent halted: " + errorMessage);
                        callback.onError(errorMessage);
                    }
                }
        );
    }

    /**
     * Phase 2: Dispatches a live request to Gemini to author 100% custom Blender Python (bpy) code from scratch.
     */
    private void dispatchDynamicScriptWriter(final String userPrompt, 
                                             final String style, 
                                             final AIDirectorSpec directorSpec, 
                                             final GeminiApiClient.ApiCallback<String> callback) {
        VynaraLogger.system("AIOrchestrator: Synthesizing live Blender Python code via Gemini Script Writer...");

        String systemInstruction = "You are an expert 3D modeling and rigging engineer using Blender's Python API (`bpy`).\n" +
                "Generate production-grade, error-free Python code for Blender 4.x/5.x to build the 3D model or scene requested.\n" +
                "CRITICAL SYNTAX & OPERATOR RULES:\n" +
                "1. Output ONLY executable Python code inside a single ```python code block. No explanations, no markdown outside the block.\n" +
                "2. Construct real, detailed, multi-part 3D geometry matching the user's prompt (e.g., car body, wheels, chassis, windows, walls, roofs, terrain, character anatomy).\n" +
                "3. Use modifiers where appropriate (Bevel, Subdivision Surface, Mirror, Solidify, Boolean).\n" +
                "4. Create Principled BSDF materials with realistic Base Color, Metallic, Roughness, and Transmission according to the Director Spec.\n" +
                "5. NEVER generate a generic single cube, bevelled box, or placeholder. Build authentic multi-component structures.\n" +
                "6. NEVER output unquoted f-strings like `fName_{i}`. All f-strings MUST have double quotes: `f\"Name_{i}\"` or use string concatenation `\"Name_\" + str(i)`.\n" +
                "7. Use correct standard Blender mesh operators: `bpy.ops.mesh.primitive_cube_add`, `bpy.ops.mesh.primitive_plane_add`, `bpy.ops.mesh.primitive_cylinder_add`, `bpy.ops.mesh.primitive_cone_add`, `bpy.ops.mesh.primitive_uv_sphere_add`. NEVER use `bpy.ops.object.mesh.` or invent `_create` operators.\n" +
                "8. Lighting & Camera operators: ALWAYS use `bpy.ops.object.light_add(type='SUN'|'POINT'|'SPOT'|'AREA', location=...)` and `bpy.ops.object.camera_add(location=...)`. NEVER use `bpy.ops.light.add`.\n" +
                "9. Do not include GUI/context-dependent operators that fail in headless mode (avoid bpy.ops.view3d, screen area operators).\n" +
                "10. Organize objects cleanly with descriptive names and parent them logically.";

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("USER PROMPT: ").append(userPrompt).append("\n");
        promptBuilder.append("STYLE: ").append(style).append("\n");
        promptBuilder.append("DIRECTOR SPECIFICATION:\n");
        promptBuilder.append("- Scene Type: ").append(directorSpec.getSceneType()).append("\n");
        promptBuilder.append("- Mood: ").append(directorSpec.getMood()).append("\n");
        promptBuilder.append("- Primary Color Hex: ").append(directorSpec.getPrimaryColorHex()).append("\n");
        promptBuilder.append("- Secondary Color Hex: ").append(directorSpec.getSecondaryColorHex()).append("\n");
        promptBuilder.append("- Camera Focal Length: ").append(directorSpec.getFocalLengthMm()).append("mm\n");
        promptBuilder.append("- Sun Intensity: ").append(directorSpec.getSunIntensity()).append("\n");
        promptBuilder.append("- Volumetric Fog: ").append(directorSpec.isUseVolumetrics()).append("\n");
        promptBuilder.append("- Hero Seed: ").append(directorSpec.getSeedHero()).append("\n");
        promptBuilder.append("- Environment Seed: ").append(directorSpec.getSeedVegetation()).append("\n\n");
        promptBuilder.append("Now generate the complete Blender Python script to sculpt and build this asset.");

        apiClient.generateContent(
                apiKeyManager.getApiKey(),
                apiKeyManager.getSelectedModel(),
                systemInstruction,
                promptBuilder.toString(),
                new GeminiApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        String cleaned = cleanPythonCode(result);
                        if (cleaned.isEmpty()) {
                            callback.onError("Gemini Script Writer returned empty code.");
                        } else {
                            VynaraLogger.system("AIOrchestrator: Phase 2 Complete. Dynamic Python script synthesized (" + cleaned.length() + " chars).");
                            callback.onSuccess(cleaned);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        VynaraLogger.e("AIOrchestrator: Phase 2 Script Writer failed: " + errorMessage);
                        callback.onError("AI Script Writer Error: " + errorMessage);
                    }
                }
        );
    }

    /**
     * Phase 3: Wraps Gemini's dynamic modeling script with headless scene initialization,
     * cinematic camera/lighting, CPU-safe Cycles settings, and standardized GLB export.
     */
    private String wrapDynamicScriptWithSafety(String dynamicCode, AIDirectorSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ==========================================\n");
        sb.append("# Vynara Autonomous 3D Studio - Dynamic AI Build\n");
        sb.append("# Scene Type: ").append(spec != null ? spec.getSceneType() : "Custom").append("\n");
        sb.append("# ==========================================\n\n");
        sb.append("import bpy\n");
        sb.append("import os\n");
        sb.append("import math\n");
        sb.append("import random\n");
        sb.append("import sys\n");
        sb.append("import addon_utils\n\n");
        sb.append("try:\n");
        sb.append("    addon_utils.enable('archimesh')\n");
        sb.append("    addon_utils.enable('rigify')\n");
        sb.append("except Exception as e:\n");
        sb.append("    print(f'Addon activation note: {e}')\n\n");
        sb.append("os.makedirs('output', exist_ok=True)\n\n");
        sb.append("# Reset scene completely\n");
        sb.append("bpy.ops.object.select_all(action='SELECT')\n");
        sb.append("bpy.ops.object.delete(use_global=False)\n\n");

        sb.append("# --- DYNAMIC AI MESH GENERATION ---\n");
        sb.append(dynamicCode).append("\n\n");

        sb.append("# --- CINEMATIC LIGHTING & CAMERA RIG ---\n");
        float focalLength = (spec != null && spec.getFocalLengthMm() > 0) ? spec.getFocalLengthMm() : 50.0f;
        float fstop = (spec != null && spec.getApertureFStop() > 0) ? spec.getApertureFStop() : 1.8f;
        float sunIntensity = (spec != null && spec.getSunIntensity() > 0) ? spec.getSunIntensity() : 4.5f;
        float[] camPos = (spec != null && spec.getCameraPosition() != null && spec.getCameraPosition().length >= 3)
                ? spec.getCameraPosition() : new float[]{0.0f, -8.0f, 3.5f};

        sb.append("try:\n");
        sb.append("    if not bpy.context.scene.camera:\n");
        sb.append("        cam_data = bpy.data.cameras.new('CinematicCamera')\n");
        sb.append("        cam_data.lens = ").append(focalLength).append("\n");
        sb.append("        cam_data.dof.use_dof = True\n");
        sb.append("        cam_data.dof.aperture_fstop = ").append(fstop).append("\n");
        sb.append("        cam_obj = bpy.data.objects.new('Camera', cam_data)\n");
        sb.append("        bpy.context.collection.objects.link(cam_obj)\n");
        sb.append("        bpy.context.scene.camera = cam_obj\n");
        sb.append("        cam_obj.location = (").append(camPos[0]).append(", ").append(camPos[1]).append(", ").append(camPos[2]).append(")\n");
        sb.append("        cam_obj.rotation_euler = (math.radians(72), 0, 0)\n");
        sb.append("except Exception as ce: print(f'Camera setup note: {ce}')\n\n");

        sb.append("try:\n");
        sb.append("    sun_data = bpy.data.lights.new('Sun', type='SUN')\n");
        sb.append("    sun_data.energy = ").append(sunIntensity).append("\n");
        sb.append("    sun_obj = bpy.data.objects.new('SunLight', sun_data)\n");
        sb.append("    bpy.context.collection.objects.link(sun_obj)\n");
        sb.append("    sun_obj.rotation_euler = (math.radians(45), math.radians(15), math.radians(-30))\n");
        sb.append("except Exception as le: print(f'Lighting setup note: {le}')\n\n");

        if (spec != null && spec.isUseVolumetrics()) {
            sb.append("try:\n");
            sb.append("    world = bpy.context.scene.world\n");
            sb.append("    if world is None:\n");
            sb.append("        world = bpy.data.worlds.new('World')\n");
            sb.append("        bpy.context.scene.world = world\n");
            sb.append("    world.use_nodes = True\n");
            sb.append("    wnodes = world.node_tree.nodes\n");
            sb.append("    wlinks = world.node_tree.links\n");
            sb.append("    vol_node = wnodes.new('ShaderNodeVolumePrincipled')\n");
            sb.append("    vol_node.inputs['Density'].default_value = ").append(spec.getVolumetricDensity()).append("\n");
            sb.append("    w_output = wnodes.get('World Output')\n");
            sb.append("    if w_output:\n");
            sb.append("        wlinks.new(vol_node.outputs['Volume'], w_output.inputs['Volume'])\n");
            sb.append("except Exception as ve: print(f'Volumetric setup note: {ve}')\n\n");
        }

        sb.append("# --- STEP 1: EXPORT INTERACTIVE 3D GLTF/GLB MODEL ---\n");
        sb.append("try:\n");
        sb.append("    bpy.ops.export_scene.gltf(filepath='output/model.glb', export_format='GLB', export_skins=True, export_animations=True)\n");
        sb.append("    print('3D GLTF Export Successful: output/model.glb')\n");
        sb.append("except Exception as ge: print(f'GLTF export warning: {ge}')\n\n");

        sb.append("# --- STEP 2: HEADLESS-SAFE CPU CYCLES PREVIEW RENDER ---\n");
        sb.append("try:\n");
        sb.append("    if bpy.context.scene.camera:\n");
        sb.append("        bpy.context.scene.render.engine = 'CYCLES'\n");
        sb.append("        bpy.context.scene.cycles.device = 'CPU'\n");
        sb.append("        bpy.context.scene.cycles.samples = 16\n");
        sb.append("        bpy.context.scene.render.resolution_x = 1280\n");
        sb.append("        bpy.context.scene.render.resolution_y = 720\n");
        sb.append("        bpy.context.scene.render.filepath = 'output/render.png'\n");
        sb.append("        bpy.ops.render.render(write_still=True)\n");
        sb.append("        print('Cycles preview render complete: output/render.png')\n");
        sb.append("except Exception as re: print(f'Preview render note: {re}')\n");

        return sb.toString();
    }

    private String cleanPythonCode(String raw) {
        if (raw == null) return "";
        String code = raw.trim();
        if (code.startsWith("```python")) {
            code = code.substring(9);
        } else if (code.startsWith("```")) {
            code = code.substring(3);
        }
        if (code.endsWith("```")) {
            code = code.substring(0, code.length() - 3);
        }
        code = code.trim();

        // 1. Auto-sanitize unquoted f-strings: e.g., fPool_LED_{i} -> f"Pool_LED_{i}"
        code = code.replaceAll("(?<=[=\\s,(])f([a-zA-Z0-9_]+\\{[^}\"\\n]+\\}[a-zA-Z0-9_]*)", "f\"$1\"");

        // 2. Auto-sanitize hallucinated combined object.mesh operator calls
        code = code.replace("bpy.ops.object.mesh.", "bpy.ops.mesh.");

        // 3. Auto-sanitize hallucinated lighting and camera operators
        code = code.replace("bpy.ops.light.add(", "bpy.ops.object.light_add(");
        code = code.replace("bpy.ops.camera.add(", "bpy.ops.object.camera_add(");
        code = code.replace(".primitive_cube_create(", ".primitive_cube_add(");
        code = code.replace(".primitive_plane_create(", ".primitive_plane_add(");
        code = code.replace(".primitive_cylinder_create(", ".primitive_cylinder_add(");
        code = code.replace(".primitive_cone_create(", ".primitive_cone_add(");
        code = code.replace(".primitive_uv_sphere_create(", ".primitive_uv_sphere_add(");

        return code.trim();
    }

    private void executeStructuredGeminiPlanning(final AIProductionRequest request, 
                                                 final AIDirectorSpec directorSpec, 
                                                 final GeminiApiClient.ApiCallback<ProductionPlan> callback) {
        ToolRegistry registry = new ToolRegistry();
        StringBuilder toolManifestBuilder = new StringBuilder();
        toolManifestBuilder.append("AUTHORITATIVE REGISTERED COMMANDS:\n");
        for (ToolDefinition tool : registry.getRegisteredTools().values()) {
            if (tool.isAvailable()) {
                toolManifestBuilder.append("- Tool ID: \"").append(tool.getId()).append("\"\n");
                toolManifestBuilder.append("  Description: ").append(tool.getDescription()).append("\n");
                if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                    toolManifestBuilder.append("  Accepted Parameters: ");
                    for (ToolParameter param : tool.getParameters()) {
                        toolManifestBuilder.append(param.getName()).append(" (").append(param.getType()).append("), ");
                    }
                    toolManifestBuilder.setLength(toolManifestBuilder.length() - 2);
                    toolManifestBuilder.append("\n");
                }
            }
        }

        List<KnowledgeEntry> knowledgeEntries = knowledgeManager.retrieveAllKnowledgeForPrompt(request.getUserPrompt());
        StringBuilder contextBuilder = new StringBuilder();
        if (!knowledgeEntries.isEmpty()) {
            contextBuilder.append("KNOWLEDGE BLUEPRINTS:\n");
            for (KnowledgeEntry entry : knowledgeEntries) {
                contextBuilder.append("- Domain: ").append(entry.getName()).append("\n");
                contextBuilder.append("  Components: ").append(entry.getComponents()).append("\n");
            }
        }

        CloudProvider activeProvider = apiKeyManager.getComputeProvider();
        String providerContext = "ACTIVE COMPUTE PIPELINE: " + activeProvider.getDisplayName() + "\n";

        String systemInstruction = "You are Vynara Autonomous 3D Technical Director.\n" +
                "KNOWLEDGE vs. TOOL vs. TASK CONTRACT:\n" +
                "- Tools are the ONLY executable operations. Execute ONLY registered tools.\n" +
                "- Ensure character mesh creation ALWAYS precedes skeleton binding and rigging.\n\n" +
                "DIRECTOR SCENE SPECIFICATION:\n" + directorSpec.toJson().toString() + "\n\n" +
                providerContext + "\n" +
                toolManifestBuilder.toString() + "\n\n" +
                "RETURN A STRICT JSON OBJECT REPRESENTING THE PRODUCTION PLAN.\n" +
                "REQUIRED JSON SCHEMA:\n" +
                "{\n" +
                "  \"intent\": \"string\",\n" +
                "  \"sceneType\": \"string\",\n" +
                "  \"quality\": \"string\",\n" +
                "  \"objects\": [ { \"name\": \"string\", \"components\": [\"string\"], \"dimensions\": {\"width\": 0.0, \"height\": 0.0, \"depth\": 0.0} } ],\n" +
                "  \"materials\": [ { \"name\": \"string\", \"colorHex\": \"#FFFFFF\", \"metallic\": 0.0, \"roughness\": 0.5, \"opacity\": 1.0 } ],\n" +
                "  \"lighting\": \"string\",\n" +
                "  \"camera\": \"string\",\n" +
                "  \"characters\": [ { \"species\": \"string\", \"riggingRequired\": true, \"animationRequired\": true } ],\n" +
                "  \"requiredTools\": [ { \"toolId\": \"string\", \"description\": \"string\", \"parameters\": {} } ],\n" +
                "  \"validationRules\": [ \"string\" ]\n" +
                "}";

        String promptWithContext = "USER PROMPT: " + request.getUserPrompt() +
                "\nSTYLE: " + request.getStyle() +
                "\nTARGET ENGINE: " + request.getTargetEngine() +
                "\n\n" + contextBuilder.toString();

        VynaraLogger.system("Asynchronously dispatching structured 3D plan request to Google Gemini API...");

        apiClient.generateStructuredJson(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), systemInstruction, promptWithContext, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String jsonResult) {
                try {
                    String cleanJson = jsonResult.trim();
                    if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                    if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                    if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                    cleanJson = cleanJson.trim();

                    JSONObject root = new JSONObject(cleanJson);
                    AIProductionPlan structuredPlan = AIProductionPlan.fromJson(root);
                    ProductionPlan executablePlan = promptInterpreter.convertStructuredPlanToExecutablePlan(request, structuredPlan);
                    callback.onSuccess(executablePlan);
                } catch (Exception e) {
                    VynaraLogger.e("Plan compilation exception: " + e.getMessage(), e);
                    callback.onError("Failed to parse Gemini production plan: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("Gemini API connection error: " + errorMessage);
                callback.onError("Gemini API connection error: " + errorMessage);
            }
        });
    }

    /**
     * Direct Blender Python script generation using Director + Dynamic AI Script Writer pipeline.
     */
    public void planBlenderProduction(final String prompt, final GeminiApiClient.ApiCallback<String> callback) {
        if (!apiKeyManager.hasApiKey()) {
            callback.onError("Gemini API key missing. Please configure it in Settings.");
            return;
        }

        directorAgent.formulateDirectorSpec(prompt, "Photorealistic", new ArrayList<>(), new DirectorAgent.DirectorCallback() {
            @Override
            public void onSpecReady(final AIDirectorSpec spec) {
                if (isDemoPreset(prompt)) {
                    ProductionPlan demoPlan = promptInterpreter.createProductionPlan(prompt, "Photorealistic", "Blender Native");
                    for (TaskNode node : demoPlan.getTaskGraph().getAllNodes()) {
                        if (node.getOperation() != null && "blender.cloud_generate".equalsIgnoreCase(node.getOperation().getToolId())) {
                            String script = node.getOperation().getStringParam("bpyScript", "");
                            if (script != null && !script.isEmpty()) {
                                callback.onSuccess(script);
                                return;
                            }
                        }
                    }
                }

                // Dynamic Generation for custom prompts
                dispatchDynamicScriptWriter(prompt, "Photorealistic", spec, new GeminiApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String dynamicBpyCode) {
                        String wrappedScript = wrapDynamicScriptWithSafety(dynamicBpyCode, spec);
                        callback.onSuccess(wrappedScript);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Studio Assistant: Translates human requests into object updates OR dynamically spawns new objects.
     */
    public void processNaturalLanguageStudioEdit(String editPrompt, String activeSceneContextJson, final GeminiApiClient.ApiCallback<String> callback) {
        if (!apiKeyManager.hasApiKey()) {
            callback.onError("Gemini API Key missing. Please set it in Settings.");
            return;
        }

        String sysInst = "You are Vynara Studio Assistant. Interpret direct 3D requests on the active scene.\n" +
                "You can either EDIT an existing object OR CREATE a new object.\n" +
                "If the user asks to add/create something (e.g. 'add a green tree', 'add a chair', 'add light'):\n" +
                "Set \"action\": \"CREATE\", specify \"creationType\" (e.g. 'tree', 'chair', 'house', 'primitive_cube', 'primitive_sphere'), \"name\": \"string\", \"position\": { \"px\": 0.0, \"py\": 0.0, \"pz\": 0.0 }, and \"material\": { \"colorHex\": \"#FFFFFF\", \"metallic\": 0.0, \"roughness\": 0.5 }.\n" +
                "If the user asks to modify an existing object (e.g. 'make it wider', 'change color to blue', 'rotate 90 degrees'):\n" +
                "Set \"action\": \"EDIT\", \"targetObjectId\": \"string\", \"transform\": { \"px\": 0.0, \"py\": 0.0, \"pz\": 0.0, \"rx\": 0.0, \"ry\": 0.0, \"rz\": 0.0, \"sx\": 1.0, \"sy\": 1.0, \"sz\": 1.0 }, \"material\": { \"colorHex\": \"#FFFFFF\", \"metallic\": 0.0, \"roughness\": 0.5 }.\n" +
                "JSON FORMAT:\n" +
                "{\n" +
                "  \"action\": \"CREATE\" or \"EDIT\",\n" +
                "  \"targetObjectId\": \"string\",\n" +
                "  \"creationType\": \"string\",\n" +
                "  \"name\": \"string\",\n" +
                "  \"transform\": { \"px\": 0.0, \"py\": 0.0, \"pz\": 0.0, \"rx\": 0.0, \"ry\": 0.0, \"rz\": 0.0, \"sx\": 1.0, \"sy\": 1.0, \"sz\": 1.0 },\n" +
                "  \"material\": { \"colorHex\": \"#FFFFFF\", \"metallic\": 0.0, \"roughness\": 0.5, \"opacity\": 1.0 }\n" +
                "}";

        String fullPrompt = "SCENE CONTEXT:\n" + activeSceneContextJson + "\n\nUSER REQUEST: " + editPrompt;

        VynaraLogger.system("Asynchronously dispatching Studio Assistant request to Google Gemini API...");

        apiClient.generateStructuredJson(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), sysInst, fullPrompt, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                String cleanJson = result.trim();
                if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                callback.onSuccess(cleanJson.trim());
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("Studio Assistant connection error: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * AI Correction Loop: Consults Gemini to repair scene issues without guessing.
     */
    public void requestCorrectionPlan(String validationMessage, String validationCategory, String sceneContextJson, final GeminiApiClient.ApiCallback<String> callback) {
        if (!apiKeyManager.hasApiKey()) {
            callback.onError("API key missing. Cannot use AI for corrections.");
            return;
        }

        String sysInst = "You are Vynara AI Corrector. A validation error occurred in the 3D scene.\n" +
                "Review the Scene Context and Error Message. Determine the best repair strategy from the registered ToolRegistry.\n" +
                "Return a STRICT JSON object representing the repair tool operation:\n" +
                "{\n" +
                "  \"toolId\": \"string (e.g., geometry.create_primitive, material.set_properties, skeleton.bind)\",\n" +
                "  \"parameters\": { \"key\": \"value\" }\n" +
                "}";

        String prompt = "ERROR CATEGORY: " + validationCategory + "\n" +
                        "ERROR MESSAGE: " + validationMessage + "\n\n" +
                        "SCENE CONTEXT:\n" + sceneContextJson;

        VynaraLogger.system("Asynchronously dispatching AI Repair Request to Google Gemini API...");

        apiClient.generateStructuredJson(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), sysInst, prompt, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                String cleanJson = result.trim();
                if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                callback.onSuccess(cleanJson.trim());
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("AI Repair error: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    public GeminiApiClient getApiClient() { return apiClient; }
    public ApiKeyManager getApiKeyManager() { return apiKeyManager; }
    public KnowledgeManager getKnowledgeManager() { return knowledgeManager; }
    public PromptInterpreter getPromptInterpreter() { return promptInterpreter; }
    public DirectorAgent getDirectorAgent() { return directorAgent; }
}