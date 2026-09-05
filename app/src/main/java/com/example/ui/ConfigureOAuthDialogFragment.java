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

import com.example.R;
import com.example.ai.ApiKeyManager;
import com.example.cloud.GitHubOAuthService;

public class ConfigureOAuthDialogFragment extends DialogFragment {

    private EditText etClientId;
    private EditText etClientSecret;

    public static ConfigureOAuthDialogFragment newInstance() {
        return new ConfigureOAuthDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Vynara2_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_configure_oauth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etClientId = view.findViewById(R.id.et_oauth_client_id);
        etClientSecret = view.findViewById(R.id.et_oauth_client_secret);
        Button btnCancel = view.findViewById(R.id.btn_oauth_cancel);
        Button btnSave = view.findViewById(R.id.btn_oauth_save);

        if (getContext() != null) {
            String savedClientId = GitHubOAuthService.getClientId(requireContext());
            String savedSecret = GitHubOAuthService.getClientSecret(requireContext());

            if (savedClientId.isEmpty()) {
                ApiKeyManager keyMgr = new ApiKeyManager(requireContext());
                savedClientId = keyMgr.getGitHubClientId();
            }

            if (etClientId != null) {
                etClientId.setText(savedClientId);
            }
            if (etClientSecret != null) {
                etClientSecret.setText(savedSecret);
            }
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String clientId = etClientId != null ? etClientId.getText().toString().trim() : "";
                String clientSecret = etClientSecret != null ? etClientSecret.getText().toString().trim() : "";

                if (clientId.isEmpty()) {
                    Toast.makeText(getContext(), "Client ID cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getContext() != null) {
                    GitHubOAuthService.saveOAuthCredentials(requireContext(), clientId, clientSecret);

                    ApiKeyManager keyMgr = new ApiKeyManager(requireContext());
                    keyMgr.saveGitHubConfig(keyMgr.getGitHubRepo(), keyMgr.getGitHubPat(), "vynara_generate");

                    Toast.makeText(getContext(), "GitHub OAuth Credentials saved!", Toast.LENGTH_SHORT).show();
                }

                dismiss();
            });
        }
    }
}