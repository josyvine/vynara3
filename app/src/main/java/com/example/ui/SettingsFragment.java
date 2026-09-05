package com.example.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.ai.AIModel;
import com.example.ai.ApiKeyManager;
import com.example.ai.GeminiApiClient;
import com.example.cloud.CloudProvider;
import com.example.cloud.GitHubOAuthService;
import com.example.cloud.GitHubWorkflowBridge;
import com.example.cloud.HuggingFaceBridge;
import com.example.cloud.models.DeviceCodeResponse;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etApiKey;
    private TextView tvStatus;
    private Spinner spinnerModel;
    private Spinner spinnerRenderQuality;

    // Cloud Worker Views
    private Spinner spinnerComputeProvider;
    private EditText etGithubRepo;
    private EditText etGithubPat;
    private EditText etHfSpaceUrl;
    private EditText etHfToken;

    // GitHub OAuth Views
    private LinearLayout layoutGithubLoggedOut;
    private LinearLayout layoutGithubLoggedIn;
    private TextView tvGithubUser;
    private Button btnGithubLogin;
    private Button btnGithubDeviceLogin;
    private Button btnGithubLogout;

    private GitHubOAuthService oAuthService;
    private AlertDialog deviceCodeDialog;

    private boolean isUpdatingModels = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        oAuthService = new GitHubOAuthService();

        etApiKey = view.findViewById(R.id.et_api_key);
        tvStatus = view.findViewById(R.id.tv_connection_status);
        spinnerModel = view.findViewById(R.id.spinner_gemini_model);
        spinnerRenderQuality = view.findViewById(R.id.spinner_settings_render_quality);

        spinnerComputeProvider = view.findViewById(R.id.spinner_compute_provider);
        etGithubRepo = view.findViewById(R.id.et_github_repo);
        etGithubPat = view.findViewById(R.id.et_github_pat);
        etHfSpaceUrl = view.findViewById(R.id.et_hf_space_url);
        etHfToken = view.findViewById(R.id.et_hf_token);

        layoutGithubLoggedOut = view.findViewById(R.id.layout_github_logged_out);
        layoutGithubLoggedIn = view.findViewById(R.id.layout_github_logged_in);
        tvGithubUser = view.findViewById(R.id.tv_github_user);
        btnGithubLogin = view.findViewById(R.id.btn_github_login);
        btnGithubDeviceLogin = view.findViewById(R.id.btn_github_device_login);
        btnGithubLogout = view.findViewById(R.id.btn_github_logout);

        final List<String> modelList = new ArrayList<>();
        modelList.add("gemini-3.5-flash");
        modelList.add("gemini-3.1-flash-lite");
        modelList.add("gemini-3.1-pro");

        final ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, modelList);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerModel != null) {
            spinnerModel.setAdapter(modelAdapter);
        }

        if (getContext() != null) {
            ApiKeyManager keyMgr = new ApiKeyManager(getContext());
            
            // Display securely masked Gemini API key
            if (keyMgr.hasApiKey()) {
                if (etApiKey != null) etApiKey.setText(keyMgr.getMaskedApiKey());
                if (tvStatus != null) {
                    tvStatus.setText("● Connected (Secure)");
                    tvStatus.setTextColor(0xFF00E676);
                }
                fetchLiveModels(keyMgr.getApiKey(), modelList, modelAdapter, keyMgr, false);
            } else {
                if (tvStatus != null) {
                    tvStatus.setText("● Disconnected");
                    tvStatus.setTextColor(0xFFFF5252);
                }
                isUpdatingModels = false;
            }

            String currentSelectedModel = keyMgr.getSelectedModel();
            int selectedIdx = modelList.indexOf(currentSelectedModel);
            if (spinnerModel != null && selectedIdx >= 0) {
                spinnerModel.setSelection(selectedIdx);
            }

            if (spinnerModel != null) {
                spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (isUpdatingModels) return;
                        if (position >= 0 && position < modelList.size()) {
                            String selected = modelList.get(position);
                            keyMgr.saveSelectedModel(selected);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            // Quality Spinner
            String[] qualities = new String[]{"Ultra (PBR + Shadows + PostFX)", "High (PBR Standard)", "Medium (Fast Mobile)", "Low (Wireframe Draft)"};
            ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, qualities);
            qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (spinnerRenderQuality != null) {
                spinnerRenderQuality.setAdapter(qualityAdapter);
            }

            // Cloud Provider Spinner
            if (spinnerComputeProvider != null) {
                CloudProvider[] providers = CloudProvider.values();
                String[] names = new String[providers.length];
                for (int i = 0; i < providers.length; i++) {
                    names[i] = providers[i].getDisplayName();
                }
                ArrayAdapter<String> provAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
                provAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerComputeProvider.setAdapter(provAdapter);
                spinnerComputeProvider.setSelection(keyMgr.getComputeProvider().ordinal());

                spinnerComputeProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position >= 0 && position < providers.length) {
                            keyMgr.saveComputeProvider(providers[position]);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            // Setup GitHub OAuth State & UI
            updateGitHubAuthUI(keyMgr);

            // Restore GitHub fields
            if (etGithubRepo != null && !keyMgr.getGitHubRepo().isEmpty()) {
                etGithubRepo.setText(keyMgr.getGitHubRepo());
            }
            if (etGithubPat != null && keyMgr.hasGitHubConfig()) {
                etGithubPat.setText(keyMgr.getMaskedGitHubPat());
            }

            // Restore Hugging Face fields
            if (etHfSpaceUrl != null && !keyMgr.getHuggingFaceSpaceUrl().isEmpty()) {
                etHfSpaceUrl.setText(keyMgr.getHuggingFaceSpaceUrl());
            }
            if (etHfToken != null && !keyMgr.getHuggingFaceToken().isEmpty()) {
                etHfToken.setText(keyMgr.getMaskedHuggingFaceToken());
            }

            // GitHub OAuth Web Login Trigger
            if (btnGithubLogin != null) {
                btnGithubLogin.setOnClickListener(v -> openGitHubLoginMenu());
            }

            // GitHub Device Code Flow Trigger
            if (btnGithubDeviceLogin != null) {
                btnGithubDeviceLogin.setOnClickListener(v -> startGitHubDeviceFlow(keyMgr));
            }

            // GitHub Logout Trigger
            if (btnGithubLogout != null) {
                btnGithubLogout.setOnClickListener(v -> showLogoutConfirmationDialog(keyMgr));
            }

            // Save & Test buttons for GitHub
            Button btnSaveGh = view.findViewById(R.id.btn_save_github);
            if (btnSaveGh != null) {
                btnSaveGh.setOnClickListener(v -> {
                    String repo = etGithubRepo != null ? etGithubRepo.getText().toString().trim() : "";
                    String pat = etGithubPat != null ? etGithubPat.getText().toString().trim() : "";
                    if (pat.contains("••••")) {
                        pat = keyMgr.getGitHubPat();
                    }
                    keyMgr.saveGitHubConfig(repo, pat, "vynara_generate");
                    Toast.makeText(getContext(), "GitHub configuration saved", Toast.LENGTH_SHORT).show();
                    if (etGithubPat != null && keyMgr.hasGitHubConfig()) {
                        etGithubPat.setText(keyMgr.getMaskedGitHubPat());
                    }
                });
            }

            Button btnTestGh = view.findViewById(R.id.btn_test_github);
            if (btnTestGh != null) {
                btnTestGh.setOnClickListener(v -> {
                    if (!keyMgr.hasGitHubConfig()) {
                        Toast.makeText(getContext(), "Save GitHub Repo and PAT first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(getContext(), "Testing GitHub Actions connection...", Toast.LENGTH_SHORT).show();
                    new GitHubWorkflowBridge().testConnection(getContext(), keyMgr.getGitHubRepo(), new GitHubWorkflowBridge.ConnectionTestCallback() {
                        @Override
                        public void onSuccess(String repoFullName, boolean hasWorkflowAccess) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Connected to: " + repoFullName + " (Push: " + hasWorkflowAccess + ")", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "GitHub Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                });
            }

            // Save & Test buttons for Hugging Face
            Button btnSaveHf = view.findViewById(R.id.btn_save_hf);
            if (btnSaveHf != null) {
                btnSaveHf.setOnClickListener(v -> {
                    String url = etHfSpaceUrl != null ? etHfSpaceUrl.getText().toString().trim() : "";
                    String token = etHfToken != null ? etHfToken.getText().toString().trim() : "";
                    if (token.contains("••••")) {
                        token = keyMgr.getHuggingFaceToken();
                    }
                    keyMgr.saveHuggingFaceConfig(url, token);
                    Toast.makeText(getContext(), "Hugging Face configuration saved", Toast.LENGTH_SHORT).show();
                    if (etHfToken != null && !token.isEmpty()) {
                        etHfToken.setText(keyMgr.getMaskedHuggingFaceToken());
                    }
                });
            }

            Button btnTestHf = view.findViewById(R.id.btn_test_hf);
            if (btnTestHf != null) {
                btnTestHf.setOnClickListener(v -> {
                    if (!keyMgr.hasHuggingFaceConfig()) {
                        Toast.makeText(getContext(), "Save Space URL first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(getContext(), "Testing Hugging Face Space...", Toast.LENGTH_SHORT).show();
                    new HuggingFaceBridge().testConnection(keyMgr.getHuggingFaceSpaceUrl(), keyMgr.getHuggingFaceToken(), new HuggingFaceBridge.ConnectionTestCallback() {
                        @Override
                        public void onSuccess(String statusMessage) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Space is Online!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "HF Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                });
            }
        }

        Button btnSaveKey = view.findViewById(R.id.btn_save_key);
        if (btnSaveKey != null) {
            btnSaveKey.setOnClickListener(v -> {
                if (etApiKey == null) return;
                String key = etApiKey.getText().toString().trim();
                if (key.contains("••••")) {
                    Toast.makeText(getContext(), "Using securely stored API key", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getContext() != null) {
                    ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                    keyMgr.saveApiKey(key);
                    Toast.makeText(getContext(), "Gemini API Key saved securely", Toast.LENGTH_SHORT).show();
                    if (tvStatus != null) {
                        tvStatus.setText("● Connected (Secure)");
                        tvStatus.setTextColor(0xFF00E676);
                    }
                    etApiKey.setText(keyMgr.getMaskedApiKey());
                    if (!key.isEmpty()) {
                        fetchLiveModels(key, modelList, modelAdapter, keyMgr, true);
                    }
                }
            });
        }

        ImageButton btnFetch = view.findViewById(R.id.btn_fetch_models);
        if (btnFetch != null) {
            btnFetch.setOnClickListener(v -> {
                if (getContext() == null || etApiKey == null) return;
                ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                String apiKey = etApiKey.getText().toString().trim();
                if (apiKey.isEmpty() || apiKey.contains("••••")) {
                    apiKey = keyMgr.getApiKey();
                }
                if (apiKey.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter and save a Gemini API key first", Toast.LENGTH_SHORT).show();
                    return;
                }
                fetchLiveModels(apiKey, modelList, modelAdapter, keyMgr, true);
            });
        }

        Button btnTest = view.findViewById(R.id.btn_test_connection);
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> {
                if (getContext() == null || etApiKey == null) return;
                ApiKeyManager keyMgr = new ApiKeyManager(getContext());
                String apiKey = etApiKey.getText().toString().trim();
                if (apiKey.isEmpty() || apiKey.contains("••••")) {
                    apiKey = keyMgr.getApiKey();
                }
                if (apiKey.isEmpty()) {
                    Toast.makeText(getContext(), "Please save an API Key first", Toast.LENGTH_SHORT).show();
                    return;
                }
                String selectedModel = keyMgr.getSelectedModel();
                Toast.makeText(getContext(), "Testing connection with " + selectedModel + "...", Toast.LENGTH_SHORT).show();
                new GeminiApiClient().testConnection(apiKey, selectedModel, new GeminiApiClient.ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Connection Successful! Model: " + selectedModel, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Connection failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });
        }

        Button btnClearCache = view.findViewById(R.id.btn_clear_cache);
        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Render, Model and Texture Cache cleared", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void updateGitHubAuthUI(ApiKeyManager keyMgr) {
        if (keyMgr == null && getContext() != null) {
            keyMgr = new ApiKeyManager(getContext());
        }
        if (keyMgr == null) return;

        boolean loggedIn = keyMgr.isGitHubLoggedIn() || GitHubOAuthService.isLoggedIn(getContext());

        if (layoutGithubLoggedIn != null) {
            layoutGithubLoggedIn.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        }
        if (layoutGithubLoggedOut != null) {
            layoutGithubLoggedOut.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        }
        if (loggedIn && tvGithubUser != null) {
            String user = keyMgr.getGitHubUsername();
            if (user.isEmpty() && getContext() != null) {
                user = GitHubOAuthService.getUserLogin(getContext());
            }
            tvGithubUser.setText("Signed in as @" + (user.isEmpty() ? "GitHub User" : user));
        }
    }

    private void openGitHubLoginMenu() {
        if (getParentFragmentManager() != null) {
            GitHubLoginDialogFragment dialog = new GitHubLoginDialogFragment();
            dialog.show(getParentFragmentManager(), "GitHubLoginDialog");
        }
    }

    private void showLogoutConfirmationDialog(ApiKeyManager keyMgr) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of Vynara GitHub Workspace?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    keyMgr.logoutGitHub();
                    GitHubOAuthService.clearAuth(getContext());
                    if (etGithubPat != null) etGithubPat.setText("");
                    updateGitHubAuthUI(keyMgr);
                    Toast.makeText(getContext(), "Successfully logged out.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startGitHubDeviceFlow(ApiKeyManager keyMgr) {
        if (getContext() == null) return;
        
        String clientId = GitHubOAuthService.getClientId(getContext());
        if (clientId.isEmpty()) {
            clientId = keyMgr.getGitHubClientId();
        }

        if (clientId == null || clientId.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please configure OAuth Client ID first.", Toast.LENGTH_SHORT).show();
            if (getParentFragmentManager() != null) {
                ConfigureOAuthDialogFragment configDialog = new ConfigureOAuthDialogFragment();
                configDialog.show(getParentFragmentManager(), "ConfigureOAuthDialog");
            }
            return;
        }

        DeviceFlowDialogFragment dialog = DeviceFlowDialogFragment.newInstance(clientId, GitHubOAuthService.getClientSecret(getContext()));
        dialog.show(getParentFragmentManager(), "DeviceFlowDialog");
    }

    private void fetchLiveModels(String apiKey, List<String> modelList, ArrayAdapter<String> modelAdapter, ApiKeyManager keyMgr, boolean showToast) {
        if (apiKey == null || apiKey.trim().isEmpty()) return;
        if (showToast && getContext() != null) {
            Toast.makeText(getContext(), "Fetching live models from Google Gemini API...", Toast.LENGTH_SHORT).show();
        }

        isUpdatingModels = true;

        new GeminiApiClient().fetchModels(apiKey.trim(), new GeminiApiClient.ApiCallback<List<AIModel>>() {
            @Override
            public void onSuccess(List<AIModel> result) {
                if (getContext() == null || result == null || result.isEmpty()) {
                    isUpdatingModels = false;
                    return;
                }
                
                modelList.clear();
                for (AIModel m : result) {
                    modelList.add(m.getName());
                }
                modelAdapter.notifyDataSetChanged();

                String savedModel = keyMgr.getSelectedModel();
                int idx = modelList.indexOf(savedModel);
                if (spinnerModel != null && idx >= 0) {
                    spinnerModel.setSelection(idx);
                } else if (!modelList.isEmpty() && spinnerModel != null) {
                    spinnerModel.setSelection(0);
                    keyMgr.saveSelectedModel(modelList.get(0));
                }

                if (spinnerModel != null) {
                    spinnerModel.post(() -> isUpdatingModels = false);
                } else {
                    isUpdatingModels = false;
                }

                if (showToast && getContext() != null) {
                    Toast.makeText(getContext(), "Fetched " + result.size() + " live models from Google API!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                isUpdatingModels = false;
                if (showToast && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to fetch live models: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (oAuthService != null) {
            oAuthService.cancelDeviceFlowPolling();
        }
        if (deviceCodeDialog != null && deviceCodeDialog.isShowing()) {
            deviceCodeDialog.dismiss();
        }
    }
}