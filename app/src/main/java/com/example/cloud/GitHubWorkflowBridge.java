package com.example.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.utils.VynaraLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GitHubWorkflowBridge {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final long POLLING_INTERVAL_MS = 4000; // 4 seconds interval
    private static final long MAX_POLLING_DURATION_MS = 300000; // 5 minutes timeout
    private static final int MAX_ARTIFACT_RETRY_ATTEMPTS = 8; // 8 retries (20s window for run-specific artifact indexing)
    private static final long ARTIFACT_RETRY_DELAY_MS = 2500; // 2.5 seconds between artifact retries

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public interface WorkflowDispatchCallback {
        void onDispatched(String eventType, String assetId);
        void onError(String errorMessage);
    }

    public interface ArtifactDownloadCallback {
        void onProgress(int percentage, long bytesRead, long totalBytes);
        void onSuccess(File downloadedFile);
        void onError(String errorMessage);
    }

    public interface ConnectionTestCallback {
        void onSuccess(String repoFullName, boolean hasWorkflowAccess);
        void onError(String errorMessage);
    }

    public interface WorkflowPollingCallback {
        void onStatusUpdate(String status, String details);
        void onProgress(int percentage, long bytesRead, long totalBytes);
        void onSuccess(File downloadedFile);
        void onError(String errorMessage);
    }

    public GitHubWorkflowBridge() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // --- Overloaded Context-Aware Methods (Auto-fetch Stored Token) ---

    public void testConnection(Context context, String repository, ConnectionTestCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        testConnection(repository, token, callback);
    }

    public void dispatchGenerationWorkflow(Context context,
                                           String repository,
                                           String eventType,
                                           String assetId,
                                           String bpyScript,
                                           WorkflowDispatchCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        dispatchGenerationWorkflow(repository, token, eventType, assetId, bpyScript, callback);
    }

    public void downloadWorkflowArtifact(Context context,
                                         String repository,
                                         String assetId,
                                         File destinationFile,
                                         ArtifactDownloadCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        downloadWorkflowArtifact(repository, token, assetId, destinationFile, callback);
    }

    public void awaitWorkflowAndDownloadArtifact(Context context,
                                                String repository,
                                                String assetId,
                                                File destinationFile,
                                                WorkflowPollingCallback callback) {
        String token = GitHubOAuthService.getAccessToken(context);
        awaitWorkflowAndDownloadArtifact(repository, token, assetId, destinationFile, callback);
    }

    // --- Standard Methods ---

    public void testConnection(String repository, String personalAccessToken, ConnectionTestCallback callback) {
        if (repository == null || repository.trim().isEmpty()) {
            callback.onError("Repository cannot be empty. Format: owner/repo");
            return;
        }
        if (personalAccessToken == null || personalAccessToken.trim().isEmpty()) {
            callback.onError("GitHub Access Token is empty. Please sign in or provide a token.");
            return;
        }

        String targetUrl = "https://api.github.com/repos/" + repository.trim();

        Request request = new Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer " + personalAccessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network connection failure: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String errorMsg = "GitHub API Error [" + response.code() + "]: " + response.message();
                        mainHandler.post(() -> callback.onError(errorMsg));
                        return;
                    }

                    String jsonStr = responseBody.string();
                    JSONObject repoObj = new JSONObject(jsonStr);
                    String fullName = repoObj.optString("full_name", repository);
                    JSONObject permissions = repoObj.optJSONObject("permissions");
                    boolean hasPushAccess = permissions != null && (permissions.optBoolean("push", false) || permissions.optBoolean("admin", false));

                    mainHandler.post(() -> callback.onSuccess(fullName, hasPushAccess));
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError("Failed to parse repository response: " + ex.getMessage()));
                }
            }
        });
    }

    public void dispatchGenerationWorkflow(String repository,
                                           String personalAccessToken,
                                           String eventType,
                                           String assetId,
                                           String bpyScript,
                                           WorkflowDispatchCallback callback) {
        if (repository == null || repository.trim().isEmpty() || personalAccessToken == null || personalAccessToken.trim().isEmpty()) {
            callback.onError("GitHub credentials are not properly configured.");
            return;
        }

        String dispatchUrl = "https://api.github.com/repos/" + repository.trim() + "/dispatches";

        try {
            JSONObject clientPayload = new JSONObject();
            clientPayload.put("asset_id", assetId);
            clientPayload.put("bpy_script", bpyScript);
            clientPayload.put("timestamp", System.currentTimeMillis());

            JSONObject rootPayload = new JSONObject();
            rootPayload.put("event_type", (eventType != null && !eventType.trim().isEmpty()) ? eventType : "vynara_generate");
            rootPayload.put("client_payload", clientPayload);

            RequestBody body = RequestBody.create(rootPayload.toString(), JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url(dispatchUrl)
                    .header("Authorization", "Bearer " + personalAccessToken.trim())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Vynara-3D-Studio-Android")
                    .post(body)
                    .build();

            VynaraLogger.system("GitHubWorkflowBridge: Dispatching workflow to: " + dispatchUrl + " with assetId: " + assetId);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    VynaraLogger.e("Workflow dispatch failed: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Failed to dispatch workflow: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.code() == 204 || response.isSuccessful()) {
                            VynaraLogger.system("GitHubWorkflowBridge: Workflow dispatched successfully (HTTP " + response.code() + ")");
                            mainHandler.post(() -> callback.onDispatched(eventType, assetId));
                        } else {
                            String err = "GitHub returned HTTP " + response.code() + " (" + response.message() + ")";
                            VynaraLogger.e(err);
                            mainHandler.post(() -> callback.onError(err));
                        }
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception ex) {
            callback.onError("Failed to assemble dispatch payload: " + ex.getMessage());
        }
    }

    public void downloadWorkflowArtifact(String repository,
                                         String personalAccessToken,
                                         String assetId,
                                         File destinationFile,
                                         ArtifactDownloadCallback callback) {
        String artifactsUrl = "https://api.github.com/repos/" + repository.trim() + "/actions/artifacts";

        Request request = new Request.Builder()
                .url(artifactsUrl)
                .header("Authorization", "Bearer " + personalAccessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Artifact search failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        mainHandler.post(() -> callback.onError("Failed to list artifacts: HTTP " + response.code()));
                        return;
                    }

                    String json = responseBody.string();
                    JSONObject root = new JSONObject(json);
                    JSONArray artifacts = root.optJSONArray("artifacts");

                    if (artifacts == null || artifacts.length() == 0) {
                        mainHandler.post(() -> callback.onError("No build artifacts found in repository."));
                        return;
                    }

                    String downloadLocationUrl = null;
                    for (int i = 0; i < artifacts.length(); i++) {
                        JSONObject artifact = artifacts.getJSONObject(i);
                        String name = artifact.optString("name", "");
                        if (name.equalsIgnoreCase(assetId) || name.contains(assetId) || name.equalsIgnoreCase("model")) {
                            downloadLocationUrl = artifact.optString("archive_download_url", null);
                            break;
                        }
                    }

                    if (downloadLocationUrl == null) {
                        mainHandler.post(() -> callback.onError("Artifact matching assetId '" + assetId + "' not ready yet."));
                        return;
                    }

                    executeBinaryDownload(downloadLocationUrl, personalAccessToken, destinationFile, callback);
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError("Failed to parse artifacts list: " + ex.getMessage()));
                }
            }
        });
    }

    public void downloadWorkflowArtifactForRun(String repository,
                                               String personalAccessToken,
                                               long runId,
                                               String assetId,
                                               File destinationFile,
                                               ArtifactDownloadCallback callback) {
        String runArtifactsUrl = "https://api.github.com/repos/" + repository.trim() + "/actions/runs/" + runId + "/artifacts";

        Request request = new Request.Builder()
                .url(runArtifactsUrl)
                .header("Authorization", "Bearer " + personalAccessToken.trim())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Run artifact search failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        mainHandler.post(() -> callback.onError("Failed to list run artifacts: HTTP " + response.code()));
                        return;
                    }

                    String json = responseBody.string();
                    JSONObject root = new JSONObject(json);
                    JSONArray artifacts = root.optJSONArray("artifacts");

                    if (artifacts == null || artifacts.length() == 0) {
                        mainHandler.post(() -> callback.onError("No artifacts produced for Run #" + runId));
                        return;
                    }

                    String downloadLocationUrl = null;
                    for (int i = 0; i < artifacts.length(); i++) {
                        JSONObject artifact = artifacts.getJSONObject(i);
                        String name = artifact.optString("name", "");
                        if (name.equalsIgnoreCase(assetId) || name.contains(assetId) || name.equalsIgnoreCase("model") || artifacts.length() == 1) {
                            downloadLocationUrl = artifact.optString("archive_download_url", null);
                            break;
                        }
                    }

                    if (downloadLocationUrl == null) {
                        mainHandler.post(() -> callback.onError("Target GLB artifact not ready for Run #" + runId));
                        return;
                    }

                    executeBinaryDownload(downloadLocationUrl, personalAccessToken, destinationFile, callback);
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError("Failed to parse run artifacts: " + ex.getMessage()));
                }
            }
        });
    }

    public void awaitWorkflowAndDownloadArtifact(String repository,
                                                String personalAccessToken,
                                                String assetId,
                                                File destinationFile,
                                                WorkflowPollingCallback callback) {
        if (repository == null || repository.trim().isEmpty() || personalAccessToken == null || personalAccessToken.trim().isEmpty()) {
            callback.onError("GitHub credentials are not properly configured.");
            return;
        }

        final long dispatchTimeMs = System.currentTimeMillis();
        VynaraLogger.system("GitHubWorkflowBridge: Starting workflow execution monitoring for assetId: " + assetId);

        final Runnable[] pollRunnable = new Runnable[1];
        pollRunnable[0] = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - dispatchTimeMs > MAX_POLLING_DURATION_MS) {
                    VynaraLogger.e("GitHubWorkflowBridge: Workflow execution timed out after " + (MAX_POLLING_DURATION_MS / 1000) + "s");
                    mainHandler.post(() -> callback.onError("GitHub Actions workflow execution timed out."));
                    return;
                }

                String runsUrl = "https://api.github.com/repos/" + repository.trim() + "/actions/runs?per_page=10";

                Request request = new Request.Builder()
                        .url(runsUrl)
                        .header("Authorization", "Bearer " + personalAccessToken.trim())
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Vynara-3D-Studio-Android")
                        .get()
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        VynaraLogger.e("Workflow status check failed: " + e.getMessage());
                        mainHandler.postDelayed(pollRunnable[0], POLLING_INTERVAL_MS);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody responseBody = response.body()) {
                            if (!response.isSuccessful() || responseBody == null) {
                                VynaraLogger.e("Workflow runs query failed: HTTP " + response.code());
                                mainHandler.postDelayed(pollRunnable[0], POLLING_INTERVAL_MS);
                                return;
                            }

                            String json = responseBody.string();
                            JSONObject root = new JSONObject(json);
                            JSONArray runs = root.optJSONArray("workflow_runs");

                            if (runs != null && runs.length() > 0) {
                                JSONObject targetRun = null;

                                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                                for (int i = 0; i < runs.length(); i++) {
                                    JSONObject r = runs.getJSONObject(i);
                                    String createdAtStr = r.optString("created_at", "");
                                    long runCreatedAtMs = 0;
                                    try {
                                        if (!createdAtStr.isEmpty()) {
                                            runCreatedAtMs = isoFormat.parse(createdAtStr).getTime();
                                        }
                                    } catch (Exception ignored) {}

                                    if (runCreatedAtMs >= (dispatchTimeMs - 15000)) {
                                        targetRun = r;
                                        break;
                                    }
                                }

                                if (targetRun != null) {
                                    String status = targetRun.optString("status", "unknown");
                                    String conclusion = targetRun.optString("conclusion", "null");
                                    long runId = targetRun.optLong("id", 0);

                                    VynaraLogger.system("GitHubWorkflowBridge: Active Run #" + runId + " Status: " + status + " Conclusion: " + conclusion);
                                    mainHandler.post(() -> callback.onStatusUpdate(status, "Run #" + runId + " [" + status + "]"));

                                    if ("completed".equalsIgnoreCase(status)) {
                                        if ("success".equalsIgnoreCase(conclusion)) {
                                            VynaraLogger.system("GitHubWorkflowBridge: Workflow run #" + runId + " succeeded! Downloading run-specific artifact...");
                                            mainHandler.post(() -> callback.onStatusUpdate("downloading", "Downloading GLB artifact..."));

                                            pollAndDownloadArtifact(repository, personalAccessToken, runId, assetId, destinationFile, callback, 1, MAX_ARTIFACT_RETRY_ATTEMPTS);
                                            return;
                                        } else {
                                            String failureMsg = "GitHub Workflow Run #" + runId + " failed with conclusion: " + conclusion;
                                            VynaraLogger.e(failureMsg);
                                            mainHandler.post(() -> callback.onError(failureMsg));
                                            return;
                                        }
                                    }
                                } else {
                                    VynaraLogger.system("GitHubWorkflowBridge: Waiting for new workflow run to be queued by GitHub Actions...");
                                    mainHandler.post(() -> callback.onStatusUpdate("queued", "Waiting for worker to start..."));
                                }
                            }

                            mainHandler.postDelayed(pollRunnable[0], POLLING_INTERVAL_MS);

                        } catch (Exception ex) {
                            VynaraLogger.e("Error parsing workflow status: " + ex.getMessage(), ex);
                            mainHandler.postDelayed(pollRunnable[0], POLLING_INTERVAL_MS);
                        }
                    }
                });
            }
        };

        mainHandler.post(pollRunnable[0]);
    }

    private void pollAndDownloadArtifact(String repository,
                                         String personalAccessToken,
                                         long runId,
                                         String assetId,
                                         File destinationFile,
                                         WorkflowPollingCallback callback,
                                         int attempt,
                                         int maxAttempts) {
        downloadWorkflowArtifactForRun(repository, personalAccessToken, runId, assetId, destinationFile, new ArtifactDownloadCallback() {
            @Override
            public void onProgress(int percentage, long bytesRead, long totalBytes) {
                callback.onProgress(percentage, bytesRead, totalBytes);
            }

            @Override
            public void onSuccess(File downloadedFile) {
                VynaraLogger.system("GitHubWorkflowBridge: Artifact extracted successfully: " + downloadedFile.getAbsolutePath());
                callback.onSuccess(downloadedFile);
            }

            @Override
            public void onError(String errorMessage) {
                if (attempt < maxAttempts) {
                    VynaraLogger.system("GitHubWorkflowBridge: Waiting for run artifact indexing (attempt " + attempt + "/" + maxAttempts + ")...");
                    mainHandler.post(() -> callback.onStatusUpdate("indexing", "Waiting for artifact indexing (" + attempt + "/" + maxAttempts + ")..."));
                    mainHandler.postDelayed(() -> pollAndDownloadArtifact(repository, personalAccessToken, runId, assetId, destinationFile, callback, attempt + 1, maxAttempts), ARTIFACT_RETRY_DELAY_MS);
                } else {
                    VynaraLogger.e("GitHubWorkflowBridge: Artifact download failed: " + errorMessage);
                    callback.onError(errorMessage);
                }
            }
        });
    }

    private void executeBinaryDownload(String downloadUrl,
                                       String token,
                                       File destinationFile,
                                       ArtifactDownloadCallback callback) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer " + token.trim())
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to download artifact binary: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError("Artifact download failed: HTTP " + response.code()));
                    return;
                }

                ResponseBody body = response.body();
                long totalBytes = body.contentLength();

                File tempZipFile = new File(destinationFile.getParentFile(), destinationFile.getName() + ".zip");

                try (InputStream inputStream = body.byteStream();
                     FileOutputStream outputStream = new FileOutputStream(tempZipFile)) {

                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (totalBytes > 0) {
                            int progress = (int) ((totalBytesRead * 100) / totalBytes);
                            long currentRead = totalBytesRead;
                            mainHandler.post(() -> callback.onProgress(progress, currentRead, totalBytes));
                        }
                    }
                    outputStream.flush();

                    boolean extracted = extractGlbFromZip(tempZipFile, destinationFile);
                    if (tempZipFile.exists()) {
                        tempZipFile.delete();
                    }

                    if (extracted && destinationFile.exists() && destinationFile.length() > 0) {
                        mainHandler.post(() -> callback.onSuccess(destinationFile));
                    } else {
                        mainHandler.post(() -> callback.onError("Extracted file is missing or invalid."));
                    }

                } catch (Exception ex) {
                    if (tempZipFile.exists()) {
                        tempZipFile.delete();
                    }
                    mainHandler.post(() -> callback.onError("Error saving artifact: " + ex.getMessage()));
                }
            }
        });
    }

    /**
     * Extracts the 3D model (.glb), preview image (.png), AND streams Blender's internal A-to-Z log.
     */
    private boolean extractGlbFromZip(File zipFile, File destinationGlbFile) {
        boolean glbFound = false;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName().toLowerCase();

                if (fileName.endsWith(".glb") || fileName.endsWith(".gltf")) {
                    if (destinationGlbFile.getParentFile() != null && !destinationGlbFile.getParentFile().exists()) {
                        destinationGlbFile.getParentFile().mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(destinationGlbFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                    }
                    glbFound = true;
                } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    // Extract cinematic Cycles still render alongside GLB
                    String renderName = destinationGlbFile.getName();
                    int dotIdx = renderName.lastIndexOf('.');
                    String baseName = (dotIdx > 0) ? renderName.substring(0, dotIdx) : renderName;
                    File destinationImgFile = new File(destinationGlbFile.getParentFile(), baseName + ".png");

                    try (FileOutputStream fos = new FileOutputStream(destinationImgFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                        VynaraLogger.system("GitHubWorkflowBridge: Extracted cinematic Cycles preview image: " + destinationImgFile.getName());
                    }
                } else if (fileName.contains("blender_execution.log") || fileName.contains("error.txt") || fileName.endsWith(".log")) {
                    // Stream Blender's internal A-to-Z execution output directly to VynaraLogger
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String logContent = baos.toString(StandardCharsets.UTF_8.name());

                    VynaraLogger.system("========== BLENDER WORKER INTERNAL LOG START ==========");
                    String[] lines = logContent.split("\\r?\\n");
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        if (line.toLowerCase().contains("error") || line.toLowerCase().contains("exception")) {
                            VynaraLogger.e("[BLENDER_WORKER] " + line.trim());
                        } else {
                            VynaraLogger.cloud("[BLENDER_WORKER] " + line.trim());
                        }
                    }
                    VynaraLogger.system("========== BLENDER WORKER INTERNAL LOG END ==========");
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            VynaraLogger.e("ZIP extraction error: " + e.getMessage(), e);
        }
        return glbFound;
    }

    /**
     * Helper to retrieve the rendered Cycles preview image if it was extracted alongside the GLB.
     */
    public static File getAssociatedRenderImage(File glbFile) {
        if (glbFile == null || glbFile.getParentFile() == null) return null;
        String name = glbFile.getName();
        int dotIdx = name.lastIndexOf('.');
        String baseName = (dotIdx > 0) ? name.substring(0, dotIdx) : name;
        File img = new File(glbFile.getParentFile(), baseName + ".png");
        return (img.exists() && img.length() > 0) ? img : null;
    }
}