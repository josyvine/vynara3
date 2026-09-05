package com.example.runtime;

import com.example.engine.SceneObject;

import java.util.ArrayList;
import java.util.List;

public class SceneGraph {
    private final List<SceneObject> rootNodes = new ArrayList<>();

    public SceneGraph() {
    }

    public void addNode(SceneObject node) {
        if (node != null && !rootNodes.contains(node)) {
            rootNodes.add(node);
        }
    }

    public void removeNode(String id) {
        if (id == null) return;
        rootNodes.removeIf(n -> n.getId().equals(id));
        for (SceneObject root : rootNodes) {
            removeNodeRecursively(root, id);
        }
    }

    private void removeNodeRecursively(SceneObject parent, String id) {
        if (parent == null || parent.getChildren() == null) return;
        parent.getChildren().removeIf(child -> child.getId().equals(id));
        for (SceneObject child : parent.getChildren()) {
            removeNodeRecursively(child, id);
        }
    }

    public boolean parentNodes(SceneObject child, SceneObject parent) {
        if (child == null || parent == null || child.equals(parent)) return false;

        // Remove child from previous structural link
        if (child.getParent() != null) {
            child.getParent().removeChild(child);
        } else {
            rootNodes.remove(child);
        }

        parent.addChild(child);
        updateWorldTransforms();
        return true;
    }

    public boolean unparentNode(SceneObject child) {
        if (child == null || child.getParent() == null) return false;

        SceneObject parent = child.getParent();
        parent.removeChild(child);
        addNode(child);
        updateWorldTransforms();
        return true;
    }

    public SceneObject findNodeById(String id) {
        if (id == null) return null;
        for (SceneObject root : rootNodes) {
            if (root.getId().equals(id)) return root;
            SceneObject found = findNodeByIdRecursively(root, id);
            if (found != null) return found;
        }
        return null;
    }

    private SceneObject findNodeByIdRecursively(SceneObject parent, String id) {
        if (parent == null) return null;
        for (SceneObject child : parent.getChildren()) {
            if (child.getId().equals(id)) return child;
            SceneObject found = findNodeByIdRecursively(child, id);
            if (found != null) return found;
        }
        return null;
    }

    public SceneObject findNodeByName(String name) {
        if (name == null) return null;
        for (SceneObject root : rootNodes) {
            if (name.equalsIgnoreCase(root.getName())) return root;
            SceneObject found = findNodeByNameRecursively(root, name);
            if (found != null) return found;
        }
        return null;
    }

    private SceneObject findNodeByNameRecursively(SceneObject parent, String name) {
        if (parent == null) return null;
        for (SceneObject child : parent.getChildren()) {
            if (name.equalsIgnoreCase(child.getName())) return child;
            SceneObject found = findNodeByNameRecursively(child, name);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Phase 15 Alignment: Recalculates world matrices recursively starting
     * from root nodes and propagating down to nested children.
     */
    public void updateWorldTransforms() {
        for (SceneObject root : rootNodes) {
            updateTransformsRecursively(root, null);
        }
    }

    private void updateTransformsRecursively(SceneObject node, float[] parentWorldMatrix) {
        if (node == null) return;
        float[] worldMat = node.getTransform().getWorldMatrix(parentWorldMatrix);
        for (SceneObject child : node.getChildren()) {
            updateTransformsRecursively(child, worldMat);
        }
    }

    public List<SceneObject> getFlatNodeList() {
        List<SceneObject> flatList = new ArrayList<>();
        for (SceneObject root : rootNodes) {
            flattenRecursively(root, flatList);
        }
        return flatList;
    }

    private void flattenRecursively(SceneObject current, List<SceneObject> list) {
        if (current == null) return;
        list.add(current);
        for (SceneObject child : current.getChildren()) {
            flattenRecursively(child, list);
        }
    }

    public List<SceneObject> getRootNodes() { return rootNodes; }

    public void clear() {
        rootNodes.clear();
    }
}