package com.example.validation.validators;

import com.example.character.AnimationClip;
import com.example.character.AnimationTrack;
import com.example.character.Keyframe;
import com.example.character.Skeleton;
import com.example.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class AnimationValidator {

    /**
     * Phase 11 Alignment: Specialized Animation Validator. Inspects animation tracks,
     * bone target bindings, keyframe counts, chronological timestamps, and timeline durations.
     */
    public List<ValidationResult> validate(AnimationClip clip, Skeleton skeleton, String characterName) {
        List<ValidationResult> results = new ArrayList<>();
        String name = characterName != null ? characterName : "Character";

        if (clip == null) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.ANIMATION,
                    "Character " + name + " is missing its active AnimationClip.",
                    "Re-bind or play a valid AnimationClip.",
                    null));
            return results;
        }

        if (clip.getTracks().isEmpty()) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.WARNING,
                    ValidationResult.Category.ANIMATION,
                    "AnimationClip '" + clip.getName() + "' contains 0 active bone tracks.",
                    "Add AnimationTrack objects with keyframes to the clip.",
                    null));
            return results;
        }

        float duration = clip.getDurationSeconds();
        if (duration <= 0.001f) {
            results.add(new ValidationResult(
                    ValidationResult.Severity.ERROR,
                    ValidationResult.Category.ANIMATION,
                    "AnimationClip '" + clip.getName() + "' has invalid duration (" + duration + "s). Duration must be positive.",
                    "Set animation duration using setDurationSeconds().",
                    null));
        }

        for (AnimationTrack track : clip.getTracks()) {
            if (track == null) continue;

            // Verify bone targets exist inside active skeleton
            if (skeleton != null && skeleton.getBoneBySemanticName(track.getBoneSemanticName()) == null) {
                results.add(new ValidationResult(
                        ValidationResult.Severity.WARNING,
                        ValidationResult.Category.ANIMATION,
                        "AnimationClip '" + clip.getName() + "' contains track '" + track.getBoneSemanticName() + "' targeting non-existent joint.",
                        "Align track semantic name to skeleton bone structures.",
                        null));
            }

            List<Keyframe> keyframes = track.getKeyframes();
            if (keyframes == null || keyframes.isEmpty()) {
                results.add(new ValidationResult(
                        ValidationResult.Severity.ERROR,
                        ValidationResult.Category.ANIMATION,
                        "Track '" + track.getBoneSemanticName() + "' inside '" + clip.getName() + "' contains 0 keyframes.",
                        "Add Keyframe timestamps to the animation track.",
                        null));
                continue;
            }

            // Verify chronological keyframe timestamps
            float lastTime = -1f;
            for (int i = 0; i < keyframes.size(); i++) {
                Keyframe kf = keyframes.get(i);
                float time = kf.getTimestampSeconds();

                if (time < 0f) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.ERROR,
                            ValidationResult.Category.ANIMATION,
                            "Track '" + track.getBoneSemanticName() + "' contains invalid negative timestamp: " + time + "s",
                            "Clamp keyframe timestamps to positive values.",
                            null));
                }

                if (time > duration) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.WARNING,
                            ValidationResult.Category.ANIMATION,
                            "Track '" + track.getBoneSemanticName() + "' contains keyframe at " + time + "s exceeding clip duration (" + duration + "s).",
                            "Invoke recalculateDuration() on the AnimationClip.",
                            null));
                }

                if (time < lastTime) {
                    results.add(new ValidationResult(
                            ValidationResult.Severity.ERROR,
                            ValidationResult.Category.ANIMATION,
                            "Track '" + track.getBoneSemanticName() + "' contains non-chronological keyframes: " + time + "s is before " + lastTime + "s",
                            "Sort track keyframe arrays chronologically.",
                            null));
                }
                lastTime = time;
            }
        }

        return results;
    }
}