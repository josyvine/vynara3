package com.example.ai.agents;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.example.ai.ApiKeyManager;
import com.example.ai.GeminiApiClient;
import com.example.ai.protocol.AIDirectorSpec;
import com.example.utils.VynaraLogger;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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
     * Phase 1: Formulates the scene specification using Gemini Vision.
     * Encodes reference photos to Base64 so the AI visually analyzes the scene structure.
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

        // 1. Read and encode reference images to Base64 for Gemini Vision
        List<String> base64Images = new ArrayList<>();
        if (referenceImageUris != null && !referenceImageUris.isEmpty()) {
            for (String uriOrPath : referenceImageUris) {
                String b64 = readImageAsBase64(uriOrPath);
                if (b64 != null && !b64.isEmpty()) {
                    base64Images.add(b64);
                } else {
                    VynaraLogger.w("DirectorAgent: Reference image could not be converted to Base64: " + uriOrPath);
                }
            }
        }

        String systemInstruction = "You are the 3D Master Art Director & Spatial Architect.\n" +
                "YOUR ROLE:\n" +
                "- You NEVER write Python code or Blender operators.\n" +
                "- Your sole job is to design the visual contract, spatial constraints, and camera framing for the scene.\n" +
                "- If reference images are provided, visually inspect the architecture, lighting angles, water features, materials, and composition to replicate them.\n" +
                "CINEMATIC RULES:\n" +
                "1. Camera: Pick an intentional focal length (35mm for wide architecture/landscapes, 50mm for natural perspective, 85mm for furniture/characters).\n" +
                "2. Depth of Field (DOF): Use a shallow aperture (f/1.4 to f/2.8, default f/1.8) focused on the hero subject.\n" +
                "3. Volumetrics & Lighting: For atmospheric scenes, set useVolumetrics=true with density between 0.01 and 0.025 to create sunbeams. Set low sun angles (elevation 15-25 degrees) for dramatic long shadows.\n" +
                "4. Color Palette: Output three harmonic hex colors (primary structure, secondary detail/earth, accent highlight) matching the visual image.\n" +
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
        if (!base64Images.isEmpty()) {
            promptBuilder.append("VISUAL REFERENCE ATTACHED: Inspect the attached visual reference image(s). Match the architectural features, pool illumination, materials, and lighting mood.\n");
        }

        VynaraLogger.system("DirectorAgent: Formulating live scene specification via Gemini Vision [" + activeModel + "] with " + base64Images.size() + " image(s)...");

        // 2. Dispatch Multimodal Request with Base64 Images
        apiClient.generateStructuredJson(
                apiKeyManager.getApiKey(),
                activeModel,
                systemInstruction,
                promptBuilder.toString(),
                base64Images,
                new GeminiApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String jsonResult) {
                        try {
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

    /**
     * Resolves local file paths, file URIs, or pre-encoded base64 strings,
     * downscaling large images to a max 1024px dimension to ensure fast network latency.
     */
    private String readImageAsBase64(String pathOrUri) {
        if (pathOrUri == null || pathOrUri.trim().isEmpty()) return null;

        String cleanPath = pathOrUri.trim();

        // Handle file:// URI scheme
        if (cleanPath.startsWith("file://")) {
            cleanPath = cleanPath.substring(7);
        }

        // Handle raw Base64 data strings if already formatted
        if (cleanPath.startsWith("data:image") && cleanPath.contains("base64,")) {
            return cleanPath.substring(cleanPath.indexOf("base64,") + 7).trim();
        }

        try {
            File imageFile = new File(cleanPath);
            if (!imageFile.exists() || imageFile.length() == 0) {
                // If direct File object fails, check if the string itself is a raw base64 string
                if (cleanPath.length() > 100 && !cleanPath.contains(File.separator)) {
                    return cleanPath;
                }
                return null;
            }

            // Decode image bounds first to prevent memory OOM
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            int maxDim = Math.max(options.outWidth, options.outHeight);
            int inSampleSize = 1;
            while (maxDim / inSampleSize > 1024) {
                inSampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            if (bitmap == null) return null;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            bitmap.recycle();

            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            VynaraLogger.e("DirectorAgent: Error reading reference image: " + e.getMessage());
            return null;
        }
    }
}