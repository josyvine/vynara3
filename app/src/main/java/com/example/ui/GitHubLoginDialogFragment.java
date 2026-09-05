package com.example.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.R;
import com.example.ai.ApiKeyManager;
import com.example.cloud.GitHubOAuthService;

public class GitHubLoginDialogFragment extends DialogFragment {

    public static GitHubLoginDialogFragment newInstance() {
        return new GitHubLoginDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Vynara2_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_github_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnSignInWeb = view.findViewById(R.id.btn_sign_in_web);
        Button btnSignInDevice = view.findViewById(R.id.btn_sign_in_device);
        TextView tvEnterOAuthCode = view.findViewById(R.id.tv_enter_oauth_code);
        TextView tvConfigureAppId = view.findViewById(R.id.tv_configure_app_id);
        Button btnDemoGuestMode = view.findViewById(R.id.btn_demo_guest_mode);

        ApiKeyManager keyMgr = new ApiKeyManager(requireContext());

        if (btnSignInWeb != null) {
            btnSignInWeb.setOnClickListener(v -> {
                String clientId = GitHubOAuthService.getClientId(requireContext());
                if (clientId.isEmpty()) {
                    clientId = keyMgr.getGitHubClientId();
                }

                if (clientId == null || clientId.trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please configure OAuth App Client ID first.", Toast.LENGTH_SHORT).show();
                    openConfigureOAuthDialog();
                    return;
                }

                String authUrl = GitHubOAuthService.buildWebAuthorizeUrl(clientId, GitHubOAuthService.DEFAULT_REDIRECT_URI, "vynara_auth");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                startActivity(browserIntent);
                dismiss();
            });
        }

        if (btnSignInDevice != null) {
            btnSignInDevice.setOnClickListener(v -> {
                String clientId = GitHubOAuthService.getClientId(requireContext());
                if (clientId.isEmpty()) {
                    clientId = keyMgr.getGitHubClientId();
                }

                if (clientId == null || clientId.trim().isEmpty()) {
                    Toast.makeText(getContext(), "Please configure OAuth App Client ID first.", Toast.LENGTH_SHORT).show();
                    openConfigureOAuthDialog();
                    return;
                }

                String clientSecret = GitHubOAuthService.getClientSecret(requireContext());

                DeviceFlowDialogFragment dialog = DeviceFlowDialogFragment.newInstance(clientId, clientSecret);
                if (getParentFragmentManager() != null) {
                    dialog.show(getParentFragmentManager(), "DeviceFlowDialog");
                }
                dismiss();
            });
        }

        if (tvEnterOAuthCode != null) {
            tvEnterOAuthCode.setOnClickListener(v -> {
                ManualOAuthDialogFragment dialog = new ManualOAuthDialogFragment();
                if (getParentFragmentManager() != null) {
                    dialog.show(getParentFragmentManager(), "ManualOAuthDialog");
                }
                dismiss();
            });
        }

        if (tvConfigureAppId != null) {
            tvConfigureAppId.setOnClickListener(v -> openConfigureOAuthDialog());
        }

        if (btnDemoGuestMode != null) {
            btnDemoGuestMode.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Exploring in Guest / Demo Mode", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }
    }

    private void openConfigureOAuthDialog() {
        ConfigureOAuthDialogFragment dialog = new ConfigureOAuthDialogFragment();
        if (getParentFragmentManager() != null) {
            dialog.show(getParentFragmentManager(), "ConfigureOAuthDialog");
        }
    }
}