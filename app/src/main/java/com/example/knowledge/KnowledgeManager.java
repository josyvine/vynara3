package com.example.knowledge;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeManager {
    private final ConceptGraph conceptGraph;

    public KnowledgeManager() {
        this.conceptGraph = new ConceptGraph();
        registerExtendedConcepts();
    }

    private void registerExtendedConcepts() {
        // Extended Domain: Tropical Village & Island Environment
        conceptGraph.addConcept(new KnowledgeEntry("village", "Tropical Village Environment", "ENVIRONMENT")
                .addComponent("elevated_wooden_stilts")
                .addComponent("timber_floor_platforms")
                .addComponent("thatched_pyramidal_roofs")
                .addComponent("sand_shoreline_terrain")
                .addComponent("ocean_water_surface")
                .addComponent("curved_palm_tree_canopies")
                .addCapability("procedural_village_environment")
                .addCapability("pbr_material_shading")
                .addCapability("cinematic_sunlight")
                .addMaterial("mat_sand")
                .addMaterial("mat_wood_timber")
                .addMaterial("mat_thatch")
                .addMaterial("mat_ocean_water")
                .setProceduralGeneratorType("geometry.create_procedural"));

        // Extended Domain: Stylized Rigged Superhero Character
        conceptGraph.addConcept(new KnowledgeEntry("superhero", "Stylized Rigged Superhero", "CHARACTER")
                .addComponent("muscular_chest_torso")
                .addComponent("armored_pelvis_waist")
                .addComponent("head_helmet_sculpt")
                .addComponent("heroic_chest_emblem")
                .addComponent("upper_arms_forearms")
                .addComponent("armored_gauntlets")
                .addComponent("thighs_calves_boots")
                .addComponent("full_skeletal_armature")
                .addCapability("procedural_anatomy_mesh")
                .addCapability("skeletal_rigging")
                .addCapability("dual_limb_ik")
                .addCapability("keyframe_animation_player")
                .addMaterial("mat_suit_blue")
                .addMaterial("mat_armor_gold")
                .setProceduralGeneratorType("character.create_humanoid"));
    }

    /**
     * Phase 3 Alignment: Retrieves the primary domain knowledge entry for a prompt.
     */
    public KnowledgeEntry retrieveKnowledgeForPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return conceptGraph.getConcept("house");
        }
        
        List<KnowledgeEntry> entries = retrieveAllKnowledgeForPrompt(userPrompt);
        return !entries.isEmpty() ? entries.get(0) : conceptGraph.getConcept("house");
    }

    /**
     * Phase 3 Alignment: Multi-concept extractor. Scans user prompts and returns 
     * all matching domain knowledge concepts (e.g., villa, pool, sofa, and tree).
     */
    public List<KnowledgeEntry> retrieveAllKnowledgeForPrompt(String userPrompt) {
        List<KnowledgeEntry> matchedConcepts = new ArrayList<>();
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            matchedConcepts.add(conceptGraph.getConcept("house"));
            return matchedConcepts;
        }

        String p = userPrompt.toLowerCase();

        // 1. Environment / Tropical Village
        if (p.contains("village") || p.contains("tropical") || p.contains("hut") || p.contains("island") || p.contains("beach") || p.contains("shoreline")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("village"));
        }

        // 2. Characters & Superheroes
        if (p.contains("superhero") || p.contains("hero") || p.contains("warrior")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("superhero"));
        } else if (p.contains("human") || p.contains("man") || p.contains("woman") || p.contains("character") || p.contains("person")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("humanoid"));
        }
        
        // 3. Animals & Quadrupeds
        if (p.contains("dog") || p.contains("cat") || p.contains("animal") || p.contains("wolf") || p.contains("quadruped") || p.contains("canine")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("dog"));
        }
        
        // 4. Flying Creatures
        if (p.contains("bird") || p.contains("eagle") || p.contains("fly") || p.contains("dragon") || p.contains("wing")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("bird"));
        }
        
        // 5. Architectural Structures (Distinguish Villa from standard House)
        if (p.contains("villa") || p.contains("mansion")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("villa"));
        } else if (p.contains("house") || p.contains("building") || p.contains("architecture") || p.contains("room")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("house"));
        }
        
        // 6. Swimming Pool
        if (p.contains("pool") || p.contains("swimming")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("pool"));
        }

        // 7. Furniture
        if (p.contains("sofa") || p.contains("couch") || p.contains("leather") || p.contains("cushion")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("sofa"));
        } else if (p.contains("chair") || p.contains("armchair")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("chair"));
        }
        
        if (p.contains("table") || p.contains("desk")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("table"));
        }
        
        // 8. Vegetation & Trees
        if (p.contains("tree") || p.contains("plant") || p.contains("forest") || p.contains("palm") || p.contains("foliage")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("tree"));
        }

        if (matchedConcepts.isEmpty()) {
            KnowledgeEntry directMatch = conceptGraph.getConcept(p);
            if (directMatch != null) {
                matchedConcepts.add(directMatch);
            } else {
                matchedConcepts.add(conceptGraph.getConcept("house"));
            }
        }

        return matchedConcepts;
    }

    private void addConceptIfMissing(List<KnowledgeEntry> list, KnowledgeEntry entry) {
        if (entry != null && !list.contains(entry)) {
            list.add(entry);
        }
    }

    public List<KnowledgeEntry> getConceptsByCategory(String category) {
        List<KnowledgeEntry> results = new ArrayList<>();
        if (category == null || conceptGraph.getAllConcepts() == null) return results;

        for (KnowledgeEntry entry : conceptGraph.getAllConcepts().values()) {
            if (category.equalsIgnoreCase(entry.getCategory())) {
                results.add(entry);
            }
        }
        return results;
    }

    public ConceptGraph getConceptGraph() {
        return conceptGraph;
    }
}