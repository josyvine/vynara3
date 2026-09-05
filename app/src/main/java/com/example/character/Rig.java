package com.example.character;

import java.util.HashMap;
import java.util.Map;

public class Rig {
    private Skeleton skeleton;
    private final Map<String, float[]> ikTargets = new HashMap<>();
    private final Map<String, float[]> poleTargets = new HashMap<>();

    public Rig(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    /**
     * Phase 9 Alignment: Solves Inverse Kinematics targets for humanoid, quadruped,
     * and avian wings using distinct upper, lower, and end bone chains.
     */
    public void setIKTarget(String limb, float x, float y, float z) {
        if (skeleton == null || limb == null) return;

        String key = limb.toLowerCase().trim();
        ikTargets.put(key, new float[] { x, y, z });

        float[] pole = poleTargets.get(key);
        float px = pole != null ? pole[0] : 0f;
        float py = pole != null ? pole[1] : 0f;
        float pz = pole != null ? pole[2] : 1f;

        if ("left_arm".equals(key)) {
            // Corrected Upper Arm -> Forearm -> Hand chain
            IKSolver.solveTwoBoneIKWithPole(
                    getBoneWithFallback("LEFT_UPPER_ARM", "LEFT_ARM"),
                    getBoneWithFallback("LEFT_FOREARM", "LEFT_ARM"),
                    getBoneWithFallback("LEFT_HAND", "LEFT_HAND"),
                    x, y, z, px, py, pz);
        } else if ("right_arm".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    getBoneWithFallback("RIGHT_UPPER_ARM", "RIGHT_ARM"),
                    getBoneWithFallback("RIGHT_FOREARM", "RIGHT_ARM"),
                    getBoneWithFallback("RIGHT_HAND", "RIGHT_HAND"),
                    x, y, z, px, py, pz);
        } else if ("left_leg".equals(key)) {
            // Phase 9 Fix: Resolved duplicate LEFT_LEG bone bug -> Thigh -> Calf -> Foot chain
            IKSolver.solveTwoBoneIKWithPole(
                    getBoneWithFallback("LEFT_THIGH", "LEFT_LEG"),
                    getBoneWithFallback("LEFT_CALF", "LEFT_LEG"),
                    getBoneWithFallback("LEFT_FOOT", "LEFT_FOOT"),
                    x, y, z, px, py, pz);
        } else if ("right_leg".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    getBoneWithFallback("RIGHT_THIGH", "RIGHT_LEG"),
                    getBoneWithFallback("RIGHT_CALF", "RIGHT_LEG"),
                    getBoneWithFallback("RIGHT_FOOT", "RIGHT_FOOT"),
                    x, y, z, px, py, pz);
        } else if ("front_left_leg".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    skeleton.getBoneBySemanticName("FRONT_LEFT_UPPER_LEG"),
                    skeleton.getBoneBySemanticName("FRONT_LEFT_LOWER_LEG"),
                    skeleton.getBoneBySemanticName("FRONT_LEFT_FOOT"),
                    x, y, z, px, py, pz);
        } else if ("front_right_leg".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    skeleton.getBoneBySemanticName("FRONT_RIGHT_UPPER_LEG"),
                    skeleton.getBoneBySemanticName("FRONT_RIGHT_LOWER_LEG"),
                    skeleton.getBoneBySemanticName("FRONT_RIGHT_FOOT"),
                    x, y, z, px, py, pz);
        } else if ("rear_left_leg".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    skeleton.getBoneBySemanticName("REAR_LEFT_UPPER_LEG"),
                    skeleton.getBoneBySemanticName("REAR_LEFT_LOWER_LEG"),
                    skeleton.getBoneBySemanticName("REAR_LEFT_FOOT"),
                    x, y, z, px, py, pz);
        } else if ("rear_right_leg".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    skeleton.getBoneBySemanticName("REAR_RIGHT_UPPER_LEG"),
                    skeleton.getBoneBySemanticName("REAR_RIGHT_LOWER_LEG"),
                    skeleton.getBoneBySemanticName("REAR_RIGHT_FOOT"),
                    x, y, z, px, py, pz);
        } else if ("left_wing".equals(key)) {
            // Avian wing assembly IK chain
            IKSolver.solveTwoBoneIKWithPole(
                    skeleton.getBoneBySemanticName("LEFT_WING_ARM"),
                    skeleton.getBoneBySemanticName("LEFT_WING_TIP"),
                    skeleton.getBoneBySemanticName("TAIL_FEATHERS"), // Fallback chain end
                    x, y, z, px, py, pz);
        } else if ("right_wing".equals(key)) {
            IKSolver.solveTwoBoneIKWithPole(
                    skeleton.getBoneBySemanticName("RIGHT_WING_ARM"),
                    skeleton.getBoneBySemanticName("RIGHT_WING_TIP"),
                    skeleton.getBoneBySemanticName("TAIL_FEATHERS"),
                    x, y, z, px, py, pz);
        }
    }

    public void setPoleTarget(String limb, float x, float y, float z) {
        if (limb == null) return;
        String key = limb.toLowerCase().trim();
        poleTargets.put(key, new float[] { x, y, z });

        float[] target = ikTargets.get(key);
        if (target != null) {
            setIKTarget(key, target[0], target[1], target[2]);
        }
    }

    private Bone getBoneWithFallback(String primarySemantic, String fallbackSemantic) {
        if (skeleton == null) return null;
        Bone bone = skeleton.getBoneBySemanticName(primarySemantic);
        return bone != null ? bone : skeleton.getBoneBySemanticName(fallbackSemantic);
    }

    public Skeleton getSkeleton() { return skeleton; }
    public Map<String, float[]> getIkTargets() { return ikTargets; }
    public Map<String, float[]> getPoleTargets() { return poleTargets; }
}