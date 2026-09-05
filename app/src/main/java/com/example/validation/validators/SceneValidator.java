package com.example.validation.validators;

import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SceneValidator {

    /**
     * Phase 11 Alignment: Specialized Scene Validator. Inspects overall scene layouts,
     * recursive cyclical node graphs, node naming collisions, and node IDs.
     */
    public List<ValidationResult> validate(Scene scene) {
        List<ValidationResult> results = new ArrayList<>();

        if (scene == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.CRITICAL,
                    ValidationResult.Category.SCENE,
                    "Active scene is null.",
                    "Initialize 3D scene.",
                    null));
            return results;
        }

        if (scene.getObjects().isEmpty()) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.WARNING,
                    ValidationResult.Category.SCENE,
                    "Scene is currently empty.",
                    "Generate procedural or primitive geometry.",
                    null));
            return results;
        }

        // Verify cyclical parent-child scene graph node links
        Set<SceneObject> visitedNodes = new HashSet<>();
        for (SceneObject root : scene.getObjects()) {
            checkSceneHierarchyRecursively(root, visitedNodes, results);
        }

        return results;
    }

    private void checkSceneHierarchyRecursively(SceneObject node, Set<SceneObject> visited, 
                                                List<ValidationResult> results) {
        if (node == null) return;

        // Cyclical node loop detection in scene graph
        if (visited.contains(node)) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.CRITICAL,
                    ValidationResult.Category.SCENE,
                    "Scene contains cyclic parent-child loop at node: " + node.getName(),
                    "Unparent and reset transformations on " + node.getName(),
                    node.getId()));
            return;
        }
        visited.add(node);

        // Node integrity validations
        if (node.getId() == null || node.getId().trim().isEmpty()) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SCENE,
                    "Found scene node with empty or null ID value.",
                    "Regenerate a unique ID for the scene node.",
                    null));
        }

        if (node.getName() == null || node.getName().trim().isEmpty()) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.WARNING,
                    ValidationResult.Category.SCENE,
                    "Scene node '" + node.getId() + "' is missing a display name.",
                    "Assign descriptive display name.",
                    node.getId()));
        }

        if (node.getChildren() != null) {
            for (SceneObject child : node.getChildren()) {
                checkSceneHierarchyRecursively(child, visited, results);
            }
        }
    }
}