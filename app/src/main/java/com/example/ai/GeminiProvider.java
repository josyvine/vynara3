package com.example.ai;

import java.util.List;

public class GeminiProvider implements AIProvider {
    private final GeminiApiClient apiClient;

    public GeminiProvider(GeminiApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public void testConnection(String apiKey, GeminiApiClient.ApiCallback<Boolean> callback) {
        apiClient.testConnection(apiKey, "gemini-2.5-flash", callback);
    }

    @Override
    public void listModels(String apiKey, GeminiApiClient.ApiCallback<List<AIModel>> callback) {
        apiClient.fetchModels(apiKey, callback);
    }

    @Override
    public void generatePlan(String apiKey, String modelId, String userPrompt, String contextJson, GeminiApiClient.ApiCallback<String> callback) {
        String systemInstruction = "You are Vynara Autonomous 3D Artist. Convert natural language user requests into strict structured JSON 3D production plans. " +
                "The output MUST be valid JSON containing: intent, object specifications, components, PBR materials, lighting, camera setup, character specifications, skeleton/rigging needs, and task DAG steps. " +
                "Do NOT invent unregistered tools. Registered tools: geometry.create_primitive, geometry.create_procedural, material.set_properties, " +
                "character.create_humanoid, character.create_creature, skeleton.bind, rig.create_ik, animation.create_clip, scene.add_light, scene.set_camera, validation.check_mesh, export.gltf.";

        String promptWithContext = "USER REQUEST: " + userPrompt + "\nCONTEXT: " + (contextJson != null ? contextJson : "{}");

        // Phase 2 Alignment: Force structured JSON response MIME type
        apiClient.generateStructuredJson(apiKey, modelId, systemInstruction, promptWithContext, callback);
    }
}