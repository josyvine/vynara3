package com.example.engine;

import java.util.HashMap;
import java.util.Map;

public class MaterialManager {
    private final Map<String, Material> materials = new HashMap<>();

    public MaterialManager() {
        populateDefaultMaterials();
    }

    private void populateDefaultMaterials() {
        // Default Base Material
        Material matDefault = new Material("mat_default", "Default Grey", "#A0A5BD");
        matDefault.setMetallic(0.1f);
        matDefault.setRoughness(0.5f);
        materials.put("mat_default", matDefault);

        // Human & Organic Skin
        Material matSkin = new Material("mat_skin", "Human Skin", "#E0AC69");
        matSkin.setMetallic(0.05f);
        matSkin.setRoughness(0.6f);
        materials.put("mat_skin", matSkin);

        // Furniture Leather & Fabrics
        Material matLeather = new Material("mat_leather_brown", "Brown Leather", "#6E3B1F");
        matLeather.setMetallic(0.1f);
        matLeather.setRoughness(0.4f);
        materials.put("mat_leather_brown", matLeather);

        Material matFabric = new Material("mat_fabric_grey", "Grey Fabric", "#555A6E");
        matFabric.setMetallic(0.0f);
        matFabric.setRoughness(0.8f);
        materials.put("mat_fabric_grey", matFabric);

        // Architectural Wood & Concrete
        Material matWood = new Material("mat_wood_walnut", "Dark Walnut Wood", "#4A2E1B");
        matWood.setMetallic(0.05f);
        matWood.setRoughness(0.35f);
        materials.put("mat_wood_walnut", matWood);

        Material matConcrete = new Material("mat_concrete", "White Concrete", "#E2E4EB");
        matConcrete.setMetallic(0.05f);
        matConcrete.setRoughness(0.7f);
        materials.put("mat_concrete", matConcrete);

        Material matTiles = new Material("mat_tiles_deck", "Deck Tiles", "#8C6239");
        matTiles.setMetallic(0.1f);
        matTiles.setRoughness(0.5f);
        materials.put("mat_tiles_deck", matTiles);

        // Glass & Transparent Pool Water
        Material matGlass = new Material("mat_glass", "Glass Windows", "#3300E5FF");
        matGlass.setMetallic(0.9f);
        matGlass.setRoughness(0.05f);
        matGlass.setOpacity(0.35f);
        materials.put("mat_glass", matGlass);

        Material matWater = new Material("mat_pool_water", "Pool Water", "#6600B2FF");
        matWater.setMetallic(0.2f);
        matWater.setRoughness(0.1f);
        matWater.setOpacity(0.65f);
        materials.put("mat_pool_water", matWater);

        // Vegetation & Foliage
        Material matFoliage = new Material("mat_foliage", "Green Leaves", "#2E7D32");
        matFoliage.setMetallic(0.05f);
        matFoliage.setRoughness(0.6f);
        materials.put("mat_foliage", matFoliage);

        Material matBark = new Material("mat_tree_bark", "Tree Bark", "#3E2723");
        matBark.setMetallic(0.0f);
        matBark.setRoughness(0.9f);
        materials.put("mat_tree_bark", matBark);

        // Metallic PBR Presets
        Material matGold = new Material("mat_metallic_gold", "Metallic Gold", "#FFD700");
        matGold.setMetallic(0.95f);
        matGold.setRoughness(0.15f);
        materials.put("mat_metallic_gold", matGold);

        Material matSteel = new Material("mat_metallic_steel", "Stainless Steel", "#B0BEC5");
        matSteel.setMetallic(0.85f);
        matSteel.setRoughness(0.25f);
        materials.put("mat_metallic_steel", matSteel);

        // Emissive Light Material
        Material matEmissive = new Material("mat_emissive_white", "Emissive Light", "#FFFFFF");
        matEmissive.setEmission(1.0f, 1.0f, 1.0f, 2.0f);
        materials.put("mat_emissive_white", matEmissive);
    }

    public synchronized Material getMaterial(String id) {
        if (id == null) return materials.get("mat_default");
        Material mat = materials.get(id);
        return mat != null ? mat : materials.get("mat_default");
    }

    public synchronized void addMaterial(Material mat) {
        if (mat != null && mat.getId() != null) {
            materials.put(mat.getId(), mat);
        }
    }

    public synchronized boolean removeMaterial(String id) {
        if (id == null || "mat_default".equals(id)) return false;
        return materials.remove(id) != null;
    }

    public synchronized boolean containsMaterial(String id) {
        return id != null && materials.containsKey(id);
    }

    public synchronized Material createCustomPBRMaterial(String name, String hexColor, float metallic, float roughness) {
        String id = "mat_custom_" + System.currentTimeMillis();
        Material customMat = new Material(id, name != null ? name : "Custom Material", hexColor);
        customMat.setMetallic(metallic);
        customMat.setRoughness(roughness);
        addMaterial(customMat);
        return customMat;
    }

    public synchronized Map<String, Material> getAllMaterials() {
        return new HashMap<>(materials);
    }
}