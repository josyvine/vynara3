package com.example.engine;

import android.graphics.Bitmap;
import android.graphics.Color;

public class Material {
    private String id;
    private String name;
    private float[] baseColorRGBA = new float[] { 0.8f, 0.8f, 0.8f, 1.0f };
    private float[] emissionRGB = new float[] { 0.0f, 0.0f, 0.0f };
    private float emissionIntensity = 0.0f;
    private float metallic = 0.1f;
    private float roughness = 0.5f;
    private float opacity = 1.0f;
    private float ambientOcclusion = 1.0f;

    // OpenGL ES GPU Texture Handles
    private int textureId = 0;
    private int normalMapTextureId = 0;

    // Staged In-Memory Bitmaps from GLTF decoding
    private Bitmap textureBitmap = null;

    // Texture Map URI / Asset ID References
    private String albedoTextureMap;
    private String normalMap;
    private String roughnessMap;
    private String metallicMap;
    private String aoMap;

    public Material(String id, String name, String hexColor) {
        this.id = id != null ? id : "mat_" + System.currentTimeMillis();
        this.name = name != null ? name : "Unnamed Material";
        setColorHex(hexColor);
    }

    public Material(String id, String name, float r, float g, float b, float a) {
        this.id = id != null ? id : "mat_" + System.currentTimeMillis();
        this.name = name != null ? name : "Unnamed Material";
        this.baseColorRGBA[0] = Math.max(0.0f, Math.min(1.0f, r));
        this.baseColorRGBA[1] = Math.max(0.0f, Math.min(1.0f, g));
        this.baseColorRGBA[2] = Math.max(0.0f, Math.min(1.0f, b));
        this.baseColorRGBA[3] = Math.max(0.0f, Math.min(1.0f, a));
        this.opacity = this.baseColorRGBA[3];
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public float[] getBaseColorRGBA() { return baseColorRGBA; }
    public float[] getEmissionRGB() { return emissionRGB; }
    public float getEmissionIntensity() { return emissionIntensity; }
    public float getMetallic() { return metallic; }
    public float getRoughness() { return roughness; }
    public float getOpacity() { return opacity; }
    public float getAmbientOcclusion() { return ambientOcclusion; }

    public int getTextureId() { return textureId; }
    public void setTextureId(int textureId) { this.textureId = textureId; }

    public Bitmap getTextureBitmap() { return textureBitmap; }
    public void setTextureBitmap(Bitmap bmp) { this.textureBitmap = bmp; }
    public boolean hasTextureBitmap() { return textureBitmap != null && !textureBitmap.isRecycled(); }
    public void clearTextureBitmap() { this.textureBitmap = null; }

    public boolean hasTexture() { 
        return textureId > 0 || hasTextureBitmap() || (albedoTextureMap != null && !albedoTextureMap.isEmpty()); 
    }

    public int getNormalMapTextureId() { return normalMapTextureId; }
    public void setNormalMapTextureId(int normalMapTextureId) { this.normalMapTextureId = normalMapTextureId; }
    public boolean hasNormalMap() { return normalMapTextureId > 0 || (normalMap != null && !normalMap.isEmpty()); }

    public String getAlbedoTextureMap() { return albedoTextureMap; }
    public String getNormalMap() { return normalMap; }
    public String getRoughnessMap() { return roughnessMap; }
    public String getMetallicMap() { return metallicMap; }
    public String getAoMap() { return aoMap; }

    public void setName(String name) { this.name = name; }
    
    public void setMetallic(float metallic) { 
        this.metallic = Math.max(0.0f, Math.min(1.0f, metallic)); 
    }
    
    public void setRoughness(float roughness) { 
        this.roughness = Math.max(0.0f, Math.min(1.0f, roughness)); 
    }
    
    public void setOpacity(float opacity) { 
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity)); 
        this.baseColorRGBA[3] = this.opacity;
    }
    
    public void setAmbientOcclusion(float ao) { 
        this.ambientOcclusion = Math.max(0.0f, Math.min(1.0f, ao)); 
    }

    public void setEmission(float r, float g, float b, float intensity) {
        this.emissionRGB[0] = Math.max(0.0f, Math.min(1.0f, r));
        this.emissionRGB[1] = Math.max(0.0f, Math.min(1.0f, g));
        this.emissionRGB[2] = Math.max(0.0f, Math.min(1.0f, b));
        this.emissionIntensity = Math.max(0.0f, intensity);
    }

    public void setAlbedoTextureMap(String textureUri) { this.albedoTextureMap = textureUri; }
    public void setNormalMap(String normalUri) { this.normalMap = normalUri; }
    public void setRoughnessMap(String roughnessUri) { this.roughnessMap = roughnessUri; }
    public void setMetallicMap(String metallicUri) { this.metallicMap = metallicUri; }
    public void setAoMap(String aoUri) { this.aoMap = aoUri; }

    public void setColorHex(String hexColor) {
        if (hexColor == null || hexColor.trim().isEmpty()) {
            this.baseColorRGBA = new float[] { 0.8f, 0.8f, 0.8f, this.opacity };
            return;
        }
        try {
            String sanitized = hexColor.trim();
            if (!sanitized.startsWith("#")) {
                sanitized = "#" + sanitized;
            }
            int c = Color.parseColor(sanitized);
            baseColorRGBA[0] = Color.red(c) / 255f;
            baseColorRGBA[1] = Color.green(c) / 255f;
            baseColorRGBA[2] = Color.blue(c) / 255f;
            baseColorRGBA[3] = (Color.alpha(c) / 255f) * opacity;
        } catch (Exception e) {
            this.baseColorRGBA = new float[] { 0.8f, 0.8f, 0.8f, this.opacity };
        }
    }

    public Material cloneMaterial(String newId, String newName) {
        Material copy = new Material(newId, newName, baseColorRGBA[0], baseColorRGBA[1], baseColorRGBA[2], baseColorRGBA[3]);
        copy.setMetallic(this.metallic);
        copy.setRoughness(this.roughness);
        copy.setOpacity(this.opacity);
        copy.setAmbientOcclusion(this.ambientOcclusion);
        copy.setEmission(this.emissionRGB[0], this.emissionRGB[1], this.emissionRGB[2], this.emissionIntensity);
        copy.setTextureId(this.textureId);
        copy.setTextureBitmap(this.textureBitmap);
        copy.setNormalMapTextureId(this.normalMapTextureId);
        copy.setAlbedoTextureMap(this.albedoTextureMap);
        copy.setNormalMap(this.normalMap);
        copy.setRoughnessMap(this.roughnessMap);
        copy.setMetallicMap(this.metallicMap);
        copy.setAoMap(this.aoMap);
        return copy;
    }
}