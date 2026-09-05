package com.example.cloud;

import android.os.Handler;
import android.os.Looper;

import com.example.utils.VynaraLogger;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HuggingFaceBridge {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType OCTET_STREAM_MEDIA_TYPE = MediaType.parse("application/octet-stream");
    private static final int DEFAULT_TIMEOUT_SECONDS = 90;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public interface GenerationCallback {
        void onProgress(int percentage, long bytesRead, long totalBytes);
        void onSuccess(File downloadedGlbFile);
        void onError(String errorMessage);
    }

    public interface ConnectionTestCallback {
        void onSuccess(String statusMessage);
        void onError(String errorMessage);
    }

    public HuggingFaceBridge() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void testConnection(String spaceUrl, String userToken, ConnectionTestCallback callback) {
        if (spaceUrl == null || spaceUrl.trim().isEmpty()) {
            callback.onError("Hugging Face Space URL cannot be empty.");
            return;
        }

        String normalizedUrl = normalizeUrl(spaceUrl);
        String targetUrl = normalizedUrl + "/docs";

        Request.Builder requestBuilder = new Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get();

        if (userToken != null && !userToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + userToken.trim());
        }

        httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Failed to connect to Space: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() || response.code() == 200 || response.code() == 307) {
                        mainHandler.post(() -> callback.onSuccess("Hugging Face Space is online and responding."));
                    } else {
                        mainHandler.post(() -> callback.onError("Space returned HTTP " + response.code() + " (" + response.message() + ")"));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    public void generateAsset(String spaceUrl,
                              String userToken,
                              String bpyScript,
                              File destinationGlbFile,
                              GenerationCallback callback) {
        if (spaceUrl == null || spaceUrl.trim().isEmpty()) {
            callback.onError("Hugging Face Space URL is not configured.");
            return;
        }

        String normalizedUrl = normalizeUrl(spaceUrl);
        String targetUrl = normalizedUrl + "/generate";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("script", bpyScript);

            RequestBody requestBody = RequestBody.create(jsonObject.toString(), JSON_MEDIA_TYPE);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Vynara-3D-Studio-Android")
                    .header("Accept", "model/gltf-binary, application/octet-stream")
                    .post(requestBody);

            if (userToken != null && !userToken.trim().isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + userToken.trim());
            }

            VynaraLogger.system("HuggingFaceBridge: Sending generation request to Hugging Face: " + targetUrl);

            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    VynaraLogger.e("HuggingFaceBridge: Generation request failed: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Request error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        String errorDetail = "Server returned HTTP " + response.code();
                        if (response.body() != null) {
                            try {
                                errorDetail += ": " + response.body().string();
                            } catch (Exception ignored) {
                            }
                        }
                        String finalError = errorDetail;
                        mainHandler.post(() -> callback.onError(finalError));
                        return;
                    }

                    streamResponseToFile(response.body(), destinationGlbFile, callback);
                }
            });

        } catch (Exception ex) {
            callback.onError("Failed to create request payload: " + ex.getMessage());
        }
    }

    public void autoRigMesh(String spaceUrl,
                            String userToken,
                            File sourceGlbFile,
                            String rigType,
                            File destinationGlbFile,
                            GenerationCallback callback) {
        if (spaceUrl == null || spaceUrl.trim().isEmpty()) {
            callback.onError("Hugging Face Space URL is not configured.");
            return;
        }
        if (sourceGlbFile == null || !sourceGlbFile.exists()) {
            callback.onError("Source mesh file does not exist.");
            return;
        }

        String normalizedUrl = normalizeUrl(spaceUrl);
        String targetUrl = normalizedUrl + "/auto_rig";

        RequestBody fileBody = RequestBody.create(sourceGlbFile, OCTET_STREAM_MEDIA_TYPE);
        RequestBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("rig_type", rigType != null ? rigType : "humanoid")
                .addFormDataPart("file", sourceGlbFile.getName(), fileBody)
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .header("Accept", "model/gltf-binary, application/octet-stream")
                .post(multipartBody);

        if (userToken != null && !userToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + userToken.trim());
        }

        VynaraLogger.system("HuggingFaceBridge: Dispatching auto-rig task to: " + targetUrl);

        httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                VynaraLogger.e("HuggingFaceBridge: Auto-rig request failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Auto-rig request failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = "Auto-rig HTTP " + response.code();
                    if (response.body() != null) {
                        try {
                            errorMsg += ": " + response.body().string();
                        } catch (Exception ignored) {
                        }
                    }
                    String finalMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalMsg));
                    return;
                }

                streamResponseToFile(response.body(), destinationGlbFile, callback);
            }
        });
    }

    private void streamResponseToFile(ResponseBody body, File destinationFile, GenerationCallback callback) {
        if (destinationFile.getParentFile() != null && !destinationFile.getParentFile().exists()) {
            destinationFile.getParentFile().mkdirs();
        }

        long totalBytes = body.contentLength();

        try (InputStream inputStream = body.byteStream();
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

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

            if (destinationFile.exists() && destinationFile.length() > 0) {
                mainHandler.post(() -> callback.onSuccess(destinationFile));
            } else {
                mainHandler.post(() -> callback.onError("Downloaded GLB file is empty."));
            }

        } catch (Exception ex) {
            VynaraLogger.e("HuggingFaceBridge: Error writing GLB output stream: " + ex.getMessage(), ex);
            mainHandler.post(() -> callback.onError("Disk write error: " + ex.getMessage()));
        }
    }

    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}