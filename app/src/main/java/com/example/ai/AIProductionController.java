package com.example.ai;

import android.content.Context;

import com.example.ai.protocol.AIProductionRequest;
import com.example.character.CharacterManager;
import com.example.cloud.CloudProvider;
import com.example.engine.ThreeDEngine;
import com.example.knowledge.KnowledgeManager;
import com.example.runtime.ProjectRuntime;
import com.example.tasks.ExecutionEngine;
import com.example.tasks.ProductionPlan;
import com.example.tools.ToolExecutor;
import com.example.tools.ToolRegistry;
import com.example.validation.ValidationManager;

import java.util.ArrayList;
import java.util.List;

public class AIProductionController {
    private final Context context;
    private final ProjectRuntime runtime;
    private final ApiKeyManager apiKeyManager;
    private final GeminiApiClient apiClient;
    private final GeminiProvider geminiProvider;
    private final KnowledgeManager knowledgeManager;
    private final ToolRegistry toolRegistry;
    private final ThreeDEngine threeDEngine;
    private final CharacterManager characterManager;
    private final ValidationManager validationManager;
    private final ToolExecutor toolExecutor;
    private final ExecutionEngine executionEngine;
    private final AIOrchestrator orchestrator;

    private ProductionPlan currentPlan;

    public AIProductionController(Context context) {
        this.context = context.getApplicationContext();
        // Connect to the unified ProjectRuntime instance to eliminate split engine instances
        this.runtime = ProjectRuntime.getInstance(this.context);
        this.apiKeyManager = new ApiKeyManager(this.context);
        this.apiClient = new GeminiApiClient();
        this.geminiProvider = new GeminiProvider(apiClient);
        
        // Bind subsystems directly from the shared ProjectRuntime
        this.knowledgeManager = runtime.getKnowledgeManager();
        this.toolRegistry = runtime.getToolRegistry();
        this.threeDEngine = runtime.getEngine();
        this.characterManager = runtime.getCharacterManager();
        this.validationManager = runtime.getValidationManager();
        this.toolExecutor = runtime.getToolExecutor();
        this.executionEngine = runtime.getExecutionEngine();
        this.orchestrator = new AIOrchestrator(apiClient, apiKeyManager, knowledgeManager);
    }

    public ProductionPlan generatePlan(String userPrompt, String style, String engine) {
        return generatePlan(userPrompt, style, engine, new ArrayList<>());
    }

    public ProductionPlan generatePlan(String userPrompt, String style, String engine, List<String> referenceImageUris) {
        if (engine != null && (engine.toLowerCase().contains("blender") || engine.toLowerCase().contains("cloud"))) {
            apiKeyManager.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);
        }
        currentPlan = orchestrator.planProduction(userPrompt, style, engine, referenceImageUris);
        return currentPlan;
    }

    /**
     * CORE PIPELINE UPDATE: Asynchronously requests an intelligent, structured 3D production plan
     * directly from the selected Gemini model, utilizing active knowledge bases and reference images.
     */
    public void generatePlanWithGemini(String userPrompt, String style, String engine, List<String> referenceImageUris, final GeminiApiClient.ApiCallback<ProductionPlan> callback) {
        if (callback == null) return;

        // Force GitHub Actions compute provider if Blender Native target engine is selected
        if (engine != null && (engine.toLowerCase().contains("blender") || engine.toLowerCase().contains("cloud"))) {
            apiKeyManager.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);
        }

        AIProductionRequest request = new AIProductionRequest(userPrompt, style, engine);
        if (referenceImageUris != null) {
            for (String uri : referenceImageUris) {
                request.addReferenceImageUri(uri);
            }
        }

        // Query the live, registered Gemini model
        orchestrator.planProductionWithGemini(request, new GeminiApiClient.ApiCallback<ProductionPlan>() {
            @Override
            public void onSuccess(ProductionPlan plan) {
                currentPlan = plan;
                callback.onSuccess(plan);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void executeCurrentPlan(ExecutionEngine.ExecutionCallback callback) {
        if (currentPlan != null && currentPlan.getTaskGraph() != null) {
            // Begin scene transaction for undo/redo rollback capability
            runtime.getTransactionManager().beginTransaction("Execute AI Plan: " + currentPlan.getProjectName());
            
            executionEngine.executeGraph(currentPlan.getTaskGraph(), new ExecutionEngine.ExecutionCallback() {
                @Override
                public void onTaskUpdated(com.example.tasks.TaskNode node, com.example.tasks.TaskGraph graph) {
                    if (callback != null) callback.onTaskUpdated(node, graph);
                }

                @Override
                public void onGraphCompleted(com.example.tasks.TaskGraph graph) {
                    // Commit transaction upon successful completion
                    runtime.getTransactionManager().commitTransaction();
                    if (callback != null) callback.onGraphCompleted(graph);
                }

                @Override
                public void onError(String errorMessage) {
                    // Rollback scene graph transaction on execution failure
                    runtime.getTransactionManager().rollbackTransaction();
                    if (callback != null) callback.onError(errorMessage);
                }
            });
        } else {
            if (callback != null) callback.onError("No active production plan to execute.");
        }
    }

    public Context getContext() { return context; }
    public ProjectRuntime getRuntime() { return runtime; }
    public ApiKeyManager getApiKeyManager() { return apiKeyManager; }
    public GeminiApiClient getApiClient() { return apiClient; }
    public GeminiProvider getGeminiProvider() { return geminiProvider; }
    public KnowledgeManager getKnowledgeManager() { return knowledgeManager; }
    public ToolRegistry getToolRegistry() { return toolRegistry; }
    public ThreeDEngine getThreeDEngine() { return threeDEngine; }
    public CharacterManager getCharacterManager() { return characterManager; }
    public ValidationManager getValidationManager() { return validationManager; }
    public ExecutionEngine getExecutionEngine() { return executionEngine; }
    public AIOrchestrator getOrchestrator() { return orchestrator; }
    public ProductionPlan getCurrentPlan() { return currentPlan; }
}