package com.example.validation.validators;

import com.example.engine.Material;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class MaterialValidator {

    /**
     * Phase 11 Alignment: Specialized Material Validator. Inspect PBR shading parameters
     * including base color channels, metallic/roughness values, and opacity ranges.
     */
    public List<ValidationResult> validate(Material material, String objectName) {
        List<ValidationResult> results = new ArrayList<>();
        String name = objectName != null ? objectName : "Unknown Mesh";

        if (material == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.WARNING,
                    ValidationResult.Category.MATERIAL,
                    "Object " + name + " has no material assigned.",
                    "Assign default PBR material properties.",
                    null));
            return results;
        }

        // Validate Metallic bounds (0.0 to 1.0)
        float metallic = material.getMetallic();
        if (metallic < 0.0f || metallic > 1.0f) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.MATERIAL,
                    "Material " + material.getName() + " on " + name + " has invalid metallic factor (" + metallic + "). Must be between 0.0 and 1.0.",
                    "Clamp metallic value to [0.0, 1.0] range.",
                    material.getId()));
        }

        // Validate Roughness bounds (0.0 to 1.0)
        float roughness = material.getRoughness();
        if (roughness < 0.0f || roughness > 1.0f) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.MATERIAL,
                    "Material " + material.getName() + " on " + name + " has invalid roughness factor (" + roughness + "). Must be between 0.0 and 1.0.",
                    "Clamp roughness value to [0.0, 1.0] range.",
                    material.getId()));
        }

        // Validate Opacity bounds (0.0 to 1.0)
        float opacity = material.getOpacity();
        if (opacity < 0.0f || opacity > 1.0f) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.MATERIAL,
                    "Material " + material.getName() + " on " + name + " has invalid opacity factor (" + opacity + "). Must be between 0.0 and 1.0.",
                    "Clamp opacity value to [0.0, 1.0] range.",
                    material.getId()));
        }

        // Validate Base Color channels and array structure
        float[] rgba = material.getBaseColorRGBA();
        if (rgba == null || rgba.length < 4) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.MATERIAL,
                    "Material " + material.getName() + " on " + name + " has missing or invalid RGBA base color channel arrays.",
                    "Re-initialize baseColorRGBA array to float[4].",
                    material.getId()));
        } else {
            for (int i = 0; i < 4; i++) {
                if (rgba[i] < 0.0f || rgba[i] > 1.0f) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.WARNING,
                            ValidationResult.Category.MATERIAL,
                            "Material " + material.getName() + " on " + name + " has color channel (" + i + ") out of standard bounds: " + rgba[i],
                            "Clamp color channel values to [0.0, 1.0].",
                            material.getId()));
                    break;
                }
            }
        }

        return results;
    }
}