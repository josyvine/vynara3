package com.example.ai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiClient {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String BLENDER_SYSTEM_INSTRUCTION =
            "You are an expert 3D modeling and rigging engineer using Blender's Python API (`bpy`).\n" +
            "When given a creation prompt, generate ONLY executable, production-grade Python code for Blender.\n" +
            "Requirements:\n" +
            "1. Start with `import bpy, math, sys, os`.\n" +
            "2. Always clear existing objects: `bpy.ops.object.select_all(action='SELECT')` and `bpy.ops.object.delete()`.\n" +
            "3. Generate requested geometry, modifiers (subdivision, bevel, boolean, mirror), materials (Principled BSDF), and armatures.\n" +
            "4. Read the target export path from command-line arguments: `output_path = sys.argv[-1] if len(sys.argv) > 1 and sys.argv[-1].endswith('.glb') else 'output.glb'`.\n" +
            "5. Ensure output directory exists and export to standard GLB: `bpy.ops.export_scene.gltf(filepath=output_path, export_format='GLB', export_skins=True, export_animations=True)`.\n" +
            "6. Output ONLY raw Python code without extra conversational commentary.";

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public GeminiApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchModels(String apiKey, final ApiCallback<List<AIModel>> callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Gemini API key is required.");
            return;
        }

        String url = BASE_URL + "models?key=" + apiKey.trim();
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getLocalizedMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    final String err = "HTTP " + response.code() + " from Gemini API";
                    mainHandler.post(() -> callback.onError(err));
                    response.close();
                    return;
                }

                try {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(bodyStr);
                    JSONArray modelsArray = json.optJSONArray("models");
                    final List<AIModel> modelList = new ArrayList<>();

                    if (modelsArray != null) {
                        for (int i = 0; i < modelsArray.length(); i++) {
                            JSONObject m = modelsArray.getJSONObject(i);
                            String rawName = m.optString("name", "");
                            String cleanName = rawName.startsWith("models/") ? rawName.substring(7) : rawName;
                            String displayName = m.optString("displayName", cleanName);
                            String description = m.optString("description", "");

                            if (!cleanName.isEmpty()) {
                                modelList.add(new AIModel(cleanName, displayName, description, true));
                            }
                        }
                    }

                    if (modelList.isEmpty()) {
                        modelList.add(new AIModel("gemini-1.5-flash", "gemini-1.5-flash", "Standard fast production model", true));
                        modelList.add(new AIModel("gemini-1.5-pro", "gemini-1.5-pro", "Advanced reasoning model", true));
                    }

                    mainHandler.post(() -> callback.onSuccess(modelList));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse models response: " + e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    public void testConnection(String apiKey, String modelId, final ApiCallback<Boolean> callback) {
        generateContent(apiKey, modelId, "You are a 3D creation assistant.", "Ping test. Respond with OK.", new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(true);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void generateContent(String apiKey, String modelId, String systemInstruction, String userPrompt, final ApiCallback<String> callback) {
        generateContentInternal(apiKey, modelId, systemInstruction, userPrompt, null, false, callback);
    }

    public void generateContent(String apiKey, String modelId, String systemInstruction, String userPrompt, List<String> base64Images, final ApiCallback<String> callback) {
        generateContentInternal(apiKey, modelId, systemInstruction, userPrompt, base64Images, false, callback);
    }

    public void generateStructuredJson(String apiKey, String modelId, String systemInstruction, String userPrompt, final ApiCallback<String> callback) {
        generateContentInternal(apiKey, modelId, systemInstruction, userPrompt, null, true, callback);
    }

    public void generateStructuredJson(String apiKey, String modelId, String systemInstruction, String userPrompt, List<String> base64Images, final ApiCallback<String> callback) {
        generateContentInternal(apiKey, modelId, systemInstruction, userPrompt, base64Images, true, callback);
    }

    public void generateBlenderScript(String apiKey, String modelId, String userPrompt, final ApiCallback<String> callback) {
        generateContentInternal(apiKey, modelId, BLENDER_SYSTEM_INSTRUCTION, userPrompt, null, false, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                String cleanedScript = cleanPythonOutput(result);
                callback.onSuccess(cleanedScript);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private void generateContentInternal(String apiKey, String modelId, String systemInstruction, String userPrompt, List<String> base64Images, boolean enforceJson, final ApiCallback<String> callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Gemini API key is missing. Please configure it in Settings.");
            return;
        }

        if (modelId == null || modelId.trim().isEmpty()) {
            callback.onError("No active Gemini model selected. Please select a model in Settings.");
            return;
        }

        String targetModel = modelId.trim();
        String url = BASE_URL + "models/" + targetModel + ":generateContent?key=" + apiKey.trim();

        try {
            JSONObject root = new JSONObject();

            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                JSONObject systemInstObj = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPartObj = new JSONObject();
                sysPartObj.put("text", systemInstruction);
                sysParts.put(sysPartObj);
                systemInstObj.put("parts", sysParts);
                root.put("systemInstruction", systemInstObj);
            }

            JSONArray contents = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            JSONArray parts = new JSONArray();

            // Inject Base64 Image Parts for Gemini Vision
            if (base64Images != null && !base64Images.isEmpty()) {
                for (String b64 : base64Images) {
                    if (b64 != null && !b64.trim().isEmpty()) {
                        JSONObject inlineData = new JSONObject();
                        inlineData.put("mime_type", "image/jpeg");
                        inlineData.put("data", b64.trim());
                        JSONObject imgPart = new JSONObject();
                        imgPart.put("inline_data", inlineData);
                        parts.put(imgPart);
                    }
                }
            }

            // Inject User Prompt Text Part
            JSONObject partText = new JSONObject();
            partText.put("text", userPrompt);
            parts.put(partText);

            userMsg.put("parts", parts);
            contents.put(userMsg);
            root.put("contents", contents);

            if (enforceJson) {
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("responseMimeType", "application/json");
                root.put("generationConfig", generationConfig);
            }

            RequestBody body = RequestBody.create(root.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getLocalizedMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String detailedError = "HTTP Error " + response.code();
                        try {
                            String errBody = response.body() != null ? response.body().string() : "";
                            JSONObject errJson = new JSONObject(errBody);
                            JSONObject errorObj = errJson.optJSONObject("error");
                            if (errorObj != null) {
                                detailedError = errorObj.optString("message", detailedError);
                            }
                        } catch (Exception ignored) {}
                        
                        final String finalErr = detailedError;
                        mainHandler.post(() -> callback.onError(finalErr));
                        response.close();
                        return;
                    }

                    try {
                        String responseStr = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(responseStr);
                        JSONArray candidates = json.optJSONArray("candidates");

                        if (candidates != null && candidates.length() > 0) {
                            JSONObject firstCand = candidates.getJSONObject(0);
                            JSONObject content = firstCand.optJSONObject("content");

                            if (content != null) {
                                JSONArray resParts = content.optJSONArray("parts");

                                if (resParts != null && resParts.length() > 0) {
                                    String textResult = resParts.getJSONObject(0).optString("text", "");
                                    textResult = cleanJsonOutput(textResult);
                                    final String finalResult = textResult;
                                    mainHandler.post(() -> callback.onSuccess(finalResult));
                                    return;
                                }
                            }
                        }

                        mainHandler.post(() -> callback.onError("No content returned in Gemini response."));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Error parsing Gemini response: " + e.getMessage()));
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Error constructing Gemini request: " + e.getMessage());
        }
    }

    private String cleanJsonOutput(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String cleanPythonOutput(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.startsWith("```python")) {
            trimmed = trimmed.substring(9);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}