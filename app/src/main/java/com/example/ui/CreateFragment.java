package com.example.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.MainActivity;
import com.example.R;
import com.example.ai.ApiKeyManager;
import com.example.utils.VynaraLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CreateFragment extends Fragment {

    private EditText etPrompt;
    private TextView tvReferenceCount;
    private TextView tvToggleAdvanced;
    private LinearLayout layoutAdvancedContent;
    private Spinner spinnerStyle, spinnerQuality, spinnerTarget, spinnerAutoMode;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register launcher to select multiple reference images from phone storage
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedImageUris.addAll(uris);
                        updateReferenceUI();
                        Toast.makeText(getContext(), uris.size() + " reference image(s) added successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "No reference image selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Register launcher to request runtime storage/media permissions
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(getContext(), "Permission denied. Cannot access reference images.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPrompt = view.findViewById(R.id.et_prompt);
        tvReferenceCount = view.findViewById(R.id.tv_reference_count);
        tvToggleAdvanced = view.findViewById(R.id.tv_toggle_advanced);
        layoutAdvancedContent = view.findViewById(R.id.layout_advanced_content);

        spinnerStyle = view.findViewById(R.id.spinner_style);
        spinnerQuality = view.findViewById(R.id.spinner_quality);
        spinnerTarget = view.findViewById(R.id.spinner_target);
        spinnerAutoMode = view.findViewById(R.id.spinner_auto_mode);

        setupSpinners();

        // Dynamically resolve and update active model sub-header from ApiKeyManager
        if (getContext() != null) {
            ApiKeyManager keyMgr = new ApiKeyManager(getContext());
            String activeModel = keyMgr.getSelectedModel();
            
            TextView tvConnectionStatus = view.findViewById(R.id.tv_model_badge);
            if (tvConnectionStatus == null) {
                tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
            }
            if (tvConnectionStatus == null) {
                int fallbackId = view.getResources().getIdentifier("tv_ai_status", "id", requireContext().getPackageName());
                if (fallbackId != 0) {
                    tvConnectionStatus = view.findViewById(fallbackId);
                }
            }

            if (tvConnectionStatus != null) {
                if (keyMgr.hasApiKey()) {
                    String displayName = (activeModel == null || activeModel.trim().isEmpty()) ? "gemini-1.5-flash" : activeModel;
                    tvConnectionStatus.setText("AI: " + displayName + " • Connected");
                    tvConnectionStatus.setTextColor(0xFF00E676); // Green connection color
                } else {
                    tvConnectionStatus.setText("AI: Disconnected (No API Key)");
                    tvConnectionStatus.setTextColor(0xFFFF5252); // Red disconnection color
                }
            }
        }

        View btnAddRef = view.findViewById(R.id.btn_add_reference);
        if (btnAddRef != null) {
            btnAddRef.setOnClickListener(v -> checkPermissionAndPickImages());
        }

        // Tap reference count badge to clear attached images
        if (tvReferenceCount != null) {
            tvReferenceCount.setOnClickListener(v -> {
                if (!selectedImageUris.isEmpty()) {
                    selectedImageUris.clear();
                    updateReferenceUI();
                    Toast.makeText(getContext(), "Reference images cleared", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Accordion toggle
        View headerAdv = view.findViewById(R.id.layout_advanced_header);
        if (headerAdv != null) {
            headerAdv.setOnClickListener(v -> {
                if (layoutAdvancedContent.getVisibility() == View.VISIBLE) {
                    layoutAdvancedContent.setVisibility(View.GONE);
                    tvToggleAdvanced.setText("Expand ▼");
                } else {
                    layoutAdvancedContent.setVisibility(View.VISIBLE);
                    tvToggleAdvanced.setText("Collapse ▲");
                }
            });
        }

        // Presets with rich director prompts
        setupPresetButton(view, R.id.preset_house, "Create a realistic modern villa with a swimming pool, wooden deck, interior lighting, furniture, and surrounding palm trees.");
        setupPresetButton(view, R.id.preset_human, "Create a stylized rigged superhero character with suit details, heroic posture, and skeletal animation tracks.");
        setupPresetButton(view, R.id.preset_dog, "Create an animated quadruped dog model with skeletal rig, fur material, and a running cycle animation.");
        setupPresetButton(view, R.id.preset_sofa, "Create a modern luxury leather sofa with realistic cushion seams, metallic legs, and wood trim.");
        setupPresetButton(view, R.id.preset_village, "Create a high-detail tropical village environment with wooden huts, sand terrain, palm trees, and ocean shoreline.");

        // Generate button
        Button btnGenerate = view.findViewById(R.id.btn_create_generate);
        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> {
                String prompt = etPrompt.getText().toString().trim();
                if (prompt.isEmpty()) {
                    prompt = "Modern Villa & Swimming Pool";
                }

                String style = spinnerStyle.getSelectedItem() != null ? spinnerStyle.getSelectedItem().toString() : "Photorealistic";
                String targetEngine = spinnerTarget.getSelectedItem() != null ? spinnerTarget.getSelectedItem().toString() : "OpenGL ES / GLTF";

                // Cache reference images locally so background workers have direct file access
                List<String> refUrisStrList = new ArrayList<>();
                for (Uri uri : selectedImageUris) {
                    if (uri != null) {
                        String localFilePath = cacheReferenceImage(requireContext(), uri);
                        refUrisStrList.add(localFilePath != null ? localFilePath : uri.toString());
                    }
                }

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).startProduction(prompt, style, targetEngine, refUrisStrList);
                }
            });
        }
    }

    private void checkPermissionAndPickImages() {
        if (getContext() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openImagePicker() {
        if (imagePickerLauncher != null) {
            imagePickerLauncher.launch("image/*");
        }
    }

    private void updateReferenceUI() {
        if (tvReferenceCount != null) {
            int count = selectedImageUris.size();
            tvReferenceCount.setText(count + " reference(s) added (Tap to clear)");
        }
    }

    private void setupPresetButton(View root, int resId, String promptText) {
        View btn = root.findViewById(resId);
        if (btn != null) {
            btn.setOnClickListener(v -> etPrompt.setText(promptText));
        }
    }

    private void setupSpinners() {
        if (getContext() == null) return;

        String[] styles = new String[]{"Photorealistic", "Stylized / Low-Poly", "Cinematic CGI", "Anime / Toon"};
        ArrayAdapter<String> adapterStyle = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, styles);
        adapterStyle.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStyle.setAdapter(adapterStyle);

        String[] qualities = new String[]{"High Detail (4K Textures)", "Medium (Optimized)", "Mobile Ultra Lite"};
        ArrayAdapter<String> adapterQuality = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, qualities);
        adapterQuality.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuality.setAdapter(adapterQuality);

        String[] targets = new String[]{"Blender Native", "OpenGL ES / GLTF", "Unreal Engine 5", "Unity Universal RP"};
        ArrayAdapter<String> adapterTarget = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, targets);
        adapterTarget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTarget.setAdapter(adapterTarget);

        String[] modes = new String[]{"Fully Autonomous AI", "Step-by-step Interactive", "Fast Draft Mode"};
        ArrayAdapter<String> adapterMode = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, modes);
        adapterMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAutoMode.setAdapter(adapterMode);
    }

    /**
     * Copies selected Android content URI into a persistent local file in the app cache
     * so background threads and Gemini Vision can access it without permission security exceptions.
     */
    private String cacheReferenceImage(Context context, Uri contentUri) {
        try {
            File cacheFolder = new File(context.getCacheDir(), "references");
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs();
            }

            File destFile = new File(cacheFolder, "ref_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + ".jpg");

            try (InputStream in = context.getContentResolver().openInputStream(contentUri);
                 FileOutputStream out = new FileOutputStream(destFile)) {

                if (in == null) return null;

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            VynaraLogger.e("CreateFragment: Failed caching reference image: " + e.getMessage());
            return contentUri.toString();
        }
    }

    public List<Uri> getSelectedImageUris() {
        return selectedImageUris;
    }
}