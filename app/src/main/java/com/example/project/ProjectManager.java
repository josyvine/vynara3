package com.example.project;

import java.util.ArrayList;
import java.util.List;

public class ProjectManager {
    private final List<Project> projects = new ArrayList<>();

    public ProjectManager() {
        // Phase 14 Alignment: Purged hardcoded mock projects.
        // Projects are now dynamically loaded from device local storage via ProjectSerializer.
    }

    public void setProjects(List<Project> loadedProjects) {
        projects.clear();
        if (loadedProjects != null) {
            projects.addAll(loadedProjects);
        }
    }

    public List<Project> getProjects() { 
        return projects; 
    }

    public void addProject(Project p) {
        if (p != null && !containsProject(p.getId())) {
            projects.add(0, p); // Add newest projects to the top of the list
        }
    }

    public boolean removeProject(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return false;
        }
        return projects.removeIf(p -> p.getId().equals(projectId));
    }

    public Project getProjectById(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return null;
        }
        for (Project p : projects) {
            if (p.getId().equals(projectId)) {
                return p;
            }
        }
        return null;
    }

    public boolean containsProject(String projectId) {
        return getProjectById(projectId) != null;
    }

    public void updateProject(Project updatedProject) {
        if (updatedProject == null || updatedProject.getId() == null) return;
        
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId().equals(updatedProject.getId())) {
                projects.set(i, updatedProject);
                return;
            }
        }
        // If it doesn't exist, add it
        addProject(updatedProject);
    }

    public void clearProjects() {
        projects.clear();
    }
}