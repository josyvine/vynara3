package com.example.ai;

import com.example.ai.AIContext;
import com.example.ai.AIOrchestrator;
import com.example.ai.GeminiApiClient;
import com.example.engine.Scene;
import com.example.tools.ToolExecutor;
import com.example.tools.ToolOperation;
import com.example.utils.VynaraLogger;
import com.example.validation.ValidationResult;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AICorrector {
    private final ToolExecutor toolExecutor;
    private final AIOrchestrator aiOrchestrator;
    private final Scene activeScene;

    public AICorrector(ToolExecutor toolExecutor, AIOrchestrator aiOrchestrator, Scene activeScene) {
        this.toolExecutor = toolExecutor;
        this.aiOrchestrator = aiOrchestrator;
        this.activeScene = activeScene;
    }

    /**
     * Evaluates validation inspection results and executes the full AI correction loop:
     * Generate -> Validate -> Inspect -> Problem Detection -> Repair Selection -> Correction -> Re-validate.
     */
    public boolean applyCorrections(List<ValidationResult> inspectionResults) {
        if (inspectionResults == null || inspectionResults.isEmpty()) {
            return true;
        }

        boolean allCorrectionsSuccessful = true;
        
        for (ValidationResult vr : inspectionResults) {
            if (vr.getSeverity() == ValidationResult.Severity.ERROR || 
                vr.getSeverity() == ValidationResult.Severity.CRITICAL) {
                
                boolean repairExecuted = executeIntelligenceDrivenRepair(vr);
                if (!repairExecuted) {
                    allCorrectionsSuccessful = false;
                }
            }
        }

        return allCorrectionsSuccessful;
    }

    /**
     * Multi-Turn AI Script Repair: Analyzes a failed Blender Python script and its exact
     * terminal traceback to synthesize a corrected script matching the user's prompt.
     */
    public void correctBlenderScript(String userPrompt, 
                                     String failedScript, 
                                     String errorMessage, 
                                     final GeminiApiClient.ApiCallback<String> callback) {
        if (aiOrchestrator == null || aiOrchestrator.getApiKeyManager() == null || !aiOrchestrator.getApiKeyManager().hasApiKey()) {
            if (callback != null) callback.onError("Cannot repair script: Gemini API key missing.");
            return;
        }

        String repairInstruction = "You are an expert Blender Python (`bpy`) engineer.\n" +
                "A generated Blender script failed during headless execution on the cloud worker.\n" +
                "Analyze the original user prompt, the failed script, and the exact Blender terminal error message.\n" +
                "Fix the syntax/API/operator/enum error and return ONLY the complete corrected Python script inside a single ```python block.\n" +
                "RULES:\n" +
                "1. Output ONLY executable Python code inside ```python. No commentary.\n" +
                "2. Ensure all mesh operators use `bpy.ops.mesh.primitive_...` (never `_create` or `bpy.ops.object.mesh.`).\n" +
                "3. Ensure all lighting operators use `bpy.ops.object.light_add` (never `bpy.ops.light.add`).\n" +
                "4. In `bpy.data.textures.new(name, type=...)`, type MUST be one of ('NONE', 'BLEND', 'CLOUDS', 'DISTORTED_NOISE', 'IMAGE', 'MAGIC', 'MARBLE', 'MUSGRAVE', 'NOISE', 'STUCCI', 'VORONOI', 'WOOD'). Never invent unlisted types like 'STORM'.\n" +
                "5. Preserve all original multi-part 3D geometry and materials from the user's prompt.";

        String repairPrompt = "USER PROMPT: " + userPrompt + "\n\n" +
                "EXACT BLENDER TERMINAL ERROR / TRACEBACK:\n" + errorMessage + "\n\n" +
                "FAILED SCRIPT:\n" + failedScript;

        VynaraLogger.system("AICorrector: Dispatching script repair request to Gemini API...");

        aiOrchestrator.getApiClient().generateContent(
                aiOrchestrator.getApiKeyManager().getApiKey(),
                aiOrchestrator.getApiKeyManager().getSelectedModel(),
                repairInstruction,
                repairPrompt,
                new GeminiApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        String cleaned = aiOrchestrator.getApiClient().cleanPythonOutput(result);
                        if (cleaned.isEmpty()) {
                            if (callback != null) callback.onError("Gemini returned empty repair script.");
                        } else {
                            VynaraLogger.system("AICorrector: Successfully repaired Python script (" + cleaned.length() + " chars).");
                            if (callback != null) callback.onSuccess(cleaned);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        VynaraLogger.e("AICorrector: Script repair failed: " + error);
                        if (callback != null) callback.onError(error);
                    }
                }
        );
    }

    /**
     * Consults Gemini AI for a repair plan, falling back to local deterministic repairs if offline.
     */
    private boolean executeIntelligenceDrivenRepair(ValidationResult vr) {
        if (vr == null || vr.getMessage() == null || toolExecutor == null) {
            return false;
        }

        if (aiOrchestrator == null || aiOrchestrator.getApiKeyManager() == null || !aiOrchestrator.getApiKeyManager().hasApiKey()) {
            return executeLocalDeterministicRepair(vr);
        }

        String sceneContextJson = AIContext.buildSceneContextJson(activeScene);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean repairSuccess = new AtomicBoolean(false);

        aiOrchestrator.requestCorrectionPlan(vr.getMessage(), vr.getCategory().name(), sceneContextJson, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String jsonResult) {
                try {
                    JSONObject opObj = new JSONObject(jsonResult);
                    String toolId = opObj.optString("toolId", null);
                    
                    if (toolId != null && !toolId.trim().isEmpty()) {
                        ToolOperation repairOp = new ToolOperation(toolId);
                        JSONObject paramsObj = opObj.optJSONObject("parameters");
                        if (paramsObj != null) {
                            java.util.Iterator<String> keys = paramsObj.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                Object val = paramsObj.opt(key);
                                if (val != null) {
                                    repairOp.setParam(key, val);
                                }
                            }
                        }
                        boolean executed = toolExecutor.executeOperation(repairOp);
                        repairSuccess.set(executed);
                    } else {
                        repairSuccess.set(executeLocalDeterministicRepair(vr));
                    }
                } catch (Exception e) {
                    repairSuccess.set(executeLocalDeterministicRepair(vr));
                }
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                repairSuccess.set(executeLocalDeterministicRepair(vr));
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return executeLocalDeterministicRepair(vr);
        }

        return repairSuccess.get();
    }

    private boolean executeLocalDeterministicRepair(ValidationResult vr) {
        String msg = vr.getMessage().toLowerCase();

        // 1. Missing or Degenerate Mesh Repair
        if (msg.contains("mesh") || msg.contains("vertex") || msg.contains("vertices")) {
            ToolOperation repairMeshOp = new ToolOperation("geometry.create_primitive")
                    .setParam("type", "cube")
                    .setParam("width", 1.5f)
                    .setParam("height", 1.5f)
                    .setParam("depth", 1.5f);
            return toolExecutor.executeOperation(repairMeshOp);
        }

        // 2. Missing Material Shading Repair
        if (msg.contains("material") || msg.contains("color") || msg.contains("shader")) {
            ToolOperation repairMatOp = new ToolOperation("material.set_properties")
                    .setParam("colorHex", "#A0A5BD")
                    .setParam("metallic", 0.1f)
                    .setParam("roughness", 0.5f);
            return toolExecutor.executeOperation(repairMatOp);
        }

        // 3. Unbound Skin or Weight Normalization Repair
        if (msg.contains("skin") || msg.contains("weight") || msg.contains("skeleton")) {
            ToolOperation bindOp = new ToolOperation("skeleton.bind");
            return toolExecutor.executeOperation(bindOp);
        }

        // 4. Default Fallback Re-validation Tool
        ToolOperation checkOp = new ToolOperation("validation.check_mesh");
        return toolExecutor.executeOperation(checkOp);
    }

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }
}