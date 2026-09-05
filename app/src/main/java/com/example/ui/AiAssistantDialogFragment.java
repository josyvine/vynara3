package com.example.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.MainActivity;
import com.example.R;
import com.example.ai.AIContext;
import com.example.ai.GeminiApiClient;
import com.example.engine.Material;
import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.runtime.ProjectRuntime;

import org.json.JSONObject;

public class AiAssistantDialogFragment extends DialogFragment {

    private EditText etPrompt;
    private ProjectRuntime runtime;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Phase 1 Alignment: Retrieve the shared ProjectRuntime context
        if (getActivity() instanceof MainActivity) {
            runtime = ((MainActivity) getActivity()).getProjectRuntime();
        } else {
            runtime = ProjectRuntime.getInstance(requireContext());
        }

        etPrompt = view.findViewById(R.id.et_studio_ai_prompt);

        Button btnCancel = view.findViewById(R.id.btn_dialog_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        Button btnApply = view.findViewById(R.id.btn_dialog_apply);
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                String input = etPrompt.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter an edit prompt", Toast.LENGTH_SHORT).show();
                    return;
                }

                executeStudioAiEdit(input);
            });
        }
    }

    /**
     * Phase 2 & 16 Alignment: Serializes active viewport 3D state, requests natural language
     * edit parameters from Gemini AI, and applies transform or material changes.
     */
    private void executeStudioAiEdit(String editPrompt) {
        if (runtime == null || getContext() == null) return;

        Toast.makeText(getContext(), "AI analyzing edit request...", Toast.LENGTH_SHORT).show();

        // 1. Serialize active viewport 3D state to supply context to Gemini
        Scene activeScene = runtime.getEngine().getSceneManager().getActiveScene();
        String contextJson = AIContext.buildSceneContextJson(activeScene);

        // 2. Query Gemini Assistant for edit parameters (PBR, transform, lighting)
        runtime.getAIOrchestrator().processNaturalLanguageStudioEdit(editPrompt, contextJson, new GeminiApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String rawJsonResult) {
                mainHandler.post(() -> {
                    boolean success = applyAiEditToScene(rawJsonResult);
                    if (success) {
                        Toast.makeText(getContext(), "AI edit applied successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        Toast.makeText(getContext(), "Failed to apply AI edit changes", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "AI Edit Error: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean applyAiEditToScene(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty() || runtime == null) return false;

        try {
            JSONObject root = new JSONObject(rawJson);
            
            // Extract targeting parameters
            String targetId = root.optString("targetObjectId", null);
            SceneObject targetNode = null;
            if (targetId != null) {
                targetNode = runtime.getEngine().getSceneManager().findObjectById(targetId);
            }
            if (targetNode == null) {
                targetNode = runtime.getEngine().getSceneManager().getSelectedObject();
            }
            if (targetNode == null && !runtime.getEngine().getSceneManager().getAllObjects().isEmpty()) {
                targetNode = runtime.getEngine().getSceneManager().getAllObjects().get(0);
            }

            if (targetNode == null) return false;

            // Begin scene transaction for undo/redo capability
            runtime.getTransactionManager().beginTransaction("Apply AI Edit: " + targetNode.getName());

            // 1. Process Translate / Transform updates
            JSONObject transform = root.optJSONObject("transform");
            if (transform != null) {
                float px = (float) transform.optDouble("px", targetNode.getTransform().getPx());
                float py = (float) transform.optDouble("py", targetNode.getTransform().getPy());
                float pz = (float) transform.optDouble("pz", targetNode.getTransform().getPz());
                targetNode.getTransform().setPosition(px, py, pz);

                float rx = (float) transform.optDouble("rx", targetNode.getTransform().getRx());
                float ry = (float) transform.optDouble("ry", targetNode.getTransform().getRy());
                float rz = (float) transform.optDouble("rz", targetNode.getTransform().getRz());
                targetNode.getTransform().setRotation(rx, ry, rz);

                float sx = (float) transform.optDouble("sx", targetNode.getTransform().getSx());
                float sy = (float) transform.optDouble("sy", targetNode.getTransform().getSy());
                float sz = (float) transform.optDouble("sz", targetNode.getTransform().getSz());
                targetNode.getTransform().setScale(sx, sy, sz);
            }

            // 2. Process Material property updates
            JSONObject material = root.optJSONObject("material");
            if (material != null) {
                String colorHex = material.optString("colorHex", null);
                float metallic = (float) material.optDouble("metallic", targetNode.getMaterial() != null ? targetNode.getMaterial().getMetallic() : 0.1f);
                float roughness = (float) material.optDouble("roughness", targetNode.getMaterial() != null ? targetNode.getMaterial().getRoughness() : 0.5f);
                float opacity = (float) material.optDouble("opacity", targetNode.getMaterial() != null ? targetNode.getMaterial().getOpacity() : 1.0f);

                Material editMat = new Material("mat_" + System.currentTimeMillis(), "AI Edit Material", colorHex != null ? colorHex : "#FFFFFF");
                editMat.setMetallic(metallic);
                editMat.setRoughness(roughness);
                editMat.setOpacity(opacity);
                targetNode.setMaterial(editMat);
            }

            // Commit transaction on success
            runtime.getTransactionManager().commitTransaction();
            return true;

        } catch (Exception e) {
            runtime.getTransactionManager().rollbackTransaction();
            return false;
        }
    }
}