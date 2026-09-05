package com.example.runtime;

import com.example.engine.Scene;

import java.util.ArrayList;
import java.util.List;

public class UndoManager {
    private final ProjectRuntime runtime;
    private final List<SceneSnapshot> undoStack = new ArrayList<>();
    private static final int MAX_UNDO_STEPS = 20;

    public UndoManager(ProjectRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Phase 13 Alignment: Pushes a stable pre-operation scene snapshot onto the undo stack.
     * Caps maximum stack depth to 20 steps to prevent Keystore or heap memory exhaustion on large scenes.
     */
    public void pushSnapshot(SceneSnapshot snapshot) {
        if (snapshot == null) return;

        if (undoStack.size() >= MAX_UNDO_STEPS) {
            undoStack.remove(0); // Prune oldest snapshot to prevent memory bloat
        }
        undoStack.add(snapshot);
    }

    /**
     * Reverts the active scene graph back to the previous stable state snapshot, 
     * caching the current state inside the Redo manager.
     */
    public boolean undo() {
        if (undoStack.isEmpty() || runtime == null) {
            return false;
        }

        Scene activeScene = runtime.getEngine().getSceneManager().getActiveScene();
        if (activeScene == null) return false;

        // Capture current state to enable Redo tracking before reverting
        SceneSnapshot currentSceneState = new SceneSnapshot(activeScene);
        runtime.getRedoManager().pushSnapshot(currentSceneState);

        // Pop top pre-operation snapshot off the stack and restore
        SceneSnapshot previousState = undoStack.remove(undoStack.size() - 1);
        previousState.restore(activeScene);
        
        // Sync selection boundaries
        runtime.getEngine().getSceneManager().clearSelection();
        
        return true;
    }

    public void clear() {
        undoStack.clear();
    }

    public int getStackSize() {
        return undoStack.size();
    }
}