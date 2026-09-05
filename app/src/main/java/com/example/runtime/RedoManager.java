package com.example.runtime;

import com.example.engine.Scene;

import java.util.ArrayList;
import java.util.List;

public class RedoManager {
    private final ProjectRuntime runtime;
    private final List<SceneSnapshot> redoStack = new ArrayList<>();
    private static final int MAX_REDO_STEPS = 20;

    public RedoManager(ProjectRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Phase 13 Alignment: Pushes an undone scene state snapshot onto the redo stack.
     */
    public void pushSnapshot(SceneSnapshot snapshot) {
        if (snapshot == null) return;

        if (redoStack.size() >= MAX_REDO_STEPS) {
            redoStack.remove(0); // Prune oldest snapshot to prevent memory bloat
        }
        redoStack.add(snapshot);
    }

    /**
     * Re-applies the last undone scene operation, caching the current pre-redo state
     * back onto the Undo manager.
     */
    public boolean redo() {
        if (redoStack.isEmpty() || runtime == null) {
            return false;
        }

        Scene activeScene = runtime.getEngine().getSceneManager().getActiveScene();
        if (activeScene == null) return false;

        // Capture current state to enable Undo tracking before re-applying
        SceneSnapshot currentSceneState = new SceneSnapshot(activeScene);
        runtime.getUndoManager().pushSnapshot(currentSceneState);

        // Pop top undone snapshot off the stack and restore
        SceneSnapshot futureState = redoStack.remove(redoStack.size() - 1);
        futureState.restore(activeScene);

        // Sync selection boundaries
        runtime.getEngine().getSceneManager().clearSelection();

        return true;
    }

    public void clear() {
        redoStack.clear();
    }

    public int getStackSize() {
        return redoStack.size();
    }
}