package com.example.ai.agents;

import com.example.ai.ApiKeyManager;
import com.example.ai.GeminiApiClient;
import com.example.ai.protocol.AIDirectorSpec;
import com.example.utils.VynaraLogger;

import org.json.JSONObject;

import java.util.List;

public class DirectorAgent {
    private final GeminiApiClient apiClient;
    private final ApiKeyManager apiKeyManager;

    public interface DirectorCallback {
        void onSpecReady(AIDirectorSpec spec);
        void onError(String errorMessage);
    }

    public DirectorAgent(GeminiApiClient apiClient, ApiKeyManager apiKeyManager) {
        this.apiClient = apiClient;
        this.apiKeyManager = apiKeyManager;
    }

    /**
     * Phase 1: Formulates the scene specification without touching 3D code.
     * Analyzes composition, lighting, camera, palette, and seeds.
     */
    public void formulateDirectorSpec(final String userPrompt,
                                      final String style,
                                      final List<String> referenceImageUris,
                                      final DirectorCallback callback) {
        if (callback == null) return;

        // Strict Check: No silent fallback if API key is missing
        if (!apiKeyManager.hasApiKey()) {
            String msg = "DirectorAgent: Gemini API Key missing in Settings. Cannot run live AI generation.";
            VynaraLogger.e(msg);
            callback.onError(msg);
            return;
        }

        final String activeModel = apiKeyManager.getSelectedModel();

        String systemInstruction = "You are the 3D Master Art Director & Spatial Architect.\n" +
                "YOUR ROLE:\n" +
                "- You NEVER write Python code or Blender operators.\n" +
                "- Your sole job is to design the visual contract, spatial constraints, and camera framing for the scene.\n" +
                "- Analyze the user's prompt to produce a photorealistic DIRECTOR SPECIFICATION.\n" +
                "CINEMATIC RULES:\n" +
                "1. Camera: Pick an intentional focal length (35mm for wide architecture/landscapes, 50mm for natural perspective, 85mm for furniture/characters).\n" +
                "2. Depth of Field (DOF): Use a shallow aperture (f/1.4 to f/2.8, default f/1.8) focused on the hero subject to create background blur.\n" +
                "3. Volumetrics & Lighting: For atmospheric scenes, set useVolumetrics=true with density between 0.01 and 0.025 to create sunbeams. Set low sun angles (elevation 15-25 degrees) for dramatic long shadows.\n" +
                "4. Color Palette: Output three harmonic hex colors (primary structure, secondary detail/earth, accent highlight).\n" +
                "5. Modular Seeds: Assign separate integer random seeds (e.g., 101, 202, 303, 404) for terrain, hero subject, vegetation, and lighting.\n\n" +
                "RETURN A RAW STRICT JSON OBJECT ONLY. DO NOT WRAP IN MARKDOWN (NO ```json):\n" +
                "{\n" +
                "  \"sceneType\": \"string\",\n" +
                "  \"mood\": \"string\",\n" +
                "  \"visualStyleNotes\": \"string\",\n" +
                "  \"camera\": {\n" +
                "    \"focalLengthMm\": 50.0,\n" +
                "    \"apertureFStop\": 1.8,\n" +
                "    \"focusDistance\": 4.5,\n" +
                "    \"position\": [0.0, -7.0, 2.5],\n" +
                "    \"target\": [0.0, 0.0, 1.2]\n" +
                "  },\n" +
                "  \"lighting\": {\n" +
                "    \"useVolumetrics\": true,\n" +
                "    \"volumetricDensity\": 0.015,\n" +
                "    \"sunElevation\": 18.0,\n" +
                "    \"sunAzimuth\": 45.0,\n" +
                "    \"sunIntensity\": 4.5,\n" +
                "    \"ambientColorHex\": \"#202835\"\n" +
                "  },\n" +
                "  \"palette\": {\n" +
                "    \"primaryColorHex\": \"#3D4A32\",\n" +
                "    \"secondaryColorHex\": \"#5A4432\",\n" +
                "    \"accentColorHex\": \"#D4A359\"\n" +
                "  },\n" +
                "  \"seeds\": {\n" +
                "    \"seedTerrain\": 101,\n" +
                "    \"seedHero\": 202,\n" +
                "    \"seedVegetation\": 303,\n" +
                "    \"seedLighting\": 404\n" +
                "  }\n" +
                "}";

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("USER PROMPT: ").append(userPrompt).append("\n");
        promptBuilder.append("REQUESTED STYLE: ").append(style).append("\n");
        if (referenceImageUris != null && !referenceImageUris.isEmpty()) {
            promptBuilder.append("REFERENCE IMAGES: ").append(referenceImageUris.size()).append(" visual reference(s) attached.\n");
            promptBuilder.append("Note: Emulate professional depth of field, natural lighting contrast, and organic material textures from the visual references.\n");
        }

        VynaraLogger.system("DirectorAgent: Formulating live scene specification via Gemini [" + activeModel + "]...");

        apiClient.generateStructuredJson(
                apiKeyManager.getApiKey(),
                activeModel,
                systemInstruction,
                promptBuilder.toString(),
                new GeminiApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String jsonResult) {
                        try {
                            // Sanitize Markdown code block formatting if Gemini wraps output
                            String cleanJson = jsonResult.trim();
                            if (cleanJson.startsWith("```json")) {
                                cleanJson = cleanJson.substring(7);
                            } else if (cleanJson.startsWith("```")) {
                                cleanJson = cleanJson.substring(3);
                            }
                            if (cleanJson.endsWith("```")) {
                                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                            }
                            cleanJson = cleanJson.trim();

                            JSONObject root = new JSONObject(cleanJson);
                            AIDirectorSpec spec = AIDirectorSpec.fromJson(root, activeModel);
                            
                            VynaraLogger.system("DirectorAgent: Live spec confirmed from [" + activeModel + "]. Zero fallbacks engaged.");
                            callback.onSpecReady(spec);
                        } catch (Exception e) {
                            String err = "DirectorAgent: Failed to parse Gemini response: " + e.getMessage();
                            VynaraLogger.e(err, e);
                            callback.onError(err);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        String err = "DirectorAgent: Google Gemini API error: " + errorMessage;
                        VynaraLogger.e(err);
                        callback.onError(err);
                    }
                }
        );
    }
}