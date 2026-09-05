package com.example.validation.validators;

import com.example.character.Skin;
import com.example.character.SkinWeight;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class SkinValidator {

    /**
     * Phase 11 Alignment: Specialized Skin Validator. Inspects character skin
     * vertex weights, bone influence references, and multi-bone normalization thresholds.
     */
    public List<ValidationResult> validate(Skin skin, String characterName) {
        List<ValidationResult> results = new ArrayList<>();
        String name = characterName != null ? characterName : "Character";

        if (skin == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SKIN,
                    "Character " + name + " has missing or uninitialized skin binder.",
                    "Initialize Skin class and bind mesh vertices.",
                    null));
            return results;
        }

        if (skin.getSkeleton() == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SKIN,
                    "Character " + name + " skin binder is missing skeleton joint references.",
                    "Re-bind skin class to a valid Skeleton.",
                    null));
            return results;
        }

        List<List<SkinWeight>> vertexSkinWeights = skin.getVertexSkinWeights();
        if (vertexSkinWeights == null || vertexSkinWeights.isEmpty()) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SKIN,
                    "Character " + name + " skin contains 0 weighted vertices.",
                    "Invoke Skin.bindMeshVerticesToSkeleton() to calculate weights.",
                    null));
            return results;
        }

        // Validate normalization and bone references across all vertices
        for (int i = 0; i < vertexSkinWeights.size(); i++) {
            List<SkinWeight> weights = vertexSkinWeights.get(i);
            if (weights == null || weights.isEmpty()) {
                results.add(new ValidationResult(
                        ValidationResult.Severity.ERROR,
                        ValidationResult.Category.SKIN,
                        "Character " + name + " has unweighted vertex at index: " + i,
                        "Verify vertex binding coverage.",
                        null));
                continue;
            }

            float sum = 0f;
            for (SkinWeight sw : weights) {
                if (sw.getBoneId() == null || skin.getSkeleton().getBoneById(sw.getBoneId()) == null) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.CRITICAL,
                            ValidationResult.Category.SKIN,
                            "Character " + name + " vertex (" + i + ") references non-existent or invalid bone ID: " + sw.getBoneId(),
                            "Re-bind skinning to map only valid skeleton joint IDs.",
                            null));
                    break;
                }
                sum += sw.getWeight();
            }

            // Check weight normalization threshold (0.01 margin of error)
            if (Math.abs(sum - 1.0f) > 0.01f) {
                results.add(new ValidationResult(
                        ValidationResult.Severity.ERROR,
                        ValidationResult.Category.SKIN,
                        "Character " + name + " vertex (" + i + ") has non-normalized bone skin weights summing to: " + sum,
                        "Invoke Skin.normalizeWeights() to force summation to 1.0.",
                        null));
            }
        }

        return results;
    }
}