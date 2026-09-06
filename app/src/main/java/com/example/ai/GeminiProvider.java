package com.example.ai;

import java.util.List;

public interface AIProvider {
    /**
     * Verifies API credentials against the provider backend.
     */
    void testConnection(String apiKey, GeminiApiClient.ApiCallback<Boolean> callback);

    /**
     * Lists all models supported by the provider.
     */
    void listModels(String apiKey, GeminiApiClient.ApiCallback<List<AIModel>> callback);

    /**
     * Standard single-turn text production planning.
     */
    void generatePlan(String apiKey, String modelId, String userPrompt, String contextJson, GeminiApiClient.ApiCallback<String> callback);

    /**
     * Multimodal production planning supporting attached visual reference images.
     */
    void generatePlan(String apiKey, String modelId, String userPrompt, String contextJson, List<String> base64Images, GeminiApiClient.ApiCallback<String> callback);

    /**
     * Phase 2 Dynamic AI Script Writer: Live generation of 100% custom Blender Python (bpy) scripts.
     */
    void generateBlenderScript(String apiKey, String modelId, String systemInstruction, String userPrompt, GeminiApiClient.ApiCallback<String> callback);

    /**
     * Strict JSON schema generation for structured Director specs and Studio Assistant operations.
     */
    void generateStructuredJson(String apiKey, String modelId, String systemInstruction, String userPrompt, GeminiApiClient.ApiCallback<String> callback);
}