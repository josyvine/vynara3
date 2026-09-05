package com.example.ui;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.MainActivity;
import com.example.R;
import com.example.character.Character;
import com.example.engine.Camera;
import com.example.engine.GLTFImporter;
import com.example.engine.Material;
import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.engine.StudioGLRenderer;
import com.example.engine.ThreeDEngine;
import com.example.export.GLTFExporter;
import com.example.runtime.ProjectRuntime;
import com.example.utils.VynaraLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class StudioFragment extends Fragment {

    private GLSurfaceView glSurfaceView;
    private StudioGLRenderer renderer;
    private ProjectRuntime runtime;
    private ThreeDEngine engine;
    
    private TextView tvStats;
    private TextView tvSelectedInfo;
    private TextView tvAnimTime;
    private SeekBar seekbarTimeline;
    private ImageButton btnAnimPlay;
    private boolean isPlaying = false;
    private android.os.Handler animHandler;
    private Runnable animRunnable;
    private ScaleGestureDetector scaleGestureDetector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_studio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Phase 1 Alignment: Fetch the single, unified shared project runtime instance
        if (getActivity() instanceof MainActivity) {
            runtime = ((MainActivity) getActivity()).getProjectRuntime();
        } else {
            runtime = ProjectRuntime.getInstance(requireContext());
        }

        engine = runtime.getEngine();
        animHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        glSurfaceView = view.findViewById(R.id.gl_surface_view);
        tvStats = view.findViewById(R.id.tv_studio_poly_stats);
        tvSelectedInfo = view.findViewById(R.id.tv_selected_object_info);
        tvAnimTime = view.findViewById(R.id.tv_anim_time);
        seekbarTimeline = view.findViewById(R.id.seekbar_timeline);
        btnAnimPlay = view.findViewById(R.id.btn_anim_play);

        // Setup OpenGL ES 2.0 Viewport Renderer
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new StudioGLRenderer(engine.getSceneManager(), engine.getCameraManager(), engine.getLightManager());
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Enable 360-degree Touch Viewport Camera Orbit Navigation with Pinch-to-Zoom
        setupViewportTouchOrbitGesture();

        updateStudioStatsUI();

        // Phase 13 Alignment: Undo & Redo transaction history
        View btnUndo = view.findViewById(R.id.btn_undo);
        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> {
                if (runtime.getUndoManager().undo()) {
                    updateStudioStatsUI();
                    Toast.makeText(getContext(), "Undo Successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Nothing to undo", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnRedo = view.findViewById(R.id.btn_redo);
        if (btnRedo != null) {
            btnRedo.setOnClickListener(v -> {
                if (runtime.getRedoManager().redo()) {
                    updateStudioStatsUI();
                    Toast.makeText(getContext(), "Redo Successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Nothing to redo", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Phase 18 Alignment: Real GLTF scene exporter
        View btnExport = view.findViewById(R.id.btn_export_gltf);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportActiveSceneToLocalGltf());
        }

        // Viewport Transform Tool Controls
        View btnSelect = view.findViewById(R.id.btn_tool_select);
        if (btnSelect != null) {
            btnSelect.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    tvSelectedInfo.setText("Selected: " + selected.getName() + " (" + selected.getSemanticType() + ")");
                } else {
                    List<SceneObject> objs = engine.getSceneManager().getAllObjects();
                    if (!objs.isEmpty()) {
                        engine.getSceneManager().selectObject(objs.get(0));
                        tvSelectedInfo.setText("Selected: " + objs.get(0).getName());
                    } else {
                        tvSelectedInfo.setText("No object selected");
                    }
                }
            });
        }

        View btnMove = view.findViewById(R.id.btn_tool_move);
        if (btnMove != null) {
            btnMove.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    runtime.getTransactionManager().beginTransaction("Translate Object");
                    selected.getTransform().translate(0.5f, 0f, 0f);
                    runtime.getTransactionManager().commitTransaction();
                    Toast.makeText(getContext(), "Translated selected object (+0.5 X)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select an object first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnRotate = view.findViewById(R.id.btn_tool_rotate);
        if (btnRotate != null) {
            btnRotate.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    runtime.getTransactionManager().beginTransaction("Rotate Object");
                    selected.getTransform().rotate(0f, 15f, 0f);
                    runtime.getTransactionManager().commitTransaction();
                    Toast.makeText(getContext(), "Rotated selected object (+15 deg Yaw)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select an object first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnScale = view.findViewById(R.id.btn_tool_scale);
        if (btnScale != null) {
            btnScale.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    runtime.getTransactionManager().beginTransaction("Scale Object");
                    selected.getTransform().scaleBy(1.1f, 1.1f, 1.1f);
                    runtime.getTransactionManager().commitTransaction();
                    Toast.makeText(getContext(), "Scaled selected object (+10% Uniform)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select an object first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnHierarchy = view.findViewById(R.id.btn_tool_hierarchy);
        if (btnHierarchy != null) {
            btnHierarchy.setOnClickListener(v -> {
                int totalObjects = engine.getSceneManager().getAllObjects().size();
                int totalLights = engine.getLightManager().getLights().size();
                Toast.makeText(getContext(), "Scene Graph: " + totalObjects + " Nodes, " + totalLights + " Lights, 1 Camera", Toast.LENGTH_LONG).show();
            });
        }

        // Timeline and animation loop setup
        animRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    for (Character c : runtime.getCharacterManager().getCharacterMap().values()) {
                        if (c.getAnimationPlayer() != null && c.getAnimationPlayer().isPlaying()) {
                            c.getAnimationPlayer().update(0.033f);
                            float seconds = c.getAnimationPlayer().getCurrentTimeSeconds();
                            if (seekbarTimeline != null) {
                                int progress = (int) ((seconds / 5.0f) * 100);
                                seekbarTimeline.setProgress(progress);
                            }
                        }
                    }
                    animHandler.postDelayed(this, 33);
                }
            }
        };

        if (btnAnimPlay != null) {
            btnAnimPlay.setOnClickListener(v -> {
                isPlaying = !isPlaying;
                btnAnimPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                
                for (Character c : runtime.getCharacterManager().getCharacterMap().values()) {
                    if (c.getAnimationPlayer() != null) {
                        if (isPlaying) {
                            c.getAnimationPlayer().resume();
                        } else {
                            c.getAnimationPlayer().pause();
                        }
                    }
                }
                
                if (isPlaying) {
                    animHandler.post(animRunnable);
                } else {
                    animHandler.removeCallbacks(animRunnable);
                }
            });
        }

        if (seekbarTimeline != null) {
            seekbarTimeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float seconds = (progress / 100.0f) * 5.0f;
                    tvAnimTime.setText(String.format(java.util.Locale.US, "%.1fs / 5.0s", seconds));
                    
                    if (fromUser) {
                        for (Character c : runtime.getCharacterManager().getCharacterMap().values()) {
                            if (c.getAnimationPlayer() != null) {
                                c.getAnimationPlayer().seek(seconds);
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // AI Assistant Dialog Launcher
        View btnAi = view.findViewById(R.id.btn_ai_studio_assistant);
        if (btnAi != null) {
            btnAi.setOnClickListener(v -> {
                AiAssistantDialogFragment dialog = new AiAssistantDialogFragment();
                dialog.show(getChildFragmentManager(), "AiAssistantDialog");
            });
        }
    }

    /**
     * Touch Event Handler: Translates touch gestures into spherical camera orbit rotation and pinch zoom.
     */
    private void setupViewportTouchOrbitGesture() {
        if (glSurfaceView == null) return;

        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                if (engine != null && engine.getCameraManager() != null) {
                    Camera camera = engine.getCameraManager().getActiveCamera();
                    if (camera != null) {
                        camera.zoom(scaleFactor);
                    }
                }
                return true;
            }
        });

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            private float previousTouchX;
            private float previousTouchY;
            private int activePointerId = MotionEvent.INVALID_POINTER_ID;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event == null) return false;

                scaleGestureDetector.onTouchEvent(event);

                int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        int pointerIndex = event.getActionIndex();
                        activePointerId = event.getPointerId(pointerIndex);
                        previousTouchX = event.getX(pointerIndex);
                        previousTouchY = event.getY(pointerIndex);
                        v.performClick();
                        return true;
                    }

                    case MotionEvent.ACTION_POINTER_DOWN: {
                        int pointerIndex = event.getActionIndex();
                        activePointerId = event.getPointerId(pointerIndex);
                        previousTouchX = event.getX(pointerIndex);
                        previousTouchY = event.getY(pointerIndex);
                        return true;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        if (event.getPointerCount() == 1 && !scaleGestureDetector.isInProgress()) {
                            int pointerIndex = event.findPointerIndex(activePointerId);
                            if (pointerIndex == -1) {
                                pointerIndex = 0;
                                activePointerId = event.getPointerId(pointerIndex);
                            }

                            float x = event.getX(pointerIndex);
                            float y = event.getY(pointerIndex);

                            float deltaX = x - previousTouchX;
                            float deltaY = y - previousTouchY;

                            if (Math.abs(deltaX) < 100f && Math.abs(deltaY) < 100f) {
                                if (engine != null && engine.getCameraManager() != null) {
                                    Camera camera = engine.getCameraManager().getActiveCamera();
                                    if (camera != null) {
                                        camera.orbit(deltaX * 0.006f, deltaY * 0.006f);
                                    }
                                }
                            }

                            previousTouchX = x;
                            previousTouchY = y;
                        }
                        return true;
                    }

                    case MotionEvent.ACTION_POINTER_UP: {
                        int pointerIndex = event.getActionIndex();
                        int pointerId = event.getPointerId(pointerIndex);
                        if (pointerId == activePointerId) {
                            int newPointerIndex = (pointerIndex == 0) ? 1 : 0;
                            if (newPointerIndex < event.getPointerCount()) {
                                activePointerId = event.getPointerId(newPointerIndex);
                                previousTouchX = event.getX(newPointerIndex);
                                previousTouchY = event.getY(newPointerIndex);
                            }
                        }
                        return true;
                    }

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        activePointerId = MotionEvent.INVALID_POINTER_ID;
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Spawns a procedural object into the active scene dynamically (used by AI Assistant).
     */
    public boolean spawnProceduralObject(String type, String name, float x, float y, float z, String colorHex) {
        if (engine == null) return false;
        try {
            runtime.getTransactionManager().beginTransaction("Spawn " + name);
            SceneObject obj = engine.createProceduralStructure(type, name);
            if (obj != null) {
                obj.getTransform().setPosition(x, y, z);
                if (colorHex != null && !colorHex.isEmpty()) {
                    Material mat = new Material("mat_" + System.currentTimeMillis(), name + "_Mat", colorHex);
                    obj.setMaterial(mat);
                }
                engine.getSceneManager().updateWorldTransforms();
                runtime.getTransactionManager().commitTransaction();
                updateStudioStatsUI();
                return true;
            }
            runtime.getTransactionManager().rollbackTransaction();
        } catch (Exception e) {
            VynaraLogger.e("Failed to spawn procedural object: " + name, e);
        }
        return false;
    }

    public void clearScene() {
        if (engine != null && engine.getSceneManager() != null) {
            engine.getSceneManager().getActiveScene().getObjects().clear();
            if (runtime != null && runtime.getCharacterManager() != null) {
                runtime.getCharacterManager().getCharacterMap().clear();
            }
            updateStudioStatsUI();
        }
    }

    public void loadAndDisplayGLBFile(File glbFile) {
        if (glbFile == null || !glbFile.exists() || engine == null) return;

        try {
            VynaraLogger.system("StudioFragment: Loading external GLB into active scene: " + glbFile.getName());
            GLTFImporter.ImportResult result = GLTFImporter.loadFromFile(glbFile);

            runtime.getTransactionManager().beginTransaction("Import GLB Model");

            for (SceneObject obj : result.getSceneObjects()) {
                engine.getSceneManager().getActiveScene().addObject(obj);
            }

            for (Character ch : result.getCharacters()) {
                runtime.getCharacterManager().registerCharacter(ch);
            }

            engine.getSceneManager().updateWorldTransforms();

            // Auto-frame camera on newly imported model
            if (!result.getSceneObjects().isEmpty() && engine.getCameraManager() != null) {
                SceneObject first = result.getSceneObjects().get(0);
                if (first.getTransform() != null) {
                    Camera cam = engine.getCameraManager().getActiveCamera();
                    if (cam != null) {
                        cam.setTarget(first.getTransform().getPx(), first.getTransform().getPy() + 1.0f, first.getTransform().getPz());
                    }
                }
            }

            runtime.getTransactionManager().commitTransaction();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateStudioStatsUI();
                    Toast.makeText(getContext(), "Imported: " + glbFile.getName(), Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            VynaraLogger.e("StudioFragment: Failed loading GLB file", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Import error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    public void updateStudioStatsUI() {
        if (tvStats != null && engine != null) {
            Scene activeScene = engine.getSceneManager().getActiveScene();
            int triangles = activeScene != null ? activeScene.getTotalTriangleCount() : 0;
            int vertices = activeScene != null ? activeScene.getTotalVertexCount() : 0;
            tvStats.setText("Tris: " + triangles + " | Verts: " + vertices);
        }
    }

    private void exportActiveSceneToLocalGltf() {
        if (getContext() == null || engine == null) return;

        try {
            Scene activeScene = engine.getSceneManager().getActiveScene();
            String gltfJson = GLTFExporter.exportSceneToGLTFJson(activeScene);

            File exportDir = new File(getContext().getExternalFilesDir(null), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                Toast.makeText(getContext(), "Failed to create export folder", Toast.LENGTH_SHORT).show();
                return;
            }

            File exportFile = new File(exportDir, "vynara_scene_" + System.currentTimeMillis() + ".gltf");
            FileOutputStream fos = new FileOutputStream(exportFile);
            fos.write(gltfJson.getBytes());
            fos.close();

            Toast.makeText(getContext(), "Scene exported to: " + exportFile.getName(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "GLTF Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (glSurfaceView != null) glSurfaceView.onResume();
        updateStudioStatsUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (glSurfaceView != null) glSurfaceView.onPause();
        isPlaying = false;
        animHandler.removeCallbacks(animRunnable);
    }
}