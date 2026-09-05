package com.example.engine;

import java.util.ArrayList;
import java.util.List;

public class SceneObject {
    private String id;
    private String name;
    private String semanticType; // PRIMITIVE, STRUCTURE, HOUSE, SOFA, CHARACTER, CREATURE, LIGHT, CAMERA
    private Transform transform;
    private Mesh mesh;
    private Material material;
    private boolean isVisible = true;
    private boolean isSelected = false;

    private SceneObject parent;
    private final List<SceneObject> children = new ArrayList<>();

    public SceneObject(String id, String name, String semanticType, Mesh mesh, Material material) {
        this.id = id != null ? id : "obj_" + System.currentTimeMillis();
        this.name = name != null ? name : "Scene Node";
        this.semanticType = semanticType != null ? semanticType : "PRIMITIVE";
        this.mesh = mesh;
        this.material = material;
        this.transform = new Transform();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSemanticType() { return semanticType; }
    public Transform getTransform() { return transform; }
    public Mesh getMesh() { return mesh; }
    public Material getMaterial() { return material; }
    public boolean isVisible() { return isVisible; }
    public boolean isSelected() { return isSelected; }

    public void setName(String name) { this.name = name; }
    public void setSemanticType(String semanticType) { this.semanticType = semanticType; }
    public void setMesh(Mesh mesh) { this.mesh = mesh; }
    public void setMaterial(Material material) { this.material = material; }
    public void setVisible(boolean visible) { isVisible = visible; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public void setTransform(Transform transform) {
        if (transform != null) {
            this.transform = transform;
        }
    }

    public void addChild(SceneObject child) {
        if (child != null && !children.contains(child)) {
            child.parent = this;
            children.add(child);
        }
    }

    public void removeChild(SceneObject child) {
        if (child != null) {
            child.parent = null;
            children.remove(child);
        }
    }

    public List<SceneObject> getChildren() { return children; }
    public SceneObject getParent() { return parent; }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Phase 15 Alignment: Deep copies this scene object node and recursively
     * duplicates its children sub-graph.
     */
    public SceneObject cloneNode(String newId, String newName) {
        // Deep copy PBR material if present
        Material clonedMat = null;
        if (this.material != null) {
            clonedMat = this.material.cloneMaterial(
                    this.material.getId() + "_copy_" + System.currentTimeMillis(),
                    this.material.getName() + " (Copy)"
            );
        }

        // Share Mesh reference but isolate structural references
        SceneObject copy = new SceneObject(newId, newName, this.semanticType, this.mesh, clonedMat);
        copy.setVisible(this.isVisible);
        
        if (this.transform != null) {
            copy.getTransform().setPosition(this.transform.getPx(), this.transform.getPy(), this.transform.getPz());
            copy.getTransform().setRotation(this.transform.getRx(), this.transform.getRy(), this.transform.getRz());
            copy.getTransform().setScale(this.transform.getSx(), this.transform.getSy(), this.transform.getSz());
        }

        for (SceneObject child : children) {
            if (child != null) {
                String childNewId = child.getId() + "_copy_" + System.currentTimeMillis();
                String childNewName = child.getName() + " (Copy)";
                copy.addChild(child.cloneNode(childNewId, childNewName));
            }
        }

        return copy;
    }
}