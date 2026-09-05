package com.example.validation.validators;

import com.example.character.Rig;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RigValidator {

    /**
     * Phase 11 Alignment: Specialized Rig Validator. Inspects Inverse Kinematics (IK)
     * target bindings, bone structural joints, and target dimension coordinates.
     */
    public List<ValidationResult> validate(Rig rig, String characterName) {
        List<ValidationResult> results = new ArrayList<>();
        String name = characterName != null ? characterName : "Character";

        if (rig == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.RIG,
                    "Character " + name + " has missing or uninitialized rig controls.",
                    "Initialize Rig class with a valid Skeleton.",
                    null));
            return results;
        }

        if (rig.getSkeleton() == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.RIG,
                    "Character " + name + " rig is missing core Skeleton references.",
                    "Re-bind rig to a valid Skeleton.",
                    null));
            return results;
        }

        Map<String, float[]> ikTargets = rig.getIkTargets();
        if (ikTargets != null && !ikTargets.isEmpty()) {
            for (String limb : ikTargets.keySet()) {
                String targetBoneName = mapLimbToTargetBone(limb);
                
                // Verify target IK bone actually exists in the active skeleton joint tree
                if (rig.getSkeleton().getBoneBySemanticName(targetBoneName) == null) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.WARNING,
                            ValidationResult.Category.RIG,
                            "Character " + name + " rig limb '" + limb + "' targets a missing or unmapped joint: " + targetBoneName,
                            "Add " + targetBoneName + " to the skeleton joint tree.",
                            null));
                }

                // Verify IK coordinate array dimensions
                float[] targetPos = ikTargets.get(limb);
                if (targetPos == null || targetPos.length < 3) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.ERROR,
                            ValidationResult.Category.RIG,
                            "Character " + name + " rig limb '" + limb + "' has empty or invalid coordinate dimensions.",
                            "Initialize target coordinates as float[3].",
                            null));
                }
            }
        }

        return results;
    }

    private String mapLimbToTargetBone(String limb) {
        if (limb == null) return "ROOT";
        String key = limb.toLowerCase().trim();
        switch (key) {
            case "left_arm": return "LEFT_HAND";
            case "right_arm": return "RIGHT_HAND";
            case "left_leg": return "LEFT_FOOT";
            case "right_leg": return "RIGHT_FOOT";
            case "front_left_leg": return "FRONT_LEFT_FOOT";
            case "front_right_leg": return "FRONT_RIGHT_FOOT";
            case "rear_left_leg": return "REAR_LEFT_FOOT";
            case "rear_right_leg": return "REAR_RIGHT_FOOT";
            default: return "ROOT";
        }
    }
}