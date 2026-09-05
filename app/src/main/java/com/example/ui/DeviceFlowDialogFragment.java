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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.ai.ApiKeyManager;
import com.example.cloud.CloudProvider;
import com.example.cloud.GitHubOAuthService;
import com.example.cloud.models.DeviceCodeResponse;

public class DeviceFlowDialogFragment extends DialogFragment {

    private static final String ARG_CLIENT_ID = "client_id";
    private static final String ARG_CLIENT_SECRET = "client_secret";

    private TextView tvUserCode;
    private TextView tvVerificationUrl;
    private TextView tvStatusMessage;
    private ProgressBar progressBar;
    private Button btnOpenVerification;
    private Button btnCancel;

    private GitHubOAuthService oAuthService;
    private String clientId;
    private String clientSecret;
    private DeviceCodeResponse deviceCodeResponse;

    public static DeviceFlowDialogFragment newInstance(String clientId, String clientSecret) {
        DeviceFlowDialogFragment fragment = new DeviceFlowDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientId);
        args.putString(ARG_CLIENT_SECRET, clientSecret);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        if (getArguments() != null) {
            clientId = getArguments().getString(ARG_CLIENT_ID, "");
            clientSecret = getArguments().getString(ARG_CLIENT_SECRET, "");
        }
        oAuthService = new GitHubOAuthService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_device_flow, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserCode = view.findViewById(R.id.tv_device_user_code);
        tvVerificationUrl = view.findViewById(R.id.tv_device_verification_url);
        tvStatusMessage = view.findViewById(R.id.tv_device_status_message);
        progressBar = view.findViewById(R.id.progress_device_polling);
        btnOpenVerification = view.findViewById(R.id.btn_open_github_verification);
        btnCancel = view.findViewById(R.id.btn_device_cancel);

        setCancelable(false);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelAndDismiss());
        }

        startDeviceFlow();
    }

    private void startDeviceFlow() {
        if (clientId == null || clientId.trim().isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Missing GitHub Client ID.", Toast.LENGTH_SHORT).show();
            }
            dismiss();
            return;
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Requesting code from GitHub...");
        }

        oAuthService.requestDeviceCode(clientId, clientSecret, new GitHubOAuthService.DeviceCodeCallback() {
            @Override
            public void onDeviceCodeReceived(DeviceCodeResponse response) {
                deviceCodeResponse = response;
                setupDeviceCodeUI(response);
                startPolling(response);
            }

            @Override
            public void onError(String errorMessage) {
                if (tvStatusMessage != null) {
                    tvStatusMessage.setText("Error: " + errorMessage);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Device Code Request Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupDeviceCodeUI(DeviceCodeResponse response) {
        if (tvUserCode != null) {
            tvUserCode.setText(response.getUserCode());
        }

        if (tvVerificationUrl != null) {
            tvVerificationUrl.setText("Verification URL:\n" + response.getVerificationUri());
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Waiting for authorization on GitHub...");
        }

        if (btnOpenVerification != null) {
            btnOpenVerification.setOnClickListener(v -> {
                if (getContext() != null) {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("GitHub User Code", response.getUserCode());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }
                    Toast.makeText(getContext(), "Code " + response.getUserCode() + " copied to clipboard!", Toast.LENGTH_SHORT).show();
                }

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(response.getVerificationUri()));
                startActivity(browserIntent);
            });
        }
    }

    private void startPolling(DeviceCodeResponse response) {
        oAuthService.startDeviceFlowPolling(
                clientId,
                clientSecret,
                response.getDeviceCode(),
                response.getInterval(),
                response.getExpiresIn(),
                new GitHubOAuthService.DevicePollingCallback() {
                    @Override
                    public void onTokenReceived(String accessToken) {
                        if (tvStatusMessage != null) {
                            tvStatusMessage.setText("Authorization granted! Fetching profile...");
                        }

                        if (getContext() != null) {
                            GitHubOAuthService.saveAccessToken(requireContext(), accessToken);
                        }

                        oAuthService.fetchUserProfile(accessToken, new GitHubOAuthService.UserProfileCallback() {
                            @Override
                            public void onSuccess(String login, String name, String avatarUrl) {
                                if (getContext() != null) {
                                    GitHubOAuthService.saveUserProfile(requireContext(), login, name, avatarUrl);

                                    if (tvStatusMessage != null) {
                                        tvStatusMessage.setText("Setting up workspace repository...");
                                    }

                                    // Method 2: Automatic Background Workspace & Workflow Provisioning
                                    oAuthService.provisionUserWorkspace(requireContext(), accessToken, login, new GitHubOAuthService.ProvisionCallback() {
                                        @Override
                                        public void onSuccess(String repoFullName) {
                                            if (getContext() != null) {
                                                ApiKeyManager keyMgr = new ApiKeyManager(requireContext());
                                                keyMgr.saveGitHubUser(login, avatarUrl);
                                                keyMgr.saveGitHubConfig(repoFullName, accessToken, "vynara_generate");
                                                keyMgr.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);

                                                Toast.makeText(getContext(), "Signed in & Workspace Ready (@" + login + ")!", Toast.LENGTH_LONG).show();

                                                notifySettingsFragmentToUpdate();
                                            }
                                            dismiss();
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            if (getContext() != null) {
                                                ApiKeyManager keyMgr = new ApiKeyManager(requireContext());
                                                keyMgr.saveGitHubUser(login, avatarUrl);
                                                keyMgr.saveGitHubConfig(login + "/vynara2", accessToken, "vynara_generate");
                                                keyMgr.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);

                                                Toast.makeText(getContext(), "Signed in as @" + login + "!", Toast.LENGTH_LONG).show();

                                                notifySettingsFragmentToUpdate();
                                            }
                                            dismiss();
                                        }
                                    });
                                } else {
                                    dismiss();
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                if (getContext() != null) {
                                    ApiKeyManager keyMgr = new ApiKeyManager(requireContext());
                                    keyMgr.saveGitHubConfig(keyMgr.getGitHubRepo(), accessToken, "vynara_generate");
                                    notifySettingsFragmentToUpdate();
                                }
                                dismiss();
                            }
                        });
                    }

                    @Override
                    public void onPending(String status) {
                        if (tvStatusMessage != null) {
                            tvStatusMessage.setText(status);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (tvStatusMessage != null) {
                            tvStatusMessage.setText("Failed: " + errorMessage);
                        }
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (getContext() != null) {
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void notifySettingsFragmentToUpdate() {
        if (getParentFragmentManager() != null) {
            for (Fragment f : getParentFragmentManager().getFragments()) {
                if (f instanceof SettingsFragment && f.isVisible()) {
                    ((SettingsFragment) f).updateGitHubAuthUI(null);
                }
            }
        }
    }

    private void cancelAndDismiss() {
        if (oAuthService != null) {
            oAuthService.cancelDeviceFlowPolling();
        }
        dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (oAuthService != null) {
            oAuthService.cancelDeviceFlowPolling();
        }
    }
}