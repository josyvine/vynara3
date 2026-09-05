package com.example.character;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationPlayer {
    private Skeleton skeleton;
    private final Map<String, AnimationClip> clipLibrary = new HashMap<>();
    private AnimationClip activeClip;
    private float currentTimeSeconds = 0f;
    private boolean isPlaying = false;

    public AnimationPlayer(Skeleton skeleton) {
        this.skeleton = skeleton;
        loadDefaultClips();
    }

    public void loadDefaultClips() {
        clipLibrary.clear();

        // 1. Idle Animation Clip (Subtle Breathing & Spine Offset)
        AnimationClip idle = new AnimationClip("idle", 2.0f, true);
        AnimationTrack spineTrack = new AnimationTrack("SPINE")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(1.0f, 0f, 0.02f, 0f, 2f, 0f, 0f))
                .addKeyframe(new Keyframe(2.0f, 0f, 0f, 0f, 0f, 0f, 0f));
        idle.addTrack(spineTrack);
        clipLibrary.put("idle", idle);

        // 2. Walk Animation Clip (Anatomical Bipedal Gait Cycle)
        AnimationClip walk = new AnimationClip("walk", 1.2f, true);

        AnimationTrack lThighTrack = new AnimationTrack("LEFT_THIGH")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 25f, 0f, 0f))
                .addKeyframe(new Keyframe(0.3f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(0.6f, 0f, 0f, 0f, -25f, 0f, 0f))
                .addKeyframe(new Keyframe(0.9f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(1.2f, 0f, 0f, 0f, 25f, 0f, 0f));

        AnimationTrack rThighTrack = new AnimationTrack("RIGHT_THIGH")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, -25f, 0f, 0f))
                .addKeyframe(new Keyframe(0.3f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(0.6f, 0f, 0f, 0f, 25f, 0f, 0f))
                .addKeyframe(new Keyframe(0.9f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(1.2f, 0f, 0f, 0f, -25f, 0f, 0f));

        // Adding Calf Tracks for correct knee flexion
        AnimationTrack lCalfTrack = new AnimationTrack("LEFT_CALF")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(0.3f, 0f, 0f, 0f, 35f, 0f, 0f)) // Bend knee
                .addKeyframe(new Keyframe(0.6f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(1.2f, 0f, 0f, 0f, 0f, 0f, 0f));

        AnimationTrack rCalfTrack = new AnimationTrack("RIGHT_CALF")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(0.6f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(0.9f, 0f, 0f, 0f, 35f, 0f, 0f)) // Bend knee
                .addKeyframe(new Keyframe(1.2f, 0f, 0f, 0f, 0f, 0f, 0f));

        AnimationTrack lArmTrack = new AnimationTrack("LEFT_UPPER_ARM")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, -15f, 0f, 0f))
                .addKeyframe(new Keyframe(0.6f, 0f, 0f, 0f, 15f, 0f, 0f))
                .addKeyframe(new Keyframe(1.2f, 0f, 0f, 0f, -15f, 0f, 0f));

        AnimationTrack rArmTrack = new AnimationTrack("RIGHT_UPPER_ARM")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 15f, 0f, 0f))
                .addKeyframe(new Keyframe(0.6f, 0f, 0f, 0f, -15f, 0f, 0f))
                .addKeyframe(new Keyframe(1.2f, 0f, 0f, 0f, 15f, 0f, 0f));

        walk.addTrack(lThighTrack).addTrack(rThighTrack).addTrack(lCalfTrack).addTrack(rCalfTrack)
            .addTrack(lArmTrack).addTrack(rArmTrack);
        clipLibrary.put("walk", walk);

        // 3. Run Animation Clip (Fast Gait Cycle)
        AnimationClip run = new AnimationClip("run", 0.7f, true);
        AnimationTrack lRunThigh = new AnimationTrack("LEFT_THIGH")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 40f, 0f, 0f))
                .addKeyframe(new Keyframe(0.35f, 0f, 0f, 0f, -40f, 0f, 0f))
                .addKeyframe(new Keyframe(0.7f, 0f, 0f, 0f, 40f, 0f, 0f));

        AnimationTrack rRunThigh = new AnimationTrack("RIGHT_THIGH")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, -40f, 0f, 0f))
                .addKeyframe(new Keyframe(0.35f, 0f, 0f, 0f, 40f, 0f, 0f))
                .addKeyframe(new Keyframe(0.7f, 0f, 0f, 0f, -40f, 0f, 0f));

        run.addTrack(lRunThigh).addTrack(rRunThigh);
        clipLibrary.put("run", run);

        // 4. Jump Animation Clip
        AnimationClip jump = new AnimationClip("jump", 1.0f, false);
        AnimationTrack rootJumpTrack = new AnimationTrack("ROOT")
                .addKeyframe(new Keyframe(0.0f, 0f, 0f, 0f, 0f, 0f, 0f))
                .addKeyframe(new Keyframe(0.5f, 0f, 0.8f, 0f, -10f, 0f, 0f))
                .addKeyframe(new Keyframe(1.0f, 0f, 0f, 0f, 0f, 0f, 0f));
        jump.addTrack(rootJumpTrack);
        clipLibrary.put("jump", jump);
    }

    public void playClip(String clipName) {
        if (clipName == null) return;
        String key = clipName.toLowerCase().trim();
        if (clipLibrary.containsKey(key)) {
            this.activeClip = clipLibrary.get(key);
            this.currentTimeSeconds = 0f;
            this.isPlaying = true;
        }
    }

    public void pause() { isPlaying = false; }
    public void resume() { isPlaying = true; }
    
    public void stop() {
        isPlaying = false;
        currentTimeSeconds = 0f;
        resetSkeletonToDefaultPose();
    }

    public void seek(float timeSeconds) {
        if (activeClip != null) {
            this.currentTimeSeconds = Math.max(0f, Math.min(activeClip.getDurationSeconds(), timeSeconds));
            evaluateAndApplyKeyframes();
        }
    }

    public void update(float deltaTimeSeconds) {
        if (!isPlaying || activeClip == null || skeleton == null) return;

        currentTimeSeconds += deltaTimeSeconds;
        if (currentTimeSeconds > activeClip.getDurationSeconds()) {
            if (activeClip.isLooping()) {
                currentTimeSeconds %= activeClip.getDurationSeconds();
            } else {
                currentTimeSeconds = activeClip.getDurationSeconds();
                isPlaying = false;
            }
        }

        evaluateAndApplyKeyframes();
    }

    /**
     * Resets all skeletal bone nodes to their default transform orientations.
     */
    private void resetSkeletonToDefaultPose() {
        if (skeleton == null) return;
        for (Bone bone : skeleton.getAllBones()) {
            if (bone != null && bone.getLocalTransform() != null) {
                bone.getLocalTransform().reset();
            }
        }
        skeleton.updateWorldTransforms();
    }

    /**
     * Phase 10 Alignment: Evaluates linear keyframe interpolation between timestamps
     * for both joint translation and rotation across active animation tracks.
     */
    private void evaluateAndApplyKeyframes() {
        if (activeClip == null || skeleton == null) return;

        for (AnimationTrack track : activeClip.getTracks()) {
            if (track == null || track.getKeyframes().isEmpty()) continue;

            Bone bone = skeleton.getBoneBySemanticName(track.getBoneSemanticName());
            if (bone == null || bone.getLocalTransform() == null) continue;

            List<Keyframe> keyframes = track.getKeyframes();
            
            // Single keyframe edge case
            if (keyframes.size() == 1) {
                Keyframe kf = keyframes.get(0);
                float[] t = kf.getTranslation();
                float[] r = kf.getRotationDegrees();
                bone.getLocalTransform().setPosition(t[0], t[1], t[2]);
                bone.getLocalTransform().setRotation(r[0], r[1], r[2]);
                continue;
            }

            // Find keyframe interval [prevKf, nextKf]
            Keyframe prevKf = keyframes.get(0);
            Keyframe nextKf = keyframes.get(keyframes.size() - 1);

            for (int i = 0; i < keyframes.size() - 1; i++) {
                if (currentTimeSeconds >= keyframes.get(i).getTimestampSeconds() && 
                    currentTimeSeconds <= keyframes.get(i + 1).getTimestampSeconds()) {
                    prevKf = keyframes.get(i);
                    nextKf = keyframes.get(i + 1);
                    break;
                }
            }

            float t0 = prevKf.getTimestampSeconds();
            float t1 = nextKf.getTimestampSeconds();
            float duration = t1 - t0;
            float factor = duration > 0.0001f ? (currentTimeSeconds - t0) / duration : 0f;
            factor = Math.max(0f, Math.min(1f, factor));

            // Interpolate Translation
            float[] p0 = prevKf.getTranslation();
            float[] p1 = nextKf.getTranslation();
            float tx = p0[0] + factor * (p1[0] - p0[0]);
            float ty = p0[1] + factor * (p1[1] - p0[1]);
            float tz = p0[2] + factor * (p1[2] - p0[2]);

            // Interpolate Rotation Degrees
            float[] r0 = prevKf.getRotationDegrees();
            float[] r1 = nextKf.getRotationDegrees();
            float rx = r0[0] + factor * (r1[0] - r0[0]);
            float ry = r0[1] + factor * (r1[1] - r0[1]);
            float rz = r0[2] + factor * (r1[2] - r0[2]);

            bone.getLocalTransform().setPosition(tx, ty, tz);
            bone.getLocalTransform().setRotation(rx, ry, rz);
        }

        skeleton.updateWorldTransforms();
    }

    public void addClip(AnimationClip clip) {
        if (clip != null && clip.getName() != null) {
            clipLibrary.put(clip.getName().toLowerCase().trim(), clip);
        }
    }

    public AnimationClip getActiveClip() { return activeClip; }
    public float getCurrentTimeSeconds() { return currentTimeSeconds; }
    public boolean isPlaying() { return isPlaying; }
    public Map<String, AnimationClip> getClipLibrary() { return clipLibrary; }
}