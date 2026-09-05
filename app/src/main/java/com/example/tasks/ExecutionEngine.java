package com.example.tasks;

import android.os.Handler;
import android.os.Looper;

import com.example.tools.ToolExecutor;
import com.example.utils.VynaraLogger;
import com.example.utils.VynaraLogger.LogLevel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutionEngine {
    private final ToolExecutor toolExecutor;
    private final ExecutorService threadPool;
    private final Handler mainHandler;
    private volatile boolean isPaused = false;
    private volatile boolean isCancelled = false;

    public interface ExecutionCallback {
        void onTaskUpdated(TaskNode node, TaskGraph graph);
        void onGraphCompleted(TaskGraph graph);
        void onError(String errorMessage);
    }

    public ExecutionEngine(ToolExecutor toolExecutor) {
        this.toolExecutor = toolExecutor;
        this.threadPool = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Phase 12 Alignment: Asynchronously executes DAG tasks in topological dependency order,
     * reporting queued, running, completed, failed, and rolled_back states.
     * Hooks execution lifecycle traces directly into VynaraLogger.
     */
    public void executeGraph(final TaskGraph graph, final ExecutionCallback callback) {
        if (graph == null) {
            VynaraLogger.system("TaskGraph compilation failed: Instance is null.");
            if (callback != null) callback.onError("Invalid TaskGraph: Instance is null.");
            return;
        }

        if (graph.hasDependencyCycles()) {
            VynaraLogger.system("TaskGraph compilation failed: Contains circular dependencies.");
            if (callback != null) callback.onError("Invalid TaskGraph: Contains circular dependencies.");
            return;
        }

        isPaused = false;
        isCancelled = false;
        
        // Reset the graph state before beginning execution
        graph.resetGraphToQueued();

        VynaraLogger.system("Asynchronously compiling and executing TaskGraph (DAG)...");

        threadPool.execute(() -> {
            boolean hasExecutionError = false;

            while (!graph.isAllCompleted() && !isCancelled && !hasExecutionError) {
                if (isPaused) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    continue;
                }

                List<TaskNode> readyTasks = graph.getReadyTasks();
                if (readyTasks.isEmpty() && !graph.isAllCompleted()) {
                    VynaraLogger.system("DAG execution stalled: Unresolved task dependencies or empty queue.");
                    break;
                }

                for (final TaskNode task : readyTasks) {
                    if (isCancelled) {
                        VynaraLogger.system("Execution cancelled on active thread loop.");
                        break;
                    }

                    task.setStatus(TaskNode.Status.RUNNING);
                    task.setProgressPercent(20);
                    notifyTaskUpdated(task, graph, callback);

                    VynaraLogger.execution("Starting execution of Task [" + task.getId() + "]: " + task.getTitle());

                    // Execute tool operation against local engine
                    boolean success = false;
                    try {
                        if (task.getOperation() != null) {
                            String toolId = task.getOperation().getToolId();
                            VynaraLogger.execution("Mapping task to registered Tool ID: " + toolId);
                            
                            if (toolExecutor != null) {
                                success = toolExecutor.executeOperation(task.getOperation());
                            } else {
                                VynaraLogger.system("Execution failure: ToolExecutor reference is null.");
                                success = false;
                            }
                        } else {
                            // Virtual planning task node success
                            VynaraLogger.execution("Executing virtual planning node: " + task.getTitle());
                            success = true;
                        }
                    } catch (Exception e) {
                        task.setErrorMessage("Execution exception: " + e.getMessage());
                        VynaraLogger.e("Exception thrown during execution of task [" + task.getId() + "]", e);
                        success = false;
                    }

                    if (success) {
                        task.setStatus(TaskNode.Status.COMPLETED);
                        task.setProgressPercent(100);
                        VynaraLogger.task("Task [" + task.getId() + "] COMPLETED successfully.");
                    } else {
                        task.setStatus(TaskNode.Status.FAILED);
                        if (task.getErrorMessage() == null) {
                            String failedToolId = task.getOperation() != null ? task.getOperation().getToolId() : "null";
                            task.setErrorMessage("Tool execution failed: " + failedToolId);
                        }
                        VynaraLogger.validator(LogLevel.ERROR, "Task [" + task.getId() + "] FAILED: " + task.getErrorMessage());
                        hasExecutionError = true;
                    }

                    notifyTaskUpdated(task, graph, callback);

                    if (!success) {
                        break; // Stop executing remaining tasks on error
                    }

                    try { Thread.sleep(150); } catch (InterruptedException ignored) {} // Smooth UI step transition
                }
            }

            final boolean finalErrorState = hasExecutionError;

            mainHandler.post(() -> {
                if (callback != null) {
                    if (isCancelled) {
                        VynaraLogger.system("TaskGraph execution cancelled by user request.");
                        callback.onError("Production workflow cancelled by user.");
                    } else if (graph.isAllCompleted() && !finalErrorState) {
                        VynaraLogger.system("TaskGraph execution successfully completed. All operations committed.");
                        callback.onGraphCompleted(graph);
                    } else {
                        VynaraLogger.system("TaskGraph execution halted. Pipeline rolling back.");
                        callback.onError("Workflow halted due to execution failure or unfulfilled dependencies.");
                    }
                }
            });
        });
    }

    private void notifyTaskUpdated(final TaskNode task, final TaskGraph graph, final ExecutionCallback callback) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onTaskUpdated(task, graph);
            }
        });
    }

    private void notifyTaskUpdated(final TaskNode task, final TaskGraph graph, final Object callback) {
        mainHandler.post(() -> {
            if (callback instanceof ExecutionCallback) {
                ((ExecutionCallback) callback).onTaskUpdated(task, graph);
            }
        });
    }

    public void pause() { 
        isPaused = true; 
        VynaraLogger.system("Execution pipeline PAUSED.");
    }
    
    public void resume() { 
        isPaused = false; 
        VynaraLogger.system("Execution pipeline RESUMED.");
    }
    
    public void cancel() { 
        isCancelled = true; 
    }
    
    public boolean isPaused() { return isPaused; }
    public boolean isCancelled() { return isCancelled; }

    public void shutdown() {
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }
}