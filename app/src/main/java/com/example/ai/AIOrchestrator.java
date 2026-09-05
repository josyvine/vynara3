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
     * Executes the Director-Worker Production Pipeline:
     * Phase 1: DirectorAgent establishes camera DOF (f/1.8), sun angle, volumetric mist, and seeds.
     * Phase 2: BlenderWorkerAgent generates modular sub-scripts (hero, environment, lighting).
     * Phase 3: Compiles executable DAG tasks.
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
                            // Phase 2: Generate Modular Worker Scripts for Blender Native
                            String assetId = "asset_" + System.currentTimeMillis();
                            BlenderWorkerAgent.WorkerScripts workerScripts = 
                                    BlenderWorkerAgent.generateModularScripts(request.getUserPrompt(), directorSpec, assetId);

                            ProductionPlan plan = promptInterpreter.createProductionPlan(
                                    request.getUserPrompt(), request.getStyle(), request.getTargetEngine(), request.getReferenceImageUris());

                            // Attach modular scripts and seeds directly to the blender.cloud_generate task node
                            for (TaskNode node : plan.getTaskGraph().getAllNodes()) {
                                if (node.getOperation() != null && 
                                        "blender.cloud_generate".equalsIgnoreCase(node.getOperation().getToolId())) {
                                    node.getOperation().setParam("assetId", assetId);
                                    node.getOperation().setParam("bpyScript", workerScripts.compositeMasterScript);
                                    node.getOperation().setParam("w1HeroScript", workerScripts.heroScript);
                                    node.getOperation().setParam("w2EnvScript", workerScripts.environmentScript);
                                    node.getOperation().setParam("w3LightScript", workerScripts.lightingAndRenderScript);
                                    node.getOperation().setParam("seedHero", directorSpec.getSeedHero());
                                    node.getOperation().setParam("seedVegetation", directorSpec.getSeedVegetation());
                                    node.getOperation().setParam("seedLighting", directorSpec.getSeedLighting());
                                    node.getOperation().setParam("directorSpec", directorSpec.toJson().toString());
                                }
                            }

                            callback.onSuccess(plan);
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
     * Direct Blender Python script generation using Director + Worker pipeline.
     */
    public void planBlenderProduction(final String prompt, final GeminiApiClient.ApiCallback<String> callback) {
        if (!apiKeyManager.hasApiKey()) {
            callback.onError("Gemini API key missing. Please configure it in Settings.");
            return;
        }

        directorAgent.formulateDirectorSpec(prompt, "Photorealistic", new ArrayList<>(), new DirectorAgent.DirectorCallback() {
            @Override
            public void onSpecReady(AIDirectorSpec spec) {
                BlenderWorkerAgent.WorkerScripts scripts = 
                        BlenderWorkerAgent.generateModularScripts(prompt, spec, "asset_" + System.currentTimeMillis());
                callback.onSuccess(scripts.compositeMasterScript);
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