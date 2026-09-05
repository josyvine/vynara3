package com.example.character;

import com.example.engine.SceneObject;

public class AttachmentSocket {
    private String name; // RIGHT_HAND_SOCKET, HEAD_SOCKET, BACK_SOCKET
    private Bone parentBone;
    private SceneObject attachedAsset;

    public AttachmentSocket(String name, Bone parentBone) {
        this.name = name;
        this.parentBone = parentBone;
    }

    public String getName() { return name; }
    public Bone getParentBone() { return parentBone; }
    public SceneObject getAttachedAsset() { return attachedAsset; }

    public void attachAsset(SceneObject asset) {
        this.attachedAsset = asset;
    }
}
