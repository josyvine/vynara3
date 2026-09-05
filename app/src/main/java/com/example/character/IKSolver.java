package com.example.character;

public class IKSolver {

    /**
     * Phase 9 Alignment: Analytical 2-Bone Inverse Kinematics (IK) solver
     * using the Law of Cosines to solve upper and lower joint angles towards 3D target coordinates.
     */
    public static boolean solveLimbIK(Bone upperBone, Bone lowerBone, Bone endBone, 
                                       float targetX, float targetY, float targetZ) {
        return solveTwoBoneIKWithPole(upperBone, lowerBone, endBone, 
                targetX, targetY, targetZ, 0f, 0f, 1f);
    }

    public static boolean solveTwoBoneIK(Bone upperBone, Bone lowerBone, Bone endBone, 
                                         float targetX, float targetY, float targetZ) {
        return solveTwoBoneIKWithPole(upperBone, lowerBone, endBone, 
                targetX, targetY, targetZ, 0f, 0f, 1f);
    }

    /**
     * Phase 9 Alignment: Solves 2-Bone IK chain (e.g. Shoulder-Elbow-Hand or Thigh-Knee-Foot)
     * using Law of Cosines with target position and pole vector orientation.
     */
    public static boolean solveTwoBoneIKWithPole(Bone upperBone, Bone lowerBone, Bone endBone, 
                                                 float targetX, float targetY, float targetZ, 
                                                 float poleX, float poleY, float poleZ) {
        if (upperBone == null || lowerBone == null || endBone == null) return false;

        // BIND-POSE ALIGNED SEGMENT LENGTHS (Calculated using parent-local offset vectors)
        float len1 = (float) Math.sqrt(
                lowerBone.getLocalTransform().getPx() * lowerBone.getLocalTransform().getPx() +
                lowerBone.getLocalTransform().getPy() * lowerBone.getLocalTransform().getPy() +
                lowerBone.getLocalTransform().getPz() * lowerBone.getLocalTransform().getPz()
        );
        float len2 = (float) Math.sqrt(
                endBone.getLocalTransform().getPx() * endBone.getLocalTransform().getPx() +
                endBone.getLocalTransform().getPy() * endBone.getLocalTransform().getPy() +
                endBone.getLocalTransform().getPz() * endBone.getLocalTransform().getPz()
        );

        if (len1 <= 0.001f) len1 = 0.4f;
        if (len2 <= 0.001f) len2 = 0.4f;

        // RESOLVE ABSOLUTE WORLD POSITION OF UPPER JOINT BASE
        float[] uWorld = getBoneWorldPosition(upperBone);

        // VECTOR FROM UPPER JOINT BASE TO GLOBAL TARGET POSITION
        float dx = targetX - uWorld[0];
        float dy = targetY - uWorld[1];
        float dz = targetZ - uWorld[2];

        float distSq = dx * dx + dy * dy + dz * dz;
        float dist = (float) Math.sqrt(distSq);

        // Clamp distance within bone chain reach limits
        float maxDist = (len1 + len2) * 0.999f;
        float minDist = Math.abs(len1 - len2) + 0.001f;
        dist = Math.max(minDist, Math.min(maxDist, dist));

        // Law of Cosines: Angle alpha at upper joint
        float cosAlpha = (len1 * len1 + dist * dist - len2 * len2) / (2f * len1 * dist);
        cosAlpha = Math.max(-1f, Math.min(1f, cosAlpha));
        float alphaRad = (float) Math.acos(cosAlpha);

        // Law of Cosines: Angle beta at knee/elbow joint
        float cosBeta = (len1 * len1 + len2 * len2 - dist * dist) / (2f * len1 * len2);
        cosBeta = Math.max(-1f, Math.min(1f, cosBeta));
        float betaRad = (float) Math.acos(cosBeta);

        float alphaDeg = (float) Math.toDegrees(alphaRad);
        float betaDeg = (float) Math.toDegrees(Math.PI - betaRad);

        // DYNAMIC AXIS-ALIGNMENT RESOLVER (vertical legs vs. horizontal arms)
        float lpx = lowerBone.getLocalTransform().getPx();
        float lpy = lowerBone.getLocalTransform().getPy();

        boolean isVerticalLimb = Math.abs(lpy) > Math.abs(lpx);

        if (isVerticalLimb) {
            // VERTICAL LIMB RESOLUTION (e.g., Humanoid/Quadruped Leg extending along -Y)
            float yawDeg = (float) Math.toDegrees(Math.atan2(dx, dz));
            float pitchDeg = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));

            // Apply soft joint boundaries (bends knee pitch backward)
            if (betaDeg < 0.0f) {
                betaDeg = 0.0f;
            } else if (betaDeg > 150.0f) {
                betaDeg = 150.0f;
            }

            // Calculate parent pitch and tilt offsets
            float targetPitch = pitchDeg - alphaDeg;
            float targetYaw = yawDeg;

            // Blend pole vector for knee direction alignment
            if (Math.abs(poleX) > 0.001f || Math.abs(poleZ) > 0.001f) {
                float poleYaw = (float) Math.toDegrees(Math.atan2(poleX, poleZ));
                targetYaw += poleYaw * 0.15f; // Smooth blend threshold
            }

            upperBone.getLocalTransform().setRotation(targetPitch, targetYaw, 0f);
            lowerBone.getLocalTransform().setRotation(betaDeg, 0f, 0f);
        } else {
            // HORIZONTAL LIMB RESOLUTION (e.g., Humanoid Arm extending along +/-X)
            boolean isLeftArm = lpx < 0;

            float yawDeg = (float) Math.toDegrees(Math.atan2(dz, dx));
            float pitchDeg = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));

            // Apply soft joint boundaries (bends elbow)
            if (betaDeg < 0.0f) {
                betaDeg = 0.0f;
            } else if (betaDeg > 150.0f) {
                betaDeg = 150.0f;
            }

            float targetPitch = pitchDeg;
            float targetRoll = isLeftArm ? (-yawDeg + alphaDeg) : (yawDeg - alphaDeg);

            // Incorporate pole vector offset for elbow layout alignment
            float targetYaw = 0f;
            if (Math.abs(poleY) > 0.001f) {
                targetYaw += (poleY > 0 ? -12f : 12f);
            }

            upperBone.getLocalTransform().setRotation(targetPitch, targetYaw, targetRoll);
            lowerBone.getLocalTransform().setRotation(0f, 0f, isLeftArm ? -betaDeg : betaDeg);
        }

        return true;
    }

    /**
     * Resolves the absolute world translation coordinate of a joint in default bind-pose 
     * by accumulating local transform translations up to the root bone.
     */
    private static float[] getBoneWorldPosition(Bone bone) {
        float[] pos = new float[] { 0f, 0f, 0f };
        Bone current = bone;
        while (current != null) {
            pos[0] += current.getLocalTransform().getPx();
            pos[1] += current.getLocalTransform().getPy();
            pos[2] += current.getLocalTransform().getPz();
            current = current.getParent();
        }
        return pos;
    }
}