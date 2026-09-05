package com.example.runtime;

import com.example.engine.Scene;
import com.example.engine.SceneObject;

import java.util.ArrayList;
import java.util.List;

public class SceneSnapshot {
    private final List<SceneObject> snapshottedRootNodes = new ArrayList<>();
    private final long snapshotTimeMs;

    /**
     * Phase 13 Alignment: Deep copies the entire active scene graph hierarchy 
     * at a specific moment in time to act as a stable, isolated restore point.
     */
    public SceneSnapshot(Scene sourceScene) {
        this.snapshotTimeMs = System.currentTimeMillis();
        if (sourceScene != null) {
            for (SceneObject root : sourceScene.getObjects()) {
                if (root != null) {
                    String cloneId = root.getId();
                    String cloneName = root.getName();
                    // Deep clone the node tree and add to snapshot collection
                    snapshottedRootNodes.add(root.cloneNode(cloneId, cloneName));
                }
            }
        }
    }

    /**
     * Restores the target scene's objects back to the exact snapshotted states.
     */
    public void restore(Scene targetScene) {
        if (targetScene == null) return;

        targetScene.getObjects().clear();
        for (SceneObject snapshottedRoot : snapshottedRootNodes) {
            if (snapshottedRoot != null) {
                String restoreId = snapshottedRoot.getId();
                String restoreName = snapshottedRoot.getName();
                // Deep clone nodes back into the active scene list
                targetScene.addObject(snapshottedRoot.cloneNode(restoreId, restoreName));
            }
        }
    }

    public long getSnapshotTimeMs() {
        return snapshotTimeMs;
    }

    public int getObjectCount() {
        return snapshottedRootNodes.size();
    }
}