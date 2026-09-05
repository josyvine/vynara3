package com.example.asset;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Asset {
    private String id;
    private String name;
    private String category; // MESH, MATERIAL, SKELETON, ANIMATION, TEXTURE
    private String format; // GLTF, OBJ, PBR, GLB
    private String fileSizeStr;
    private String filePath;
    private String thumbnailPath;
    private int polyCount;
    private long createdAtMs;

    public Asset(String id, String name, String category, String format, String fileSizeStr) {
        this.id = id != null ? id : "asset_" + System.currentTimeMillis();
        this.name = name != null ? name : "Generated Asset";
        this.category = category != null ? category.toUpperCase() : "MESH";
        this.format = format != null ? format.toUpperCase() : "GLTF";
        this.fileSizeStr = fileSizeStr != null ? fileSizeStr : "0 KB";
        this.createdAtMs = System.currentTimeMillis();
    }

    public Asset(String id, String name, String category, String format, String fileSizeStr, String filePath, int polyCount) {
        this(id, name, category, format, fileSizeStr);
        this.filePath = filePath;
        this.polyCount = Math.max(0, polyCount);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getFormat() { return format; }
    public String getFileSizeStr() { return fileSizeStr; }
    public String getFilePath() { return filePath; }
    public String getThumbnailPath() { return thumbnailPath; }
    public int getPolyCount() { return polyCount; }
    public long getCreatedAtMs() { return createdAtMs; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category != null ? category.toUpperCase() : "MESH"; }
    public void setFormat(String format) { this.format = format != null ? format.toUpperCase() : "GLTF"; }
    public void setFileSizeStr(String fileSizeStr) { this.fileSizeStr = fileSizeStr; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public void setPolyCount(int polyCount) { this.polyCount = Math.max(0, polyCount); }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAtMs));
    }
}