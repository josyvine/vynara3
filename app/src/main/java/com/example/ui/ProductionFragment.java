package com.example.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.MainActivity;
import com.example.R;
import com.example.ai.AIProductionController;
import com.example.ai.GeminiApiClient;
import com.example.tasks.ExecutionEngine;
import com.example.tasks.ProductionPlan;
import com.example.tasks.TaskGraph;
import com.example.tasks.TaskNode;

import java.util.ArrayList;
import java.util.List;

public class ProductionFragment extends Fragment {

    private static final String ARG_PROMPT = "arg_prompt";
    private static final String ARG_STYLE = "arg_style";
    private static final String ARG_ENGINE = "arg_engine";
    private static final String ARG_REF_IMAGES = "arg_ref_images";

    private TextView tvProjectTitle;
    private TextView tvStatus;
    private TextView tvTaskCounter;
    private TextView tvProgressPercent;
    private ProgressBar progressBar;
    private RecyclerView rvTasks;
    private TaskNodeAdapter adapter;

    private String prompt = "3D Asset Creation";
    private String style = "Photorealistic";
    private String targetEngine = "OpenGL ES / GLTF";
    private ArrayList<String> referenceImageUris = new ArrayList<>();

    private AIProductionController controller;
    private ProductionPlan activePlan;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static ProductionFragment newInstance(String prompt, String style, String targetEngine, List<String> referenceImageUris) {
        ProductionFragment fragment = new ProductionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROMPT, prompt);
        args.putString(ARG_STYLE, style);
        args.putString(ARG_ENGINE, targetEngine);
        args.putStringArrayList(ARG_REF_IMAGES, referenceImageUris != null ? new ArrayList<>(referenceImageUris) : new ArrayList<>());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            prompt = getArguments().getString(ARG_PROMPT, prompt);
            style = getArguments().getString(ARG_STYLE, style);
            targetEngine = getArguments().getString(ARG_ENGINE, targetEngine);
            referenceImageUris = getArguments().getStringArrayList(ARG_REF_IMAGES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_production, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProjectTitle = view.findViewById(R.id.tv_project_title);
        tvStatus = view.findViewById(R.id.tv_current_task_status);
        tvTaskCounter = view.findViewById(R.id.tv_task_counter);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        progressBar = view.findViewById(R.id.progress_production);
        rvTasks = view.findViewById(R.id.rv_tasks);

        tvProjectTitle.setText("Creating: " + prompt);

        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskNodeAdapter();
        rvTasks.setAdapter(adapter);

        // Phase 1 Alignment: Initialize Controller bound to shared ProjectRuntime
        controller = new AIProductionController(requireContext());

        // Set UI to loading state while asynchronously requesting dynamic tool-planning layout from Gemini
        progressBar.setIndeterminate(true);
        tvStatus.setText("AI: Devising 3D production plan with Gemini...");

        controller.generatePlanWithGemini(prompt, style, targetEngine, referenceImageUris, new GeminiApiClient.ApiCallback<ProductionPlan>() {
            @Override
            public void onSuccess(ProductionPlan plan) {
                handler.post(() -> {
                    activePlan = plan;
                    progressBar.setIndeterminate(false);
                    if (activePlan != null && activePlan.getTaskGraph() != null) {
                        adapter.setTasks(activePlan.getTaskGraph().getAllNodes());
                        startRealExecutionPipeline();
                    } else {
                        tvStatus.setText("AI Error: Generated production plan was empty.");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                handler.post(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(0);
                    tvStatus.setText("AI Error: Generation Failed.");
                    Toast.makeText(getContext(), "AI Workflow Halted: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });

        Button btnPause = view.findViewById(R.id.btn_pause_production);
        if (btnPause != null) {
            btnPause.setOnClickListener(v -> {
                ExecutionEngine engine = controller.getExecutionEngine();
                if (engine.isPaused()) {
                    engine.resume();
                    btnPause.setText("Pause");
                    Toast.makeText(getContext(), "Pipeline Resumed", Toast.LENGTH_SHORT).show();
                } else {
                    engine.pause();
                    btnPause.setText("Resume");
                    Toast.makeText(getContext(), "Pipeline Paused", Toast.LENGTH_SHORT).show();
                }
            });
        }

        Button btnCancel = view.findViewById(R.id.btn_cancel_production);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                controller.getExecutionEngine().cancel();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToCreate();
                }
            });
        }

        Button btnViewStudio = view.findViewById(R.id.btn_open_in_studio);
        if (btnViewStudio != null) {
            btnViewStudio.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToStudio();
                }
            });
        }
    }

    /**
     * Phase 12 & 21 Alignment: Executes the true background-threaded tool execution 
     * pipeline on the shared runtime instead of mock timer updates.
     */
    private void startRealExecutionPipeline() {
        if (activePlan == null || activePlan.getTaskGraph() == null) {
            tvStatus.setText("AI Error: Failed to compile production plan.");
            return;
        }

        controller.executeCurrentPlan(new ExecutionEngine.ExecutionCallback() {
            @Override
            public void onTaskUpdated(TaskNode node, TaskGraph graph) {
                // Post updates to the Main thread safely
                handler.post(() -> {
                    if (node != null) {
                        tvStatus.setText("Executing: " + node.getTitle());
                        adapter.setTasks(graph.getAllNodes());
                    }
                    int completed = graph.getCompletedCount();
                    int total = graph.getTotalCount();
                    int percent = (int) (((float) completed / total) * 100);
                    
                    progressBar.setProgress(percent);
                    tvProgressPercent.setText(percent + "%");
                    tvTaskCounter.setText("Tasks: " + completed + " / " + total);
                });
            }

            @Override
            public void onGraphCompleted(TaskGraph graph) {
                handler.post(() -> {
                    tvStatus.setText("AI Status: All Tasks Completed Successfully! ✦");
                    progressBar.setProgress(100);
                    tvProgressPercent.setText("100%");
                    tvTaskCounter.setText("Tasks: " + graph.getTotalCount() + " / " + graph.getTotalCount());
                    Toast.makeText(getContext(), "3D Generation Complete!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                handler.post(() -> {
                    tvStatus.setText("AI Error: Pipeline Halted.");
                    Toast.makeText(getContext(), "Workflow halted: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}