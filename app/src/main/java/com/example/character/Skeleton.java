package com.example.character;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Skeleton {
    private Bone rootBone;
    private final Map<String, Bone> semanticBoneMap = new HashMap<>();
    private final Map<String, Bone> idBoneMap = new HashMap<>();
    private final List<Bone> boneList = new ArrayList<>();

    public Skeleton(Bone rootBone) {
        this.rootBone = rootBone;
        registerBoneRecursively(rootBone);
    }

    private void registerBoneRecursively(Bone bone) {
        if (bone == null) return;
        
        if (bone.getSemanticName() != null) {
            semanticBoneMap.put(bone.getSemanticName().toUpperCase(), bone);
        }
        if (bone.getId() != null) {
            idBoneMap.put(bone.getId(), bone);
        }
        
        if (!boneList.contains(bone)) {
            boneList.add(bone);
        }

        for (Bone child : bone.getChildren()) {
            registerBoneRecursively(child);
        }
    }

    public Bone getRootBone() { return rootBone; }

    public Bone getBoneBySemanticName(String semanticName) {
        if (semanticName == null) return null;
        return semanticBoneMap.get(semanticName.toUpperCase());
    }

    public Bone getBoneById(String id) {
        if (id == null) return null;
        return idBoneMap.get(id);
    }

    public int getBoneIndex(String semanticName) {
        if (semanticName == null) return -1;
        Bone bone = getBoneBySemanticName(semanticName);
        return bone != null ? boneList.indexOf(bone) : -1;
    }

    /**
     * Phase 9 Alignment: Recursively updates global world matrices across 
     * the skeleton hierarchy tree starting from the root bone.
     */
    public void updateWorldTransforms() {
        if (rootBone != null) {
            updateBoneWorldTransformRecursively(rootBone, null);
        }
    }

    private void updateBoneWorldTransformRecursively(Bone bone, float[] parentWorldMatrix) {
        if (bone == null || bone.getLocalTransform() == null) return;
        
        float[] worldMat = bone.getLocalTransform().getWorldMatrix(parentWorldMatrix);
        for (Bone child : bone.getChildren()) {
            updateBoneWorldTransformRecursively(child, worldMat);
        }
    }

    public List<Bone> getAllBones() {
        return boneList;
    }

    public int getBoneCount() { return boneList.size(); }
}