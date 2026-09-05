package com.example.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConceptGraph {
    private final Map<String, KnowledgeEntry> concepts = new HashMap<>();

    public ConceptGraph() {
        populateDefaultConcepts();
    }

    private void populateDefaultConcepts() {
        // 1. Humanoid Character Anatomy (With precise skeletal/joint definitions)
        concepts.put("humanoid", new KnowledgeEntry("humanoid", "Humanoid Character", "CHARACTER")
                .addComponent("pelvis_root")
                .addComponent("spine_lower").addComponent("spine_chest")
                .addComponent("neck_joint").addComponent("head_skull")
                .addComponent("left_clavicle").addComponent("left_shoulder").addComponent("left_elbow").addComponent("left_wrist").addComponent("left_hand_fingers")
                .addComponent("right_clavicle").addComponent("right_shoulder").addComponent("right_elbow").addComponent("right_wrist").addComponent("right_hand_fingers")
                .addComponent("left_hip").addComponent("left_knee").addComponent("left_ankle").addComponent("left_toes")
                .addComponent("right_hip").addComponent("right_knee").addComponent("right_ankle").addComponent("right_toes")
                .addCapability("procedural_anatomy_mesh")
                .addCapability("skeletal_rigging")
                .addCapability("dual_limb_ik")
                .addCapability("keyframe_animation_player")
                .addMaterial("mat_skin")
                .addMaterial("mat_fabric_grey")
                .setProceduralGeneratorType("character.create_humanoid"));

        // 2. Quadruped / Dog Anatomy (With complete four-leg and tail rigging parameters)
        concepts.put("dog", new KnowledgeEntry("dog", "Dog / Quadruped", "ANIMAL")
                .addComponent("pelvis_anchor").addComponent("spine_chain").addComponent("chest_ribcage")
                .addComponent("neck_support").addComponent("skull_snout").addComponent("ears_left_right")
                .addComponent("front_left_shoulder").addComponent("front_left_knee").addComponent("front_left_paw")
                .addComponent("front_right_shoulder").addComponent("front_right_knee").addComponent("front_right_paw")
                .addComponent("rear_left_hip").addComponent("rear_left_ankle").addComponent("rear_left_paw")
                .addComponent("rear_right_hip").addComponent("rear_right_ankle").addComponent("rear_right_paw")
                .addComponent("tail_chain")
                .addCapability("procedural_quadruped_mesh")
                .addCapability("quadruped_rigging")
                .addCapability("locomotion_animation")
                .addMaterial("mat_leather_brown")
                .setProceduralGeneratorType("character.create_creature"));

        // 3. Bird / Flying Creature Anatomy (With wing structures and flight control bones)
        concepts.put("bird", new KnowledgeEntry("bird", "Bird / Flying Creature", "ANIMAL")
                .addComponent("body_torso").addComponent("neck").addComponent("head_beak")
                .addComponent("left_wing_shoulder").addComponent("left_wing_elbow").addComponent("left_wing_tip")
                .addComponent("right_wing_shoulder").addComponent("right_wing_elbow").addComponent("right_wing_tip")
                .addComponent("left_leg_hip").addComponent("left_foot_claws")
                .addComponent("right_leg_hip").addComponent("right_foot_claws")
                .addComponent("tail_feathers")
                .addCapability("procedural_bird_mesh")
                .addCapability("wing_rigging")
                .addCapability("flight_animation_gait")
                .addMaterial("mat_foliage")
                .setProceduralGeneratorType("character.create_creature"));

        // 4. Architecture: Modern Villa / House
        concepts.put("villa", new KnowledgeEntry("villa", "Modern Seaside Villa", "ARCHITECTURE")
                .addComponent("wooden_deck_base")
                .addComponent("concrete_foundation_slab")
                .addComponent("exterior_concrete_walls").addComponent("interior_partition_walls")
                .addComponent("entrance_door_frame").addComponent("window_glass_frames")
                .addComponent("seaside_swimming_basin").addComponent("pool_water_surface")
                .addComponent("outdoor_lounge_sofa").addComponent("lounge_coffee_table")
                .addComponent("surrounding_palm_trees")
                .addCapability("procedural_architecture")
                .addCapability("pbr_material_shading")
                .addCapability("directional_evening_lighting")
                .addMaterial("mat_concrete")
                .addMaterial("mat_wood_walnut")
                .addMaterial("mat_glass")
                .addMaterial("mat_pool_water")
                .setProceduralGeneratorType("geometry.create_procedural"));

        concepts.put("house", new KnowledgeEntry("house", "Modern House", "ARCHITECTURE")
                .addComponent("concrete_foundation").addComponent("four_perimeter_walls").addComponent("door_opening")
                .addComponent("window_frames").addComponent("translucent_glass_panes").addComponent("sloped_roof_cap")
                .addCapability("procedural_architecture")
                .addMaterial("mat_concrete")
                .addMaterial("mat_wood_walnut")
                .addMaterial("mat_glass")
                .setProceduralGeneratorType("geometry.create_procedural"));

        // 5. Architecture: Swimming Pool
        concepts.put("pool", new KnowledgeEntry("pool", "Swimming Pool & Deck", "ARCHITECTURE")
                .addComponent("surrounding_tiled_deck").addComponent("basin_concrete_walls")
                .addComponent("translucent_water_surface").addComponent("basin_foundation")
                .addCapability("procedural_pool_geometry")
                .addCapability("water_transmission_shader")
                .addMaterial("mat_tiles_deck")
                .addMaterial("mat_concrete")
                .addMaterial("mat_pool_water")
                .setProceduralGeneratorType("geometry.create_procedural"));

        // 6. Furniture: Leather Sofa
        concepts.put("sofa", new KnowledgeEntry("sofa", "Leather Sofa", "FURNITURE")
                .addComponent("wooden_frame_base").addComponent("backrest_leather_cushion")
                .addComponent("left_leather_armrest").addComponent("right_leather_armrest")
                .addComponent("three_seat_cushions").addComponent("four_steel_supporting_pegs")
                .addCapability("procedural_furniture_assembly")
                .addMaterial("mat_wood_walnut")
                .addMaterial("mat_leather_brown")
                .addMaterial("mat_metallic_steel")
                .setProceduralGeneratorType("geometry.create_procedural"));

        // 7. Furniture: Wooden Table
        concepts.put("table", new KnowledgeEntry("table", "Wooden Table", "FURNITURE")
                .addComponent("walnut_tabletop_surface").addComponent("two_steel_supporting_beams").addComponent("four_steel_supporting_legs")
                .addCapability("procedural_table_assembly")
                .addMaterial("mat_wood_walnut")
                .addMaterial("mat_metallic_steel")
                .setProceduralGeneratorType("geometry.create_procedural"));

        // 8. Furniture: Chair
        concepts.put("chair", new KnowledgeEntry("chair", "Armchair", "FURNITURE")
                .addComponent("seat_fabric_cushion").addComponent("backrest_fabric_cushion").addComponent("two_wooden_backrest_struts").addComponent("four_wooden_legs")
                .addCapability("procedural_chair_assembly")
                .addMaterial("mat_fabric_grey")
                .addMaterial("mat_wood_walnut")
                .setProceduralGeneratorType("geometry.create_procedural"));

        // 9. Environment: Procedural Tree / Vegetation
        concepts.put("tree", new KnowledgeEntry("tree", "Procedural Tree", "ENVIRONMENT")
                .addComponent("vertical_bark_trunk").addComponent("four_angled_trunk_branches")
                .addComponent("foliage_main_crown").addComponent("four_secondary_foliage_spheres")
                .addCapability("procedural_vegetation_generation")
                .addMaterial("mat_tree_bark")
                .addMaterial("mat_foliage")
                .setProceduralGeneratorType("geometry.create_procedural"));
    }

    public void addConcept(KnowledgeEntry entry) {
        if (entry != null && entry.getId() != null) {
            concepts.put(entry.getId().toLowerCase(), entry);
        }
    }

    public KnowledgeEntry getConcept(String key) {
        if (key == null) return concepts.get("humanoid");
        String lowerKey = key.toLowerCase().trim();

        if (concepts.containsKey(lowerKey)) {
            return concepts.get(lowerKey);
        }

        for (Map.Entry<String, KnowledgeEntry> entry : concepts.entrySet()) {
            if (lowerKey.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return concepts.get("house"); // Default fallback
    }

    public List<KnowledgeEntry> getConceptsByCategory(String category) {
        List<KnowledgeEntry> results = new ArrayList<>();
        if (category == null) return results;

        for (KnowledgeEntry entry : concepts.values()) {
            if (category.equalsIgnoreCase(entry.getCategory())) {
                results.add(entry);
            }
        }
        return results;
    }

    public Map<String, KnowledgeEntry> getAllConcepts() {
        return concepts;
    }
}