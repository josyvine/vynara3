package com.example.validation.validators;

import com.example.character.Bone;
import com.example.character.Skeleton;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkeletonValidator {

    /**
     * Phase 11 Alignment: Specialized Skeleton Validator. Inspects character skeleton
     * bone hierarchies, root joint bindings, duplicate bone names, and cyclical bone dependencies.
     */
    public List<ValidationResult> validate(Skeleton skeleton, String characterName) {
        List<ValidationResult> results = new ArrayList<>();
        String name = characterName != null ? characterName : "Character";

        if (skeleton == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SKELETON,
                    "Character " + name + " has missing or uninitialized skeletal bone hierarchy.",
                    "Call SkeletonBuilder to construct bone joints.",
                    null));
            return results;
        }

        if (skeleton.getRootBone() == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SKELETON,
                    "Character " + name + " is missing its root joint anchor bone.",
                    "Bind a valid root bone at the top of the skeleton chain.",
                    null));
            return results;
        }

        if (skeleton.getBoneCount() == 0) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.SKELETON,
                    "Character " + name + " contains 0 registered joints.",
                    "Rebuild skeleton hierarchy joints.",
                    null));
            return results;
        }

        // Verify duplicate semantic joint names and cyclical loops inside joint trees
        Set<String> visitedSemanticNames = new HashSet<>();
        Set<Bone> visitedBones = new HashSet<>();
        checkBoneHierarchyRecursively(skeleton.getRootBone(), visitedSemanticNames, visitedBones, results, name);

        return results;
    }

    private void checkBoneHierarchyRecursively(Bone bone, Set<String> visitedNames, Set<Bone> visitedBones, 
                                               List<ValidationResult> results, String charName) {
        if (bone == null) return;

        // Cyclical linkage loop verification
        if (visitedBones.contains(bone)) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.CRITICAL,
                    ValidationResult.Category.SKELETON,
                    "Character " + charName + " has a cyclic dependency loop at bone joint: " + bone.getSemanticName(),
                    "Re-parent bone linkages to eliminate cyclic loops.",
                    bone.getId()));
            return;
        }
        visitedBones.add(bone);

        // Duplicate semantic name verification
        String semantic = bone.getSemanticName();
        if (semantic == null || semantic.trim().isEmpty()) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.WARNING,
                    ValidationResult.Category.SKELETON,
                    "Character " + charName + " has a joint with an empty or null semantic name reference.",
                    "Assign clear joint semantic identifiers.",
                    bone.getId()));
        } else {
            String upper = semantic.toUpperCase().trim();
            if (visitedNames.contains(upper)) {
                results.add(new ValidationResult(
                        ValidationResult.Severity.WARNING,
                        ValidationResult.Category.SKELETON,
                        "Character " + charName + " has duplicate joint name bindings in its tree: " + semantic,
                        "Verify bone joint semantic names are unique.",
                        bone.getId()));
            }
            visitedNames.add(upper);
        }

        for (Bone child : bone.getChildren()) {
            checkBoneHierarchyRecursively(child, visitedNames, visitedBones, results, charName);
        }
    }
}