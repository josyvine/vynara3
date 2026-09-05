package com.example.ai;

import java.util.List;

public interface AIProvider {
    void testConnection(String apiKey, GeminiApiClient.ApiCallback<Boolean> callback);
    void listModels(String apiKey, GeminiApiClient.ApiCallback<List<AIModel>> callback);
    void generatePlan(String apiKey, String modelId, String userPrompt, String contextJson, GeminiApiClient.ApiCallback<String> callback);
}
