package com.example.character;

public class SkinWeight {
    private String boneId;
    private float weight;

    public SkinWeight(String boneId, float weight) {
        this.boneId = boneId != null ? boneId : "bone_root";
        this.weight = Math.max(0.0f, Math.min(1.0f, weight));
    }

    public String getBoneId() { 
        return boneId; 
    }

    public float getWeight() { 
        return weight; 
    }

    public void setBoneId(String boneId) {
        if (boneId != null && !boneId.trim().isEmpty()) {
            this.boneId = boneId;
        }
    }

    public void setWeight(float weight) { 
        this.weight = Math.max(0.0f, Math.min(1.0f, weight)); 
    }

    public SkinWeight cloneWeight() {
        return new SkinWeight(this.boneId, this.weight);
    }
}