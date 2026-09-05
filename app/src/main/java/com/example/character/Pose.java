package com.example.character;

import java.util.HashMap;
import java.util.Map;

public class Pose {
    private String name;
    private final Map<String, float[]> boneRotations = new HashMap<>(); // SemanticName -> [rx, ry, rz]

    public Pose(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public Pose setBoneRotation(String boneSemanticName, float rx, float ry, float rz) {
        boneRotations.put(boneSemanticName.toUpperCase(), new float[] { rx, ry, rz });
        return this;
    }

    public void applyToSkeleton(Skeleton skeleton) {
        if (skeleton == null) return;
        for (Map.Entry<String, float[]> entry : boneRotations.entrySet()) {
            Bone bone = skeleton.getBoneBySemanticName(entry.getKey());
            if (bone != null) {
                float[] r = entry.getValue();
                bone.getLocalTransform().setRotation(r[0], r[1], r[2]);
            }
        }
    }
}
