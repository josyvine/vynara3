package com.example.ai;

import com.example.ai.protocol.AIProductionPlan;
import com.example.ai.protocol.AIProductionRequest;
import com.example.cloud.CloudProvider;
import com.example.knowledge.KnowledgeEntry;
import com.example.knowledge.KnowledgeManager;
import com.example.tasks.ProductionPlan;
import com.example.tools.ToolDefinition;
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

    public AIOrchestrator(GeminiApiClient apiClient, ApiKeyManager apiKeyManager, KnowledgeManager knowledgeManager) {
        this.apiClient = apiClient;
        this.apiKeyManager = apiKeyManager;
        this.knowledgeManager = knowledgeManager;
        this.promptInterpreter = new PromptInterpreter(knowledgeManager);
    }

    public ProductionPlan planProduction(String userPrompt, String style, String targetEngine) {
        return planProduction(userPrompt, style, targetEngine, new ArrayList<>());
    }

    public ProductionPlan planProduction(String userPrompt, String style, String targetEngine, List<String> referenceImageUris) {
        return promptInterpreter.createProductionPlan(userPrompt, style, targetEngine, referenceImageUris);
    }

    /**
     * CORE UPGRADE: Gemini is now the true production planner.
     * Integrates Knowledge Engine blueprints, strictly exposes valid ToolRegistry manifests,
     * and enforces structured JSON outputs for executable graph generation.
     */
    public void planProductionWithGemini(final AIProductionRequest request, final GeminiApiClient.ApiCallback<ProductionPlan> callback) {
        if (request == null || callback == null) return;

        if (!apiKeyManager.hasApiKey()) {
            VynaraLogger.system("API Key missing. Executing local offline prompt interpreter fallback.");
            ProductionPlan localPlan = promptInterpreter.createProductionPlan(
                    request.getUserPrompt(), request.getStyle(), request.getTargetEngine(), request.getReferenceImageUris());
            callback.onSuccess(localPlan);
            return;
        }

        // Dynamically compile the authoritative registered Tool Registry manifest
        ToolRegistry registry = new ToolRegistry();
        StringBuilder toolManifestBuilder = new StringBuilder();
        toolManifestBuilder.append("AUTHORITATIVE REGISTERED COMMANDS (You must ONLY select toolIds from this list):\n");
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

        // Inject deep construction knowledge from the Knowledge Engine
        List<KnowledgeEntry> knowledgeEntries = knowledgeManager.retrieveAllKnowledgeForPrompt(request.getUserPrompt());
        StringBuilder contextBuilder = new StringBuilder();
        if (!knowledgeEntries.isEmpty()) {
            contextBuilder.append("KNOWLEDGE ENGINE BLUEPRINTS (Use these concepts to structure the 3D generation plan):\n");
            for (KnowledgeEntry entry : knowledgeEntries) {
                contextBuilder.append("- Concept Domain: ").append(entry.getName()).append("\n");
                contextBuilder.append("  Components required: ").append(entry.getComponents()).append("\n");
                contextBuilder.append("  Required capabilities: ").append(entry.getRequiredCapabilities()).append("\n");
                contextBuilder.append("  Default materials: ").append(entry.getDefaultMaterials()).append("\n");
            }
        }

        CloudProvider activeProvider = apiKeyManager.getComputeProvider();
        String providerContext = "ACTIVE COMPUTE PIPELINE: " + activeProvider.getDisplayName() + "\n";

        String systemInstruction = "You are Vynara Autonomous 3D Master Technical Director & Architectural Planner.\n" +
                "KNOWLEDGE vs. CAPABILITY vs. TOOL vs. TASK CONTRACT:\n" +
                "- Knowledge describes construction rules and facts. Capabilities describe what the system knows how to do.\n" +
                "- Tools are the ONLY executable operations. Tasks are concrete operations in the production plan.\n" +
                "- You may reason using knowledge and capabilities, but you may execute ONLY registered tools.\n" +
                "- When Target Engine is BLENDER_NATIVE or Cloud Workers (GitHub Actions) are used, select `blender.cloud_generate` as the primary production tool.\n" +
                "- DO NOT select `rig.auto_rig_cloud` unless Hugging Face is explicitly chosen in the provider settings.\n" +
                "- CRITICAL PROCEDURAL FIDELITY & BLENDER 4.x SCRIPT RULES FOR `bpyScript` in `blender.cloud_generate`:\n" +
                "  1. Output clean multiline Python 3 code using standard newlines (\\n). NEVER concatenate lines with semicolons (;).\n" +
                "  2. Clear startup scene cleanly: `bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)`.\n" +
                "  3. Enable Blender built-in addons: `import addon_utils; addon_utils.enable('archimesh'); addon_utils.enable('rigify')`.\n" +
                "  4. NEVER output a single primitive cube or plain placeholder box. Construct multi-part detailed geometry with Bevel, Subdivision Surface, or Solidify modifiers.\n" +
                "  5. For architectural prompts: build foundation slabs, detailed walls with openings, window frames with glass transmission, door panels, and ambient sun + warm interior point lights.\n" +
                "  6. For furniture/vehicles: construct multi-part structures with distinct cushions, bevels, seams, and PBR material slots (leather, metallic, wood, chrome).\n" +
                "  7. For characters/creatures: construct anatomical body segments, create an Armature hierarchy, and parent with `ARMATURE_AUTO`.\n" +
                "  8. ALWAYS import `os`, create directory `os.makedirs('output', exist_ok=True)`, and export the final 3D scene directly using:\n" +
                "     `bpy.ops.export_scene.gltf(filepath='output/model.glb', export_format='GLB')`\n" +
                "- Choose your commands strictly from the provided Authoritative Registered Commands manifest. Never invent Tool IDs.\n\n" +
                providerContext + "\n" +
                toolManifestBuilder.toString() + "\n\n" +
                "RETURN A STRICT JSON OBJECT REPRESENTING THE PRODUCTION PLAN.\n" +
                "REQUIRED JSON SCHEMA:\n" +
                "{\n" +
                "  \"intent\": \"string (e.g., CREATE_SCENE, CREATE_CHARACTER)\",\n" +
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
                "\nATTACHED REFERENCE IMAGES COUNT: " + request.getReferenceImageUris().size() +
                "\n\n" + contextBuilder.toString();

        VynaraLogger.system("Asynchronously dispatching 3D creation request to Google Gemini API...");
        VynaraLogger.gemini("Dispatched Prompt: " + request.getUserPrompt());
        VynaraLogger.ai("Active reasoning model: " + apiKeyManager.getSelectedModel());

        apiClient.generateStructuredJson(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), systemInstruction, promptWithContext, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String jsonResult) {
                VynaraLogger.gemini("Raw API Response Payload Received: " + jsonResult);
                try {
                    JSONObject root = new JSONObject(jsonResult);
                    AIProductionPlan structuredPlan = AIProductionPlan.fromJson(root);
                    ProductionPlan executablePlan = promptInterpreter.convertStructuredPlanToExecutablePlan(request, structuredPlan);
                    callback.onSuccess(executablePlan);
                } catch (Exception e) {
                    VynaraLogger.e("Plan compilation exception thrown inside parsing phase", (Throwable) e);
                    callback.onError("Failed to parse Gemini production plan: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("Gemini API connection error callback fired: " + errorMessage);
                callback.onError("Gemini API connection error: " + errorMessage);
            }
        });
    }

    /**
     * Direct Blender Python script generation for cloud runners.
     */
    public void planBlenderProduction(String prompt, final GeminiApiClient.ApiCallback<String> callback) {
        if (!apiKeyManager.hasApiKey()) {
            callback.onError("Gemini API key missing. Please configure it in Settings.");
            return;
        }

        String blenderSystemInstruction = "You are Vynara Master 3D Technical Director & Blender Python (`bpy`) Expert.\n" +
                "REQUIREMENTS FOR BLENDER SCRIPT GENERATION:\n" +
                "1. Clear default scene completely: `bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)`.\n" +
                "2. Enable built-in addons: `import addon_utils; addon_utils.enable('archimesh'); addon_utils.enable('rigify')`.\n" +
                "3. NEVER output a single primitive cube or plain box placeholder.\n" +
                "4. Construct detailed, multi-component, photorealistic 3D models using `bpy.ops.mesh` and `bpy.data` objects.\n" +
                "5. For architectural prompts: build foundation slabs, exterior walls, window panes with glass transmission, roof structures, pool cutouts with blue water material, wooden decks, and lighting.\n" +
                "6. For furniture/vehicles: construct multi-element models with Bevel modifiers, smooth shading, distinct material slots (metallic, leather, wood, fabric).\n" +
                "7. For character/creature prompts: construct anatomical geometry, generate bone armature, parent with automatic weights (`bpy.ops.object.parent_set(type='ARMATURE_AUTO')`), and assign PBR materials.\n" +
                "8. CRITICAL MANDATORY STEP: Import `os`, create output directory `os.makedirs('output', exist_ok=True)`, and export the final 3D scene directly to GLB format using:\n" +
                "   `bpy.ops.export_scene.gltf(filepath='output/model.glb', export_format='GLB')`\n" +
                "9. CRITICAL PYTHON FORMATTING RULE: Output clean multiline Python code with standard newlines (\\n). NEVER concatenate Python statements on a single line using semicolons (;). Return ONLY valid executable Python code without Markdown formatting backticks.";

        VynaraLogger.system("Generating Blender Python execution script via Gemini...");
        apiClient.generateBlenderScript(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), prompt + "\n" + blenderSystemInstruction, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String bpyScript) {
                VynaraLogger.gemini("Blender script generated successfully (" + bpyScript.length() + " chars)");
                callback.onSuccess(bpyScript);
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("Failed to generate Blender script: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * CORE UPGRADE: Processes natural language 3D scene editing.
     * Translates human intent into specific tool parameters applied to existing scene nodes OR creates new objects.
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
                "Set \"action\": \"EDIT\", \"targetObjectId\": \"string (match from context)\", \"transform\": { \"px\": 0.0, \"py\": 0.0, \"pz\": 0.0, \"rx\": 0.0, \"ry\": 0.0, \"rz\": 0.0, \"sx\": 1.0, \"sy\": 1.0, \"sz\": 1.0 }, \"material\": { \"colorHex\": \"#FFFFFF\", \"metallic\": 0.0, \"roughness\": 0.5 }.\n" +
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
        VynaraLogger.gemini("Dispatched Studio Edit Prompt: " + editPrompt);

        apiClient.generateStructuredJson(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), sysInst, fullPrompt, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                VynaraLogger.gemini("Raw Studio Edit Response Payload Received: " + result);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("Studio Assistant connection error callback fired: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * CORE UPGRADE: AI Correction Loop implementation.
     * Consults Gemini to dynamically determine the exact tool operation needed to fix a validation error.
     */
    public void requestCorrectionPlan(String validationMessage, String validationCategory, String sceneContextJson, final GeminiApiClient.ApiCallback<String> callback) {
        if (!apiKeyManager.hasApiKey()) {
            callback.onError("API key missing. Cannot use AI for corrections.");
            return;
        }

        String sysInst = "You are Vynara AI Corrector. A validation error occurred in the 3D scene during the inspection phase. " +
                "Review the provided Scene Context and the Error Message. Determine the best repair strategy from the registered ToolRegistry.\n" +
                "Return a STRICT JSON object representing the tool operation needed to repair the scene.\n" +
                "JSON FORMAT:\n" +
                "{\n" +
                "  \"toolId\": \"string (e.g., geometry.create_primitive, material.set_properties, skeleton.bind)\",\n" +
                "  \"parameters\": { \"key\": \"value\" }\n" +
                "}";

        String prompt = "ERROR CATEGORY: " + validationCategory + "\n" +
                        "ERROR MESSAGE: " + validationMessage + "\n\n" +
                        "SCENE CONTEXT:\n" + sceneContextJson;

        VynaraLogger.system("Asynchronously dispatching AI Repair Request to Google Gemini API...");
        VynaraLogger.gemini("Dispatched AI Repair Diagnostic: " + validationMessage);

        apiClient.generateStructuredJson(apiKeyManager.getApiKey(), apiKeyManager.getSelectedModel(), sysInst, prompt, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                VynaraLogger.gemini("Raw AI Repair Response Payload Received: " + result);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMessage) {
                VynaraLogger.e("AI Repair connection error callback fired: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    public GeminiApiClient getApiClient() { return apiClient; }
    public ApiKeyManager getApiKeyManager() { return apiKeyManager; }
    public KnowledgeManager getKnowledgeManager() { return knowledgeManager; }
    public PromptInterpreter getPromptInterpreter() { return promptInterpreter; }
}