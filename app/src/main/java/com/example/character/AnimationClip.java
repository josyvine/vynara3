package com.example.character;

import java.util.ArrayList;
import java.util.List;

public class AnimationClip {
    private String name;
    private float durationSeconds;
    private boolean isLooping;
    private final List<AnimationTrack> tracks = new ArrayList<>();

    public AnimationClip(String name, float durationSeconds, boolean isLooping) {
        this.name = name != null ? name : "clip";
        this.durationSeconds = Math.max(0.1f, durationSeconds);
        this.isLooping = isLooping;
    }

    public String getName() { return name; }
    public float getDurationSeconds() { return durationSeconds; }
    public boolean isLooping() { return isLooping; }
    public List<AnimationTrack> getTracks() { return tracks; }

    public void setName(String name) { this.name = name; }
    public void setDurationSeconds(float durationSeconds) { 
        this.durationSeconds = Math.max(0.1f, durationSeconds); 
    }
    public void setLooping(boolean looping) { isLooping = looping; }

    public AnimationClip addTrack(AnimationTrack track) {
        if (track != null) {
            tracks.add(track);
            recalculateDuration();
        }
        return this;
    }

    public AnimationTrack getTrackForBone(String boneSemanticName) {
        if (boneSemanticName == null) return null;
        for (AnimationTrack track : tracks) {
            if (boneSemanticName.equalsIgnoreCase(track.getBoneSemanticName())) {
                return track;
            }
        }
        return null;
    }

    /**
     * Phase 10 Alignment: Scans all keyframes across attached bone tracks
     * and automatically extends clip duration to match the furthest keyframe.
     */
    public void recalculateDuration() {
        float maxTime = durationSeconds;
        for (AnimationTrack track : tracks) {
            if (track != null && track.getKeyframes() != null) {
                for (Keyframe kf : track.getKeyframes()) {
                    if (kf.getTimestampSeconds() > maxTime) {
                        maxTime = kf.getTimestampSeconds();
                    }
                }
            }
        }
        this.durationSeconds = maxTime;
    }

    public AnimationClip cloneClip(String newName) {
        AnimationClip copy = new AnimationClip(newName != null ? newName : this.name, this.durationSeconds, this.isLooping);
        for (AnimationTrack track : tracks) {
            if (track != null) {
                copy.addTrack(track.cloneTrack());
            }
        }
        return copy;
    }
}