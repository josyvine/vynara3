package com.example;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.ai.ApiKeyManager;
import com.example.cloud.CloudProvider;
import com.example.cloud.GitHubOAuthService;
import com.example.runtime.ProjectRuntime;
import com.example.ui.AssetsFragment;
import com.example.ui.CreateFragment;
import com.example.ui.InAppFloatingConsoleView;
import com.example.ui.LandingFragment;
import com.example.ui.ProductionFragment;
import com.example.ui.ProjectsFragment;
import com.example.ui.SettingsFragment;
import com.example.ui.StudioFragment;
import com.example.utils.VynaraLogger;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ProjectRuntime projectRuntime;
    private GitHubOAuthService gitHubOAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Phase 1 Alignment: Initialize unified shared 3D project runtime instance
        projectRuntime = ProjectRuntime.getInstance(getApplicationContext());
        gitHubOAuthService = new GitHubOAuthService();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            loadFragment(new LandingFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create) {
                loadFragment(new CreateFragment());
                return true;
            } else if (id == R.id.nav_projects) {
                loadFragment(new ProjectsFragment());
                return true;
            } else if (id == R.id.nav_assets) {
                loadFragment(new AssetsFragment());
                return true;
            } else if (id == R.id.nav_studio) {
                loadFragment(new StudioFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        // Instantiate and dynamically attach the custom in-app overlay diagnostic console
        InAppFloatingConsoleView floatingConsole = new InAppFloatingConsoleView(this);
        
        FrameLayout.LayoutParams consoleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        consoleParams.gravity = Gravity.TOP | Gravity.END;
        consoleParams.topMargin = 120; // Safe vertical boundary offset from system status bar
        consoleParams.rightMargin = 20;

        ViewGroup rootContainer = findViewById(android.R.id.content);
        if (rootContainer != null) {
            rootContainer.addView(floatingConsole, consoleParams);
        }

        // Write boot diagnostic log line & active compute provider state
        ApiKeyManager keyManager = projectRuntime.getAIOrchestrator().getApiKeyManager();
        CloudProvider provider = keyManager.getComputeProvider();
        VynaraLogger.system("Vynara engine initialized cleanly. Active compute pipeline: " + provider.getDisplayName());

        // Handle initial intent for OAuth callback if opened via link
        handleOAuthIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOAuthIntent(intent);
    }

    private void handleOAuthIntent(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        Uri data = intent.getData();
        if ("vynara".equalsIgnoreCase(data.getScheme()) && "oauth-callback".equalsIgnoreCase(data.getHost())) {
            String code = data.getQueryParameter("code");
            String error = data.getQueryParameter("error");

            if (error != null) {
                String errorDesc = data.getQueryParameter("error_description");
                VynaraLogger.e("GitHub OAuth denied: " + (errorDesc != null ? errorDesc : error));
                Toast.makeText(this, "GitHub sign-in failed: " + error, Toast.LENGTH_LONG).show();
                return;
            }

            if (code != null && !code.trim().isEmpty()) {
                VynaraLogger.system("GitHub OAuth code received from browser redirect. Starting token exchange...");
                ApiKeyManager keyMgr = getProjectRuntime().getAIOrchestrator().getApiKeyManager();
                String clientId = keyMgr.getGitHubClientId();
                String clientSecret = keyMgr.getGitHubClientSecret();

                gitHubOAuthService.exchangeCodeForToken(clientId, clientSecret, code, GitHubOAuthService.DEFAULT_REDIRECT_URI, new GitHubOAuthService.OAuthTokenCallback() {
                    @Override
                    public void onSuccess(String accessToken, String tokenType, String scope) {
                        gitHubOAuthService.fetchUserProfile(accessToken, new GitHubOAuthService.UserProfileCallback() {
                            @Override
                            public void onSuccess(String login, String name, String avatarUrl) {
                                keyMgr.saveGitHubUser(login, avatarUrl);
                                keyMgr.saveGitHubConfig(keyMgr.getGitHubRepo(), accessToken, "vynara_generate");
                                keyMgr.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);

                                VynaraLogger.system("Authenticated as GitHub user: @" + login);
                                Toast.makeText(MainActivity.this, "Signed in as @" + login + " successfully!", Toast.LENGTH_LONG).show();
                                navigateToSettings();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                keyMgr.saveGitHubConfig(keyMgr.getGitHubRepo(), accessToken, "vynara_generate");
                                keyMgr.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);
                                Toast.makeText(MainActivity.this, "GitHub connected!", Toast.LENGTH_SHORT).show();
                                navigateToSettings();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        VynaraLogger.e("GitHub OAuth exchange failed: " + errorMessage);
                        Toast.makeText(MainActivity.this, "OAuth Exchange Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToCreate() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_create);
        }
    }

    public void navigateToStudio() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_studio);
        }
    }

    public void navigateToSettings() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        }
    }

    public void startProduction(String prompt) {
        startProduction(prompt, "Photorealistic", "OpenGL ES / GLTF", new ArrayList<>());
    }

    public void startProduction(String prompt, String style, String targetEngine, List<String> referenceImageUris) {
        loadFragment(ProductionFragment.newInstance(prompt, style, targetEngine, referenceImageUris));
    }

    public ProjectRuntime getProjectRuntime() {
        if (projectRuntime == null) {
            projectRuntime = ProjectRuntime.getInstance(getApplicationContext());
        }
        return projectRuntime;
    }
}