package com.example.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.ai.ApiKeyManager;
import com.example.cloud.CloudProvider;
import com.example.cloud.GitHubOAuthService;

public class ManualOAuthDialogFragment extends DialogFragment {

    private EditText etOAuthCode;
    private GitHubOAuthService oAuthService;

    public static ManualOAuthDialogFragment newInstance() {
        return new ManualOAuthDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        oAuthService = new GitHubOAuthService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_manual_oauth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etOAuthCode = view.findViewById(R.id.et_manual_oauth_code);
        Button btnCancel = view.findViewById(R.id.btn_manual_oauth_cancel);
        Button btnSubmit = view.findViewById(R.id.btn_manual_oauth_submit);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submitCode());
        }
    }

    private void submitCode() {
        if (etOAuthCode == null || getContext() == null) return;

        String code = etOAuthCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(getContext(), "Please enter an OAuth code.", Toast.LENGTH_SHORT).show();
            return;
        }

        String clientId = GitHubOAuthService.getClientId(requireContext());
        String clientSecret = GitHubOAuthService.getClientSecret(requireContext());

        ApiKeyManager keyMgr = new ApiKeyManager(requireContext());
        if (clientId.isEmpty()) {
            clientId = keyMgr.getGitHubClientId();
        }

        if (clientId == null || clientId.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please configure OAuth App Client ID first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Exchanging authorization code for token...", Toast.LENGTH_SHORT).show();

        oAuthService.exchangeCodeForToken(clientId, clientSecret, code, GitHubOAuthService.DEFAULT_REDIRECT_URI, new GitHubOAuthService.OAuthTokenCallback() {
            @Override
            public void onSuccess(String accessToken, String tokenType, String scope) {
                if (getContext() != null) {
                    GitHubOAuthService.saveAccessToken(requireContext(), accessToken);

                    oAuthService.fetchUserProfile(accessToken, new GitHubOAuthService.UserProfileCallback() {
                        @Override
                        public void onSuccess(String login, String name, String avatarUrl) {
                            if (getContext() != null) {
                                GitHubOAuthService.saveUserProfile(requireContext(), login, name, avatarUrl);

                                Toast.makeText(getContext(), "Setting up workspace repository...", Toast.LENGTH_SHORT).show();

                                // Method 2: Automatic Background Workspace & Workflow Provisioning
                                oAuthService.provisionUserWorkspace(requireContext(), accessToken, login, new GitHubOAuthService.ProvisionCallback() {
                                    @Override
                                    public void onSuccess(String repoFullName) {
                                        if (getContext() != null) {
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
                                keyMgr.saveGitHubConfig(keyMgr.getGitHubRepo(), accessToken, "vynara_generate");
                                notifySettingsFragmentToUpdate();
                            }
                            dismiss();
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "OAuth Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
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
}