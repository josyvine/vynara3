package com.example.runtime;

import com.example.engine.Scene;

public class TransactionManager {
    private final ProjectRuntime runtime;
    private SceneSnapshot activeTransactionSnapshot;
    private String activeTransactionLabel;
    private boolean isInsideTransaction = false;

    public TransactionManager(ProjectRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Phase 13 Alignment: Captures a complete deep-copy snapshot of the active 
     * scene graph before tool executions begin.
     */
    public void beginTransaction(String label) {
        if (isInsideTransaction) {
            // Nested transaction fallback: commit active one first
            commitTransaction();
        }

        this.activeTransactionLabel = label != null ? label : "AI Tool Operation";
        Scene activeScene = runtime.getEngine().getSceneManager().getActiveScene();
        
        if (activeScene != null) {
            this.activeTransactionSnapshot = new SceneSnapshot(activeScene);
            this.isInsideTransaction = true;
        }
    }

    /**
     * Commits the pre-operation state snapshot onto the Undo manager stack, 
     * finalizing active scene graph modifications.
     */
    public void commitTransaction() {
        if (!isInsideTransaction || activeTransactionSnapshot == null) {
            return;
        }

        runtime.getUndoManager().pushSnapshot(activeTransactionSnapshot);
        runtime.getRedoManager().clear(); // Clear redo stack on new operation commit
        
        this.activeTransactionSnapshot = null;
        this.activeTransactionLabel = null;
        this.isInsideTransaction = false;
    }

    /**
     * Phase 13 Alignment: Restores the scene graph directly back to the 
     * pre-transaction snapshot state if an error or validation failure occurs.
     */
    public void rollbackTransaction() {
        if (!isInsideTransaction || activeTransactionSnapshot == null) {
            return;
        }

        Scene activeScene = runtime.getEngine().getSceneManager().getActiveScene();
        if (activeScene != null) {
            activeTransactionSnapshot.restore(activeScene);
        }

        this.activeTransactionSnapshot = null;
        this.activeTransactionLabel = null;
        this.isInsideTransaction = false;
    }

    public boolean isInsideTransaction() {
        return isInsideTransaction;
    }

    public String getActiveTransactionLabel() {
        return activeTransactionLabel;
    }
}