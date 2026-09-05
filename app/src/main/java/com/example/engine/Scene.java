package com.example.engine;

import java.util.ArrayList;
import java.util.List;

public class Scene {
    private String id;
    private String name;
    private final List<SceneObject> objects = new ArrayList<>();

    public Scene(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
    }

    public void addObject(SceneObject obj) {
        if (obj != null) {
            objects.add(obj);
        }
    }

    public void removeObject(String objectId) {
        if (objectId == null) return;
        objects.removeIf(o -> o.getId().equals(objectId));
        for (SceneObject parent : objects) {
            removeChildRecursively(parent, objectId);
        }
    }

    private void removeChildRecursively(SceneObject parent, String objectId) {
        if (parent == null || parent.getChildren() == null) return;
        parent.getChildren().removeIf(child -> child.getId().equals(objectId));
        for (SceneObject child : parent.getChildren()) {
            removeChildRecursively(child, objectId);
        }
    }

    public SceneObject findObjectById(String objectId) {
        if (objectId == null) return null;
        for (SceneObject obj : objects) {
            if (obj.getId().equals(objectId)) return obj;
            SceneObject found = findChildRecursively(obj, objectId);
            if (found != null) return found;
        }
        return null;
    }

    private SceneObject findChildRecursively(SceneObject parent, String objectId) {
        if (parent == null || parent.getChildren() == null) return null;
        for (SceneObject child : parent.getChildren()) {
            if (child.getId().equals(objectId)) return child;
            SceneObject found = findChildRecursively(child, objectId);
            if (found != null) return found;
        }
        return null;
    }

    public SceneObject findObjectByName(String name) {
        if (name == null) return null;
        for (SceneObject obj : objects) {
            if (name.equalsIgnoreCase(obj.getName())) return obj;
            SceneObject found = findChildByNameRecursively(obj, name);
            if (found != null) return found;
        }
        return null;
    }

    private SceneObject findChildByNameRecursively(SceneObject parent, String name) {
        if (parent == null || parent.getChildren() == null) return null;
        for (SceneObject child : parent.getChildren()) {
            if (name.equalsIgnoreCase(child.getName())) return child;
            SceneObject found = findChildByNameRecursively(child, name);
            if (found != null) return found;
        }
        return null;
    }

    public List<SceneObject> getObjects() { return objects; }

    /**
     * Returns a flat list containing all root scene objects and nested children nodes.
     */
    public List<SceneObject> getFlatObjectList() {
        List<SceneObject> flatList = new ArrayList<>();
        for (SceneObject rootObj : objects) {
            collectFlatListRecursively(rootObj, flatList);
        }
        return flatList;
    }

    private void collectFlatListRecursively(SceneObject current, List<SceneObject> list) {
        if (current == null) return;
        list.add(current);
        if (current.getChildren() != null) {
            for (SceneObject child : current.getChildren()) {
                collectFlatListRecursively(child, list);
            }
        }
    }

    public int getTotalTriangleCount() {
        int count = 0;
        for (SceneObject obj : getFlatObjectList()) {
            if (obj.getMesh() != null) {
                count += obj.getMesh().getTriangleCount();
            }
        }
        return count;
    }

    public int getTotalVertexCount() {
        int count = 0;
        for (SceneObject obj : getFlatObjectList()) {
            if (obj.getMesh() != null) {
                count += obj.getMesh().getVertexCount();
            }
        }
        return count;
    }
}