package com.example.character;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.Mesh;
import com.example.engine.SceneObject;
import com.example.engine.ThreeDEngine;

import java.util.HashMap;
import java.util.Map;

public class CharacterManager {
    private final ThreeDEngine engine;
    private final Map<String, Character> characterMap = new HashMap<>();

    public CharacterManager(ThreeDEngine engine) {
        this.engine = engine;
    }

    /**
     * Phase 6 Alignment: Constructs a rigged humanoid character container
     * with standard bone hierarchy.
     */
    public Character createHumanoid(CharacterSpecification spec) {
        if (spec == null) {
            spec = new CharacterSpecification("HUMANOID", "Humanoid Character");
        }

        String id = "char_" + System.currentTimeMillis();
        Skeleton skeleton = SkeletonBuilder.buildHumanoidSkeleton(spec.getHeight());
        MaterialManager matMgr = engine != null ? engine.getMaterialManager() : new MaterialManager();

        Mesh baseMesh = createBaseCharacterMesh(spec.getHeight(), 0.5f, 0.3f);
        Material mat = matMgr != null ? matMgr.getMaterial("mat_default") : null;
        if (mat == null) {
            mat = new Material("mat_" + id, "CharMat", 0.2f, 0.6f, 0.9f, 1.0f);
            if (matMgr != null) {
                matMgr.addMaterial(mat);
            }
        }

        SceneObject characterMeshObj = new SceneObject(id, spec.getName(), "CHARACTER", baseMesh, mat);

        if (engine != null && engine.getSceneManager() != null && engine.getSceneManager().getActiveScene() != null) {
            engine.getSceneManager().getActiveScene().addObject(characterMeshObj);
        }

        Character character = new Character(id, spec, characterMeshObj, skeleton);
        
        // Initialize multi-influence vertex skinning weights against the skeleton
        if (character.getSkin() != null) {
            character.getSkin().normalizeWeights();
        }

        characterMap.put(id, character);
        return character;
    }

    /**
     * Phase 7 Alignment: Constructs a rigged creature/quadruped container
     * with standard bone hierarchy.
     */
    public Character createCreature(CharacterSpecification spec) {
        if (spec == null) {
            spec = new CharacterSpecification("QUADRUPED", "Creature");
        }

        String id = "creature_" + System.currentTimeMillis();
        Skeleton skeleton;
        if ("bird".equalsIgnoreCase(spec.getSpecies())) {
            skeleton = SkeletonBuilder.buildBirdSkeleton();
        } else {
            skeleton = SkeletonBuilder.buildQuadrupedSkeleton();
        }

        MaterialManager matMgr = engine != null ? engine.getMaterialManager() : new MaterialManager();
        Mesh baseMesh = createBaseCharacterMesh(1.0f, 0.8f, 1.2f);
        Material mat = matMgr != null ? matMgr.getMaterial("mat_default") : null;
        if (mat == null) {
            mat = new Material("mat_" + id, "CreatureMat", 0.8f, 0.5f, 0.2f, 1.0f);
            if (matMgr != null) {
                matMgr.addMaterial(mat);
            }
        }

        SceneObject creatureMeshObj = new SceneObject(id, spec.getName(), "CREATURE", baseMesh, mat);

        if (engine != null && engine.getSceneManager() != null && engine.getSceneManager().getActiveScene() != null) {
            engine.getSceneManager().getActiveScene().addObject(creatureMeshObj);
        }

        Character character = new Character(id, spec, creatureMeshObj, skeleton);
        
        if (character.getSkin() != null) {
            character.getSkin().normalizeWeights();
        }

        characterMap.put(id, character);
        return character;
    }

    public void registerCharacter(Character character) {
        if (character != null && character.getId() != null) {
            characterMap.put(character.getId(), character);
        }
    }

    public Character getCharacter(String id) {
        if (id == null) return null;
        return characterMap.get(id);
    }

    public boolean removeCharacter(String id) {
        if (id == null) return false;
        Character removed = characterMap.remove(id);
        if (removed != null && engine != null && engine.getSceneManager() != null && engine.getSceneManager().getActiveScene() != null) {
            engine.getSceneManager().getActiveScene().removeObject(removed.getId());
            return true;
        }
        return false;
    }

    public Map<String, Character> getCharacterMap() {
        return characterMap;
    }

    public void clearCharacters() {
        characterMap.clear();
    }

    private Mesh createBaseCharacterMesh(float height, float width, float depth) {
        float hw = width / 2.0f;
        float hh = height / 2.0f;
        float hd = depth / 2.0f;

        float[] positions = new float[]{
            -hw, 0,  hd,   hw, 0,  hd,   hw, height,  hd,  -hw, height,  hd,
            -hw, 0, -hd,  -hw, height, -hd,   hw, height, -hd,   hw, 0, -hd,
            -hw, height, -hd,  -hw, height,  hd,   hw, height,  hd,   hw, height, -hd,
            -hw, 0, -hd,   hw, 0, -hd,   hw, 0,  hd,  -hw, 0,  hd,
             hw, 0, -hd,   hw, height, -hd,   hw, height,  hd,   hw, 0,  hd,
            -hw, 0, -hd,  -hw, 0,  hd,  -hw, height,  hd,  -hw, height, -hd
        };

        float[] normals = new float[]{
             0,  0,  1,   0,  0,  1,   0,  0,  1,   0,  0,  1,
             0,  0, -1,   0,  0, -1,   0,  0, -1,   0,  0, -1,
             0,  1,  0,   0,  1,  0,   0,  1,  0,   0,  1,  0,
             0, -1,  0,   0, -1,  0,   0, -1,  0,   0, -1,  0,
             1,  0,  0,   1,  0,  0,   1,  0,  0,   1,  0,  0,
            -1,  0,  0,  -1,  0,  0,  -1,  0,  0,  -1,  0,  0
        };

        float[] uvs = new float[]{
            0,0, 1,0, 1,1, 0,1,
            1,0, 1,1, 0,1, 0,0,
            0,1, 0,0, 1,0, 1,1,
            1,1, 0,1, 0,0, 1,0,
            1,0, 1,1, 0,1, 0,0,
            0,0, 1,0, 1,1, 0,1
        };

        short[] indices = new short[]{
             0,  1,  2,   0,  2,  3,
             4,  5,  6,   4,  6,  7,
             8,  9, 10,   8, 10, 11,
            12, 13, 14,  12, 14, 15,
            16, 17, 18,  16, 18, 19,
            20, 21, 22,  20, 22, 23
        };

        return new Mesh(positions, normals, uvs, indices);
    }
}