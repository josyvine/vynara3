package com.example.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TaskGraph {
    private final Map<String, TaskNode> nodes = new HashMap<>();

    public void addTask(TaskNode node) {
        if (node != null) {
            nodes.put(node.getId(), node);
        }
    }

    public void removeTask(String id) {
        if (id == null) return;
        nodes.remove(id);
        for (TaskNode node : nodes.values()) {
            node.getDependencyTaskIds().remove(id);
        }
    }

    public TaskNode getNode(String id) {
        return nodes.get(id);
    }

    public List<TaskNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    public boolean hasDependencyCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String id : nodes.keySet()) {
            if (isCyclicUtil(id, visited, recStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCyclicUtil(String id, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(id)) return true;
        if (visited.contains(id)) return false;

        visited.add(id);
        recStack.add(id);

        TaskNode node = nodes.get(id);
        if (node != null) {
            for (String depId : node.getDependencyTaskIds()) {
                if (isCyclicUtil(depId, visited, recStack)) {
                    return true;
                }
            }
        }

        recStack.remove(id);
        return false;
    }

    public List<TaskNode> getReadyTasks() {
        List<TaskNode> ready = new ArrayList<>();
        for (TaskNode node : nodes.values()) {
            if (node.getStatus() == TaskNode.Status.QUEUED || node.getStatus() == TaskNode.Status.WAITING) {
                boolean allDepsCompleted = true;
                for (String depId : node.getDependencyTaskIds()) {
                    TaskNode dep = nodes.get(depId);
                    if (dep == null || dep.getStatus() != TaskNode.Status.COMPLETED) {
                        allDepsCompleted = false;
                        break;
                    }
                }
                if (allDepsCompleted) {
                    ready.add(node);
                }
            }
        }
        return ready;
    }

    /**
     * Phase 12 Alignment: Returns tasks ordered according to topological DAG dependencies.
     */
    public List<TaskNode> getTopologicallySortedTasks() {
        List<TaskNode> result = new ArrayList<>();
        if (hasDependencyCycles()) return result;

        Map<String, Integer> inDegree = new HashMap<>();
        for (String id : nodes.keySet()) {
            inDegree.put(id, 0);
        }

        for (TaskNode node : nodes.values()) {
            for (String depId : node.getDependencyTaskIds()) {
                if (inDegree.containsKey(depId)) {
                    inDegree.put(depId, inDegree.get(depId) + 1);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String u = queue.poll();
            TaskNode node = nodes.get(u);
            if (node != null) {
                result.add(node);
                for (String depId : node.getDependencyTaskIds()) {
                    if (inDegree.containsKey(depId)) {
                        inDegree.put(depId, inDegree.get(depId) - 1);
                        if (inDegree.get(depId) == 0) {
                            queue.add(depId);
                        }
                    }
                }
            }
        }

        return result;
    }

    public boolean isAllCompleted() {
        for (TaskNode node : nodes.values()) {
            if (node.getStatus() != TaskNode.Status.COMPLETED && node.getStatus() != TaskNode.Status.SKIPPED) {
                return false;
            }
        }
        return true;
    }

    public boolean hasFailedTasks() {
        for (TaskNode node : nodes.values()) {
            if (node.getStatus() == TaskNode.Status.FAILED) {
                return true;
            }
        }
        return false;
    }

    public void resetGraphToQueued() {
        for (TaskNode node : nodes.values()) {
            node.setStatus(TaskNode.Status.QUEUED);
            node.setProgressPercent(0);
            node.setErrorMessage(null);
        }
    }

    public int getCompletedCount() {
        int count = 0;
        for (TaskNode node : nodes.values()) {
            if (node.getStatus() == TaskNode.Status.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    public int getFailedCount() {
        int count = 0;
        for (TaskNode node : nodes.values()) {
            if (node.getStatus() == TaskNode.Status.FAILED) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount() {
        return nodes.size();
    }

    public void clear() {
        nodes.clear();
    }
}