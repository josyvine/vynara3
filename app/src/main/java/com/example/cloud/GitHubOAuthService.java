package com.example.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.ai.ApiKeyManager;
import com.example.cloud.models.DeviceCodeResponse;
import com.example.utils.VynaraLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GitHubOAuthService {
    private static final String OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String OAUTH_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String USER_API_URL = "https://api.github.com/user";
    private static final String USER_REPOS_URL = "https://api.github.com/user/repos?per_page=100&sort=updated";

    public static final String DEFAULT_REDIRECT_URI = "vynara://oauth-callback";
    public static final String DEFAULT_SCOPES = "repo,workflow,user";

    private static final String PREFS_NAME = "vynara_github_auth_prefs";
    private static final String KEY_CLIENT_ID = "github_client_id";
    private static final String KEY_CLIENT_SECRET = "github_client_secret";
    private static final String KEY_ACCESS_TOKEN = "github_access_token";
    private static final String KEY_USER_LOGIN = "github_user_login";
    private static final String KEY_USER_NAME = "github_user_name";
    private static final String KEY_AVATAR_URL = "github_avatar_url";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String WORKFLOW_YAML_CONTENT =
            "name: Vynara Blender Worker\n" +
            "\n" +
            "on:\n" +
            "  repository_dispatch:\n" +
            "    types: [vynara_generate]\n" +
            "\n" +
            "jobs:\n" +
            "  build_3d_asset:\n" +
            "    runs-on: ubuntu-latest\n" +
            "\n" +
            "    steps:\n" +
            "      - name: Checkout Code\n" +
            "        uses: actions/checkout@v4\n" +
            "\n" +
            "      - name: Restore Blender Cache\n" +
            "        id: blender-cache\n" +
            "        uses: actions/cache@v4\n" +
            "        with:\n" +
            "          path: /opt/blender\n" +
            "          key: blender-4.2-linux-x64\n" +
            "\n" +
            "      - name: Download Blender Binary\n" +
            "        if: steps.blender-cache.outputs.cache-hit != 'true'\n" +
            "        run: |\n" +
            "          sudo mkdir -p /opt/blender\n" +
            "          curl -sL https://download.blender.org/release/Blender4.2/blender-4.2.0-linux-x64.tar.xz | sudo tar -xJ --strip-components=1 -C /opt/blender\n" +
            "\n" +
            "      - name: Execute Blender Python Script\n" +
            "        run: |\n" +
            "          mkdir -p output\n" +
            "          echo \"${{ github.event.client_payload.bpy_script }}\" > run_task.py\n" +
            "          /opt/blender/blender -b -P run_task.py -- output/model.glb\n" +
            "\n" +
            "      - name: Upload Finished Model\n" +
            "        uses: actions/upload-artifact@v4\n" +
            "        with:\n" +
            "          name: ${{ github.event.client_payload.asset_id }}\n" +
            "          path: output/model.glb\n";

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private volatile boolean isPollingCancelled = false;

    public interface OAuthTokenCallback {
        void onSuccess(String accessToken, String tokenType, String scope);
        void onError(String errorMessage);
    }

    public interface DeviceCodeCallback {
        void onDeviceCodeReceived(DeviceCodeResponse response);
        void onError(String errorMessage);
    }

    public interface DevicePollingCallback {
        void onTokenReceived(String accessToken);
        void onPending(String status);
        void onError(String errorMessage);
    }

    public interface UserProfileCallback {
        void onSuccess(String login, String name, String avatarUrl);
        void onError(String errorMessage);
    }

    public interface UserReposCallback {
        void onSuccess(List<String> repoFullNames);
        void onError(String errorMessage);
    }

    public interface ProvisionCallback {
        void onSuccess(String repoFullName);
        void onError(String errorMessage);
    }

    public GitHubOAuthService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // --- SharedPreferences Helpers for Credential & Token Storage ---

    public static void saveOAuthCredentials(Context context, String clientId, String clientSecret) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CLIENT_ID, clientId != null ? clientId.trim() : "")
                .putString(KEY_CLIENT_SECRET, clientSecret != null ? clientSecret.trim() : "")
                .apply();
    }

    public static String getClientId(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CLIENT_ID, "");
    }

    public static String getClientSecret(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CLIENT_SECRET, "");
    }

    public static void saveAccessToken(Context context, String token) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token != null ? token.trim() : "")
                .apply();
    }

    public static String getAccessToken(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }

    public static void saveUserProfile(Context context, String login, String name, String avatarUrl) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_LOGIN, login != null ? login.trim() : "")
                .putString(KEY_USER_NAME, name != null ? name.trim() : "")
                .putString(KEY_AVATAR_URL, avatarUrl != null ? avatarUrl.trim() : "")
                .apply();
    }

    public static String getUserLogin(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_LOGIN, "");
    }

    public static String getUserName(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }

    public static String getAvatarUrl(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AVATAR_URL, "");
    }

    public static boolean isLoggedIn(Context context) {
        String token = getAccessToken(context);
        return token != null && !token.trim().isEmpty();
    }

    public static void clearAuth(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_USER_LOGIN)
                .remove(KEY_USER_NAME)
                .remove(KEY_AVATAR_URL)
                .apply();
    }

    // --- Web OAuth Methods ---

    public static String buildWebAuthorizeUrl(String clientId, String redirectUri, String state) {
        Uri.Builder builder = Uri.parse(OAUTH_AUTHORIZE_URL).buildUpon();
        builder.appendQueryParameter("client_id", clientId != null ? clientId.trim() : "");
        builder.appendQueryParameter("redirect_uri", redirectUri != null ? redirectUri.trim() : DEFAULT_REDIRECT_URI);
        builder.appendQueryParameter("scope", DEFAULT_SCOPES);
        if (state != null && !state.trim().isEmpty()) {
            builder.appendQueryParameter("state", state.trim());
        }
        return builder.build().toString();
    }

    public void exchangeCodeForToken(String clientId,
                                    String clientSecret,
                                    String code,
                                    String redirectUri,
                                    OAuthTokenCallback callback) {
        if (clientId == null || clientId.trim().isEmpty()) {
            callback.onError("GitHub Client ID is missing.");
            return;
        }
        if (code == null || code.trim().isEmpty()) {
            callback.onError("Authorization code is empty.");
            return;
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("code", code.trim())
                .add("redirect_uri", redirectUri != null ? redirectUri.trim() : DEFAULT_REDIRECT_URI);

        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            formBuilder.add("client_secret", clientSecret.trim());
        }

        Request request = new Request.Builder()
                .url(OAUTH_TOKEN_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .post(formBuilder.build())
                .build();

        VynaraLogger.system("GitHubOAuthService: Exchanging OAuth code for Access Token...");

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                VynaraLogger.e("OAuth token exchange network failure: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Network error during token exchange: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        String err = "GitHub returned HTTP " + response.code();
                        mainHandler.post(() -> callback.onError(err));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    if (json.has("error")) {
                        String errorDesc = json.optString("error_description", json.optString("error", "Unknown OAuth error"));
                        mainHandler.post(() -> callback.onError(errorDesc));
                        return;
                    }

                    String accessToken = json.optString("access_token", "");
                    String tokenType = json.optString("token_type", "bearer");
                    String scope = json.optString("scope", "");

                    if (!accessToken.isEmpty()) {
                        VynaraLogger.system("GitHubOAuthService: Token exchange successful.");
                        mainHandler.post(() -> callback.onSuccess(accessToken, tokenType, scope));
                    } else {
                        mainHandler.post(() -> callback.onError("Response did not contain an access_token."));
                    }
                } catch (Exception e) {
                    VynaraLogger.e("Failed to parse token response: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    // --- Device Flow Methods ---

    public void requestDeviceCode(String clientId, DeviceCodeCallback callback) {
        requestDeviceCode(clientId, null, callback);
    }

    public void requestDeviceCode(String clientId, String clientSecret, DeviceCodeCallback callback) {
        if (clientId == null || clientId.trim().isEmpty()) {
            callback.onError("GitHub Client ID is required for Device Flow.");
            return;
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("scope", DEFAULT_SCOPES);

        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            formBuilder.add("client_secret", clientSecret.trim());
        }

        Request request = new Request.Builder()
                .url(DEVICE_CODE_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .post(formBuilder.build())
                .build();

        VynaraLogger.system("GitHubOAuthService: Requesting Device Flow code...");

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                VynaraLogger.e("Device code request failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Network failure: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onError("GitHub returned HTTP " + response.code()));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    if (json.has("error")) {
                        String errorDesc = json.optString("error_description", json.optString("error", "Failed to start Device Flow"));
                        mainHandler.post(() -> callback.onError(errorDesc));
                        return;
                    }

                    DeviceCodeResponse codeResponse = DeviceCodeResponse.fromJson(json);
                    if (codeResponse.isValid()) {
                        VynaraLogger.system("GitHubOAuthService: Received User Code: " + codeResponse.getUserCode());
                        mainHandler.post(() -> callback.onDeviceCodeReceived(codeResponse));
                    } else {
                        mainHandler.post(() -> callback.onError("Received invalid Device Code payload from GitHub."));
                    }
                } catch (Exception e) {
                    VynaraLogger.e("Error parsing Device Code response: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    public void startDeviceFlowPolling(String clientId,
                                       String deviceCode,
                                       int intervalSeconds,
                                       int expiresInSeconds,
                                       DevicePollingCallback callback) {
        startDeviceFlowPolling(clientId, null, deviceCode, intervalSeconds, expiresInSeconds, callback);
    }

    public void startDeviceFlowPolling(String clientId,
                                       String clientSecret,
                                       String deviceCode,
                                       int intervalSeconds,
                                       int expiresInSeconds,
                                       DevicePollingCallback callback) {
        isPollingCancelled = false;
        final long startTime = System.currentTimeMillis();
        final long maxDurationMs = (expiresInSeconds > 0 ? expiresInSeconds : 900) * 1000L;
        final int intervalMs = Math.max(intervalSeconds, 5) * 1000;

        final Runnable[] pollRunnable = new Runnable[1];

        pollRunnable[0] = new Runnable() {
            @Override
            public void run() {
                if (isPollingCancelled) {
                    mainHandler.post(() -> callback.onError("Device login cancelled."));
                    return;
                }

                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    mainHandler.post(() -> callback.onError("Device code has expired. Please try again."));
                    return;
                }

                pollDeviceTokenOnce(clientId, clientSecret, deviceCode, new DevicePollingCallback() {
                    @Override
                    public void onTokenReceived(String accessToken) {
                        mainHandler.post(() -> callback.onTokenReceived(accessToken));
                    }

                    @Override
                    public void onPending(String status) {
                        mainHandler.post(() -> callback.onPending(status));
                        if (!isPollingCancelled && pollRunnable[0] != null) {
                            mainHandler.postDelayed(pollRunnable[0], intervalMs);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> callback.onError(errorMessage));
                    }
                });
            }
        };

        mainHandler.postDelayed(pollRunnable[0], intervalMs);
    }

    public void cancelDeviceFlowPolling() {
        this.isPollingCancelled = true;
    }

    private void pollDeviceTokenOnce(String clientId, String clientSecret, String deviceCode, DevicePollingCallback callback) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("client_id", clientId.trim())
                .add("device_code", deviceCode.trim())
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            formBuilder.add("client_secret", clientSecret.trim());
        }

        Request request = new Request.Builder()
                .url(OAUTH_TOKEN_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .post(formBuilder.build())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onPending("Connecting to GitHub...");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        callback.onPending("Waiting for user authorization...");
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    if (json.has("access_token")) {
                        String token = json.getString("access_token");
                        callback.onTokenReceived(token);
                        return;
                    }

                    String error = json.optString("error", "");
                    if ("authorization_pending".equalsIgnoreCase(error)) {
                        callback.onPending("Waiting for authorization on GitHub...");
                    } else if ("slow_down".equalsIgnoreCase(error)) {
                        callback.onPending("Slow down requested by GitHub...");
                    } else if ("expired_token".equalsIgnoreCase(error)) {
                        callback.onError("Device code has expired.");
                    } else if ("access_denied".equalsIgnoreCase(error)) {
                        callback.onError("Access was denied by the user.");
                    } else {
                        String errorDesc = json.optString("error_description", error);
                        callback.onError(errorDesc.isEmpty() ? "Unknown authorization error." : errorDesc);
                    }
                } catch (Exception e) {
                    callback.onPending("Processing...");
                }
            }
        });
    }

    // --- User Profile & Repository Operations ---

    public void fetchUserProfile(String accessToken, UserProfileCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Access token is empty.");
            return;
        }

        Request request = new Request.Builder()
                .url(USER_API_URL)
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to fetch user profile: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onError("HTTP " + response.code() + " fetching profile"));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONObject json = new JSONObject(jsonStr);

                    String login = json.optString("login", "");
                    String name = json.optString("name", login);
                    String avatarUrl = json.optString("avatar_url", "");

                    mainHandler.post(() -> callback.onSuccess(login, name, avatarUrl));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Error parsing profile: " + e.getMessage()));
                }
            }
        });
    }

    public void fetchUserRepositories(String accessToken, UserReposCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Access token is empty.");
            return;
        }

        Request request = new Request.Builder()
                .url(USER_REPOS_URL)
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to fetch repositories: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onError("HTTP " + response.code() + " fetching repos"));
                        return;
                    }

                    String jsonStr = body.string();
                    JSONArray array = new JSONArray(jsonStr);
                    List<String> repoNames = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject repo = array.getJSONObject(i);
                        String fullName = repo.optString("full_name", "");
                        if (!fullName.isEmpty()) {
                            repoNames.add(fullName);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(repoNames));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Error parsing repositories: " + e.getMessage()));
                }
            }
        });
    }

    // --- Method 2: Automatic Background Workspace & Workflow Provisioning ---

    public void provisionUserWorkspace(Context context, String accessToken, String userLogin, ProvisionCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty() || userLogin == null || userLogin.trim().isEmpty()) {
            callback.onError("Invalid credentials for provisioning.");
            return;
        }

        final String repoFullName = userLogin.trim() + "/vynara2";
        final String repoCheckUrl = "https://api.github.com/repos/" + repoFullName;

        VynaraLogger.system("GitHubOAuthService: Checking if repository " + repoFullName + " exists...");

        Request checkRequest = new Request.Builder()
                .url(repoCheckUrl)
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(checkRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Repo check failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    response.close();
                    VynaraLogger.system("GitHubOAuthService: Repo " + repoFullName + " exists. Checking workflow file...");
                    checkAndCommitWorkflow(context, accessToken, userLogin, repoFullName, callback);
                } else if (response.code() == 404) {
                    response.close();
                    VynaraLogger.system("GitHubOAuthService: Repo " + repoFullName + " missing. Creating private repository...");
                    createPrivateRepo(context, accessToken, userLogin, repoFullName, callback);
                } else {
                    int status = response.code();
                    response.close();
                    mainHandler.post(() -> callback.onError("GitHub API returned HTTP " + status + " during repo check"));
                }
            }
        });
    }

    private void createPrivateRepo(Context context, String accessToken, String userLogin, String repoFullName, ProvisionCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", "vynara2");
            json.put("description", "Vynara 3D Studio Cloud Render Workspace");
            json.put("private", true);
            json.put("auto_init", true);

            RequestBody body = RequestBody.create(json.toString(), JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url("https://api.github.com/user/repos")
            .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Vynara-3D-Studio-Android")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Failed to create private repo: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() || response.code() == 201) {
                        response.close();
                        VynaraLogger.system("GitHubOAuthService: Private repository created successfully. Waiting for init...");
                        mainHandler.postDelayed(() -> checkAndCommitWorkflow(context, accessToken, userLogin, repoFullName, callback), 2000);
                    } else {
                        int code = response.code();
                        response.close();
                        mainHandler.post(() -> callback.onError("GitHub returned HTTP " + code + " creating repository"));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("JSON creation error: " + e.getMessage());
        }
    }

    private void checkAndCommitWorkflow(Context context, String accessToken, String userLogin, String repoFullName, ProvisionCallback callback) {
        String workflowCheckUrl = "https://api.github.com/repos/" + repoFullName + "/contents/.github/workflows/vynara_worker.yml";

        Request request = new Request.Builder()
                .url(workflowCheckUrl)
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                commitWorkflowFile(context, accessToken, repoFullName, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    response.close();
                    VynaraLogger.system("GitHubOAuthService: Workflow file already exists in " + repoFullName);
                    saveAndFinishProvisioning(context, accessToken, userLogin, repoFullName, callback);
                } else {
                    response.close();
                    VynaraLogger.system("GitHubOAuthService: Workflow file missing. Pushing vynara_worker.yml...");
                    commitWorkflowFile(context, accessToken, repoFullName, callback);
                }
            }
        });
    }

    private void commitWorkflowFile(Context context, String accessToken, String repoFullName, ProvisionCallback callback) {
        try {
            String encodedYaml = Base64.encodeToString(WORKFLOW_YAML_CONTENT.getBytes(), Base64.NO_WRAP);

            JSONObject json = new JSONObject();
            json.put("message", "Initialize Vynara Blender worker workflow");
            json.put("content", encodedYaml);
            json.put("branch", "main");

            RequestBody body = RequestBody.create(json.toString(), JSON_MEDIA_TYPE);

            String putUrl = "https://api.github.com/repos/" + repoFullName + "/contents/.github/workflows/vynara_worker.yml";

            Request request = new Request.Builder()
                    .url(putUrl)
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Vynara-3D-Studio-Android")
                    .put(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Failed to push workflow file: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int code = response.code();
                    response.close();
                    if (code == 200 || code == 201) {
                        VynaraLogger.system("GitHubOAuthService: Workflow file successfully committed to " + repoFullName);
                        saveAndFinishProvisioning(context, accessToken, repoFullName.split("/")[0], repoFullName, callback);
                    } else {
                        mainHandler.post(() -> callback.onError("GitHub returned HTTP " + code + " committing workflow"));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Error assembling workflow payload: " + e.getMessage());
        }
    }

    private void saveAndFinishProvisioning(Context context, String accessToken, String userLogin, String repoFullName, ProvisionCallback callback) {
        if (context != null) {
            ApiKeyManager keyMgr = new ApiKeyManager(context);
            keyMgr.saveGitHubUser(userLogin, getAvatarUrl(context));
            keyMgr.saveGitHubConfig(repoFullName, accessToken, "vynara_generate");
            keyMgr.saveComputeProvider(CloudProvider.GITHUB_ACTIONS);
        }

        mainHandler.post(() -> callback.onSuccess(repoFullName));
    }
}