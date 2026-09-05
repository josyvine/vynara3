package com.example.character;

import com.example.engine.Transform;

import java.util.ArrayList;
import java.util.List;

public class Bone {
    private String id;
    private String semanticName; // ROOT, PELVIS, SPINE, CHEST, NECK, HEAD, LEFT_ARM, RIGHT_ARM, etc.
    private Transform localTransform;
    private Bone parent;
    private final List<Bone> children = new ArrayList<>();

    public Bone(String id, String semanticName) {
        this.id = id;
        this.semanticName = semanticName;
        this.localTransform = new Transform();
    }

    public String getId() { return id; }
    public String getSemanticName() { return semanticName; }
    public Transform getLocalTransform() { return localTransform; }
    public Bone getParent() { return parent; }
    public List<Bone> getChildren() { return children; }

    public void addChild(Bone child) {
        if (child != null) {
            child.parent = this;
            children.add(child);
        }
    }
}
