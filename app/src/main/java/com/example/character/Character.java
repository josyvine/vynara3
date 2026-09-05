package com.example.character;

import com.example.engine.SceneObject;

public class Character {
    private String id;
    private CharacterSpecification specification;
    private SceneObject sceneObject;
    private Skeleton skeleton;
    private Skin skin;
    private Rig rig;
    private AnimationPlayer animationPlayer;

    public Character(String id, CharacterSpecification spec, SceneObject sceneObject, Skeleton skeleton) {
        this.id = id;
        this.specification = spec != null ? spec : new CharacterSpecification("HUMANOID", "Character");
        this.sceneObject = sceneObject;
        this.skeleton = skeleton;
        
        int vertexCount = (sceneObject != null && sceneObject.getMesh() != null) 
                ? sceneObject.getMesh().getVertexCount() 
                : 100;

        this.skin = new Skin(skeleton, vertexCount);
        this.rig = new Rig(skeleton);
        this.animationPlayer = new AnimationPlayer(skeleton);
    }

    /**
     * Phase 10 Alignment: Updates character skeletal animation playback and 
     * applies joint rotations to bone nodes over time.
     */
    public void update(float deltaTimeSeconds) {
        if (animationPlayer != null && animationPlayer.isPlaying()) {
            animationPlayer.update(deltaTimeSeconds);
        }
    }

    public String getId() { return id; }
    public CharacterSpecification getSpecification() { return specification; }
    public SceneObject getSceneObject() { return sceneObject; }
    public Skeleton getSkeleton() { return skeleton; }
    public Skin getSkin() { return skin; }
    public Rig getRig() { return rig; }
    public AnimationPlayer getAnimationPlayer() { return animationPlayer; }

    public void setSceneObject(SceneObject sceneObject) {
        this.sceneObject = sceneObject;
    }

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
        if (this.rig != null) {
            this.rig = new Rig(skeleton);
        }
        if (this.animationPlayer != null) {
            this.animationPlayer = new AnimationPlayer(skeleton);
        }
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    public void setRig(Rig rig) {
        this.rig = rig;
    }

    public void setAnimationPlayer(AnimationPlayer animationPlayer) {
        this.animationPlayer = animationPlayer;
    }
}