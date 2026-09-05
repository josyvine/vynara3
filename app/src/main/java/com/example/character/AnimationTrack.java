package com.example.character;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AnimationTrack {
    private String boneSemanticName;
    private final List<Keyframe> keyframes = new ArrayList<>();

    public AnimationTrack(String boneSemanticName) {
        this.boneSemanticName = boneSemanticName != null ? boneSemanticName.toUpperCase() : "ROOT";
    }

    public String getBoneSemanticName() { return boneSemanticName; }
    public List<Keyframe> getKeyframes() { return keyframes; }

    public void setBoneSemanticName(String boneSemanticName) {
        if (boneSemanticName != null && !boneSemanticName.trim().isEmpty()) {
            this.boneSemanticName = boneSemanticName.toUpperCase();
        }
    }

    /**
     * Phase 10 Alignment: Adds a keyframe and keeps the track chronologically 
     * sorted by timestamp.
     */
    public AnimationTrack addKeyframe(Keyframe kf) {
        if (kf != null) {
            keyframes.add(kf);
            Collections.sort(keyframes, Comparator.comparingDouble(Keyframe::getTimestampSeconds));
        }
        return this;
    }

    public Keyframe getKeyframeAt(int index) {
        if (index >= 0 && index < keyframes.size()) {
            return keyframes.get(index);
        }
        return null;
    }

    public AnimationTrack cloneTrack() {
        AnimationTrack copy = new AnimationTrack(this.boneSemanticName);
        for (Keyframe kf : keyframes) {
            if (kf != null) {
                copy.addKeyframe(kf.cloneKeyframe());
            }
        }
        return copy;
    }
}