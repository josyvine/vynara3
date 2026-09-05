package com.example.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.cloud.CloudProvider;

public class ApiKeyManager {
    private static final String PREF_NAME = "vynara_secure_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_SELECTED_MODEL = "selected_gemini_model";

    // Cloud Worker MCP Configuration Keys
    private static final String KEY_COMPUTE_PROVIDER = "compute_provider";
    private static final String KEY_GITHUB_REPO = "github_repo";
    private static final String KEY_GITHUB_PAT = "github_pat";
    private static final String KEY_GITHUB_EVENT = "github_event";
    private static final String KEY_HF_SPACE_URL = "hf_space_url";
    private static final String KEY_HF_TOKEN = "hf_token";

    // GitHub OAuth2 Session Keys
    private static final String KEY_GITHUB_CLIENT_ID = "github_client_id";
    private static final String KEY_GITHUB_CLIENT_SECRET = "github_client_secret";
    private static final String KEY_GITHUB_USERNAME = "github_username";
    private static final String KEY_GITHUB_AVATAR_URL = "github_avatar_url";

    // Default Vynara GitHub OAuth App Client ID (Overrideable by user in Settings)
    private static final String DEFAULT_GITHUB_CLIENT_ID = "Ov23liSpdE2NbapBqoqu";

    private SharedPreferences prefs;

    public ApiKeyManager(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            this.prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Phase 26 Alignment: Fallback to private SharedPreferences if Android Keystore is unavailable
            this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveApiKey(String apiKey) {
        if (apiKey != null) {
            prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply();
        }
    }

    public String getApiKey() {
        return prefs.getString(KEY_GEMINI_API_KEY, "");
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    public String getMaskedApiKey() {
        String key = getApiKey();
        if (key == null || key.length() < 8) {
            return "••••••••••••••••";
        }
        return key.substring(0, 4) + "••••••••••••" + key.substring(key.length() - 3);
    }

    public void saveSelectedModel(String modelId) {
        if (modelId != null) {
            prefs.edit().putString(KEY_SELECTED_MODEL, modelId.trim()).apply();
        }
    }

    public String getSelectedModel() {
        // Return blank by default, forcing the app to dynamically register and bind 
        // to a valid model returned from the Google server instead of guessing a deprecated default
        return prefs.getString(KEY_SELECTED_MODEL, "");
    }

    public void clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply();
    }

    // ==========================================
    // Cloud Worker MCP Configuration Methods
    // ==========================================

    public void saveComputeProvider(CloudProvider provider) {
        if (provider != null) {
            prefs.edit().putString(KEY_COMPUTE_PROVIDER, provider.getId()).apply();
        }
    }

    public CloudProvider getComputeProvider() {
        String providerId = prefs.getString(KEY_COMPUTE_PROVIDER, CloudProvider.LOCAL.getId());
        return CloudProvider.fromId(providerId);
    }

    public void saveGitHubConfig(String repo, String pat, String eventName) {
        SharedPreferences.Editor editor = prefs.edit();
        if (repo != null) {
            editor.putString(KEY_GITHUB_REPO, repo.trim());
        }
        if (pat != null) {
            editor.putString(KEY_GITHUB_PAT, pat.trim());
        }
        if (eventName != null && !eventName.trim().isEmpty()) {
            editor.putString(KEY_GITHUB_EVENT, eventName.trim());
        } else {
            editor.putString(KEY_GITHUB_EVENT, "vynara_generate");
        }
        editor.apply();
    }

    public String getGitHubRepo() {
        return prefs.getString(KEY_GITHUB_REPO, "");
    }

    public String getGitHubPat() {
        return prefs.getString(KEY_GITHUB_PAT, "");
    }

    public String getGitHubEvent() {
        return prefs.getString(KEY_GITHUB_EVENT, "vynara_generate");
    }

    public boolean hasGitHubConfig() {
        String repo = getGitHubRepo();
        String pat = getGitHubPat();
        return pat != null && !pat.trim().isEmpty();
    }

    public String getMaskedGitHubPat() {
        String token = getGitHubPat();
        if (token == null || token.length() < 8) {
            return "••••••••••••••••";
        }
        return token.substring(0, 4) + "••••••••••••" + token.substring(token.length() - 3);
    }

    public void saveGitHubOAuthApp(String clientId, String clientSecret) {
        SharedPreferences.Editor editor = prefs.edit();
        if (clientId != null && !clientId.trim().isEmpty()) {
            editor.putString(KEY_GITHUB_CLIENT_ID, clientId.trim());
        }
        if (clientSecret != null) {
            editor.putString(KEY_GITHUB_CLIENT_SECRET, clientSecret.trim());
        }
        editor.apply();
    }

    public String getGitHubClientId() {
        return prefs.getString(KEY_GITHUB_CLIENT_ID, DEFAULT_GITHUB_CLIENT_ID);
    }

    public String getGitHubClientSecret() {
        return prefs.getString(KEY_GITHUB_CLIENT_SECRET, "");
    }

    public void saveGitHubUser(String username, String avatarUrl) {
        SharedPreferences.Editor editor = prefs.edit();
        if (username != null) {
            editor.putString(KEY_GITHUB_USERNAME, username.trim());
        }
        if (avatarUrl != null) {
            editor.putString(KEY_GITHUB_AVATAR_URL, avatarUrl.trim());
        }
        editor.apply();
    }

    public String getGitHubUsername() {
        return prefs.getString(KEY_GITHUB_USERNAME, "");
    }

    public String getGitHubAvatarUrl() {
        return prefs.getString(KEY_GITHUB_AVATAR_URL, "");
    }

    public boolean isGitHubLoggedIn() {
        return hasGitHubConfig() && !getGitHubUsername().isEmpty();
    }

    public void logoutGitHub() {
        prefs.edit()
                .remove(KEY_GITHUB_PAT)
                .remove(KEY_GITHUB_USERNAME)
                .remove(KEY_GITHUB_AVATAR_URL)
                .apply();
    }

    public void saveHuggingFaceConfig(String spaceUrl, String token) {
        SharedPreferences.Editor editor = prefs.edit();
        if (spaceUrl != null) {
            editor.putString(KEY_HF_SPACE_URL, spaceUrl.trim());
        }
        if (token != null) {
            editor.putString(KEY_HF_TOKEN, token.trim());
        }
        editor.apply();
    }

    public String getHuggingFaceSpaceUrl() {
        return prefs.getString(KEY_HF_SPACE_URL, "");
    }

    public String getHuggingFaceToken() {
        return prefs.getString(KEY_HF_TOKEN, "");
    }

    public boolean hasHuggingFaceConfig() {
        String url = getHuggingFaceSpaceUrl();
        return url != null && !url.trim().isEmpty();
    }

    public String getMaskedHuggingFaceToken() {
        String token = getHuggingFaceToken();
        if (token == null || token.length() < 8) {
            return "••••••••••••••••";
        }
        return token.substring(0, 4) + "••••••••••••" + token.substring(token.length() - 3);
    }

    public void clearCloudConfig() {
        prefs.edit()
                .remove(KEY_COMPUTE_PROVIDER)
                .remove(KEY_GITHUB_REPO)
                .remove(KEY_GITHUB_PAT)
                .remove(KEY_GITHUB_EVENT)
                .remove(KEY_GITHUB_USERNAME)
                .remove(KEY_GITHUB_AVATAR_URL)
                .remove(KEY_HF_SPACE_URL)
                .remove(KEY_HF_TOKEN)
                .apply();
    }
}