package com.example.asset;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.utils.VynaraLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AssetManager {
    private static final String CACHE_SUBDIR = "models_cache";

    private final List<Asset> assets = new ArrayList<>();
    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public interface OnAssetReadyListener {
        void onProgress(int percentage);
        void onSuccess(File assetFile);
        void onError(String message);
    }

    public AssetManager() {
        // Phase 15 Alignment: Purged hardcoded mock sample assets.
        // The asset library is populated dynamically from generated 3D files stored locally.
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setAssets(List<Asset> loadedAssets) {
        assets.clear();
        if (loadedAssets != null) {
            assets.addAll(loadedAssets);
        }
    }

    public List<Asset> getAssets() { 
        return assets; 
    }

    public void addAsset(Asset a) {
        if (a != null && !containsAsset(a.getId())) {
            assets.add(0, a); // Add newest generated assets to the top
        }
    }

    public boolean removeAsset(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }
        return assets.removeIf(a -> a.getId().equals(assetId));
    }

    public Asset getAssetById(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return null;
        }
        for (Asset a : assets) {
            if (a.getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }

    public boolean containsAsset(String assetId) {
        return getAssetById(assetId) != null;
    }

    public List<Asset> getAssetsByCategory(String category) {
        List<Asset> filtered = new ArrayList<>();
        if (category == null || category.trim().isEmpty() || "ALL".equalsIgnoreCase(category)) {
            return new ArrayList<>(assets);
        }
        for (Asset a : assets) {
            if (category.equalsIgnoreCase(a.getCategory())) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    public List<Asset> searchAssets(String query) {
        List<Asset> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(assets);
        }
        String q = query.toLowerCase().trim();
        for (Asset a : assets) {
            if ((a.getName() != null && a.getName().toLowerCase().contains(q)) ||
                (a.getCategory() != null && a.getCategory().toLowerCase().contains(q)) ||
                (a.getFormat() != null && a.getFormat().toLowerCase().contains(q))) {
                results.add(a);
            }
        }
        return results;
    }

    public void clearAssets() {
        assets.clear();
    }

    // ==========================================
    // On-Demand Asset Streaming & Disk Caching
    // ==========================================

    public void fetchAssetOnDemand(Context context, String assetId, String downloadUrl, OnAssetReadyListener listener) {
        if (context == null || assetId == null || assetId.trim().isEmpty()) {
            if (listener != null) listener.onError("Invalid asset identification or context.");
            return;
        }

        File cacheDir = new File(context.getFilesDir(), CACHE_SUBDIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        String fileName = assetId.endsWith(".glb") ? assetId : assetId + ".glb";
        File localFile = new File(cacheDir, fileName);

        // Fast Path: Check if model is already stored locally on disk
        if (localFile.exists() && localFile.length() > 0) {
            VynaraLogger.system("AssetManager: Cache hit for asset: " + assetId + " (" + localFile.length() + " bytes)");
            if (listener != null) {
                listener.onProgress(100);
                listener.onSuccess(localFile);
            }
            return;
        }

        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            if (listener != null) {
                listener.onError("Asset is not cached locally and no remote download URL was provided.");
            }
            return;
        }

        VynaraLogger.system("AssetManager: Streaming asset on-demand from: " + downloadUrl);

        Request request = new Request.Builder()
                .url(downloadUrl.trim())
                .header("User-Agent", "Vynara-3D-Studio-Android")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                VynaraLogger.e("Asset stream connection error: " + e.getMessage(), e);
                if (listener != null) {
                    mainHandler.post(() -> listener.onError("Network stream failed: " + e.getMessage()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (listener != null) {
                        mainHandler.post(() -> listener.onError("Server returned HTTP " + response.code()));
                    }
                    response.close();
                    return;
                }

                ResponseBody body = response.body();
                long totalBytes = body.contentLength();

                try (InputStream is = body.byteStream();
                     FileOutputStream fos = new FileOutputStream(localFile)) {

                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int read;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        totalBytesRead += read;

                        if (totalBytes > 0 && listener != null) {
                            int progress = (int) ((totalBytesRead * 100) / totalBytes);
                            mainHandler.post(() -> listener.onProgress(progress));
                        }
                    }
                    fos.flush();

                    if (localFile.exists() && localFile.length() > 0) {
                        Asset downloadedAsset = new Asset(assetId, assetId, "STREAMED", "GLB", localFile.getAbsolutePath());
                        addAsset(downloadedAsset);

                        if (listener != null) {
                            mainHandler.post(() -> listener.onSuccess(localFile));
                        }
                    } else {
                        if (listener != null) {
                            mainHandler.post(() -> listener.onError("Downloaded file is empty."));
                        }
                    }

                } catch (Exception ex) {
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                    VynaraLogger.e("Failed writing streamed asset to storage", ex);
                    if (listener != null) {
                        mainHandler.post(() -> listener.onError("File storage error: " + ex.getMessage()));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    public File getLocalCachedFile(Context context, String assetId) {
        if (context == null || assetId == null) return null;
        File cacheDir = new File(context.getFilesDir(), CACHE_SUBDIR);
        String fileName = assetId.endsWith(".glb") ? assetId : assetId + ".glb";
        File file = new File(cacheDir, fileName);
        return file.exists() ? file : null;
    }

    public boolean isAssetCached(Context context, String assetId) {
        File file = getLocalCachedFile(context, assetId);
        return file != null && file.length() > 0;
    }
}