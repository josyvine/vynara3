package com.example.ai;

import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.validation.ValidationManager;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class AIInspector {
    private final ValidationManager validationManager;

    public AIInspector(ValidationManager validationManager) {
        this.validationManager = validationManager != null ? validationManager : new ValidationManager();
    }

    /**
     * Inspects the active 3D scene and returns detailed validation results across
     * meshes, materials, skeletons, skinning, and lighting.
     */
    public List<ValidationResult> inspect(Scene scene) {
        if (scene == null) {
            List<ValidationResult> nullResults = new ArrayList<>();
            nullResults.add(new ValidationResult(ValidationResult.Severity.CRITICAL, "Scene instance is null.", "Initialize 3D ProjectRuntime scene."));
            return nullResults;
        }
        return validationManager.validateScene(scene);
    }

    /**
     * Inspects a specific scene object in isolation.
     */
    public List<ValidationResult> inspectObject(SceneObject object) {
        if (object == null) {
            List<ValidationResult> nullResults = new ArrayList<>();
            nullResults.add(new ValidationResult(ValidationResult.Severity.ERROR, "Target SceneObject is null.", "Verify target object ID."));
            return nullResults;
        }
        return validationManager.validateObject(object);
    }

    /**
     * Checks if the inspection results contain any critical or blocking errors.
     */
    public boolean hasCriticalErrors(List<ValidationResult> results) {
        if (results == null || results.isEmpty()) return false;
        for (ValidationResult vr : results) {
            if (vr.getSeverity() == ValidationResult.Severity.ERROR || 
                vr.getSeverity() == ValidationResult.Severity.CRITICAL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats inspection results into a structured text report for Gemini diagnosis.
     */
    public String getInspectionDiagnosticsReport(List<ValidationResult> results) {
        if (results == null || results.isEmpty()) {
            return "Inspection Status: CLEAN (0 Issues Detected)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("AI Inspection Report:\n");
        int issueCount = 0;
        for (ValidationResult vr : results) {
            if (!vr.isPassed()) {
                issueCount++;
                sb.append(" - [").append(vr.getSeverity().name()).append("] ")
                  .append(vr.getMessage());
                if (vr.getRepairSuggestion() != null) {
                    sb.append(" (Suggestion: ").append(vr.getRepairSuggestion()).append(")");
                }
                sb.append("\n");
            }
        }

        if (issueCount == 0) {
            return "Inspection Status: PASS (All validation checks passed)";
        }
        return sb.toString();
    }

    public ValidationManager getValidationManager() {
        return validationManager;
    }
}