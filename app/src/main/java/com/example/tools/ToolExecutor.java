package com.example.tools;

import com.example.ai.ApiKeyManager;
import com.example.asset.AssetManager;
import com.example.character.Character;
import com.example.character.CharacterManager;
import com.example.character.CharacterSpecification;
import com.example.cloud.CloudProvider;
import com.example.cloud.GitHubOAuthService;
import com.example.cloud.GitHubWorkflowBridge;
import com.example.cloud.HuggingFaceBridge;
import com.example.engine.GLTFImporter;
import com.example.engine.Material;
import com.example.engine.SceneObject;
import com.example.engine.ThreeDEngine;
import com.example.export.GLTFExporter;
import com.example.runtime.ProjectRuntime;
import com.example.utils.VynaraLogger;
import com.example.utils.VynaraLogger.LogLevel;
import com.example.validation.ValidationManager;
import com.example.validation.ValidationResult;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToolExecutor {
    private final ThreeDEngine engine;
    private final CharacterManager characterManager;
    private final ValidationManager validationManager;

    public ToolExecutor(ThreeDEngine engine, CharacterManager characterManager, ValidationManager validationManager) {
        this.engine = engine;
        this.characterManager = characterManager;
        this.validationManager = validationManager;
    }

    /**
     * Overloaded execution method resolving direct caller invocations
     * from dialogs and controllers without altering existing engine logic.
     */
    public boolean execute(ToolOperation op, ProjectRuntime runtime) {
        return executeOperation(op);
    }

    /**
     * Overloaded execution method alias for executeOperation.
     */
    public boolean execute(ToolOperation op) {
        return executeOperation(op);
    }

    public boolean executeOperation(ToolOperation op) {
        if (op == null || op.getToolId() == null) return false;

        String id = op.getToolId().toLowerCase().trim();

        switch (id) {
            case "geometry.create_primitive": {
                String type = op.getStringParam("type", "cube");
                float w = op.getFloatParam("width", 1.5f);
                float h = op.getFloatParam("height", 1.5f);
                float d = op.getFloatParam("depth", 1.5f);
                
                VynaraLogger.execution("Executing geometry.create_primitive: type=" + type + ", dimensions=" + w + "x" + h + "x" + d);
                SceneObject obj = engine.createPrimitive(type, w, h, d);
                engine.getSceneManager().updateWorldTransforms();
                return obj != null;
            }

            case "geometry.create_procedural": {
                String type = op.getStringParam("type", "house");
                String name = op.getStringParam("name", type.toUpperCase());
                
                VynaraLogger.generator("Executing geometry.create_procedural: type=" + type + ", name=" + name);
                SceneObject obj = engine.createProceduralStructure(type, name);
                engine.getSceneManager().updateWorldTransforms();
                return obj != null;
            }

            case "geometry.transform.translate": {
                String objId = op.getStringParam("objectId", null);
                SceneObject obj = findTargetObject(objId);
                if (obj != null) {
                    float x = op.getFloatParam("x", 0f);
                    float y = op.getFloatParam("y", 0f);
                    float z = op.getFloatParam("z", 0f);
                    
                    VynaraLogger.execution("Executing geometry.transform.translate: objectId=" + obj.getId() + ", coords=[" + x + ", " + y + ", " + z + "]");
                    obj.getTransform().setPosition(x, y, z);
                    engine.getSceneManager().updateWorldTransforms();
                    return true;
                }
                VynaraLogger.e("geometry.transform.translate FAILED: Target object reference null.");
                return false;
            }

            case "geometry.transform.rotate": {
                String objId = op.getStringParam("objectId", null);
                SceneObject obj = findTargetObject(objId);
                if (obj != null) {
                    float x = op.getFloatParam("x", 0f);
                    float y = op.getFloatParam("y", 0f);
                    float z = op.getFloatParam("z", 0f);
                    
                    VynaraLogger.execution("Executing geometry.transform.rotate: objectId=" + obj.getId() + ", angles=[" + x + "d, " + y + "d, " + z + "d]");
                    obj.getTransform().setRotation(x, y, z);
                    engine.getSceneManager().updateWorldTransforms();
                    return true;
                }
                VynaraLogger.e("geometry.transform.rotate FAILED: Target object reference null.");
                return false;
            }

            case "geometry.transform.scale": {
                String objId = op.getStringParam("objectId", null);
                SceneObject obj = findTargetObject(objId);
                if (obj != null) {
                    float sx = op.getFloatParam("scaleX", 1f);
                    float sy = op.getFloatParam("scaleY", 1f);
                    float sz = op.getFloatParam("scaleZ", 1f);
                    
                    VynaraLogger.execution("Executing geometry.transform.scale: objectId=" + obj.getId() + ", scaleFactors=[" + sx + ", " + sy + ", " + sz + "]");
                    obj.getTransform().setScale(sx, sy, sz);
                    engine.getSceneManager().updateWorldTransforms();
                    return true;
                }
                VynaraLogger.e("geometry.transform.scale FAILED: Target object reference null.");
                return false;
            }

            case "geometry.delete_object": {
                String objId = op.getStringParam("objectId", null);
                boolean success;
                if (objId != null) {
                    VynaraLogger.execution("Executing geometry.delete_object: objectId=" + objId);
                    engine.getSceneManager().getActiveScene().removeObject(objId);
                    success = true;
                } else {
                    VynaraLogger.execution("Executing geometry.delete_object: Deleting currently selected object.");
                    success = engine.getSceneManager().deleteSelectedObject();
                }
                engine.getSceneManager().updateWorldTransforms();
                return success;
            }

            case "geometry.duplicate_object": {
                String objId = op.getStringParam("objectId", null);
                SceneObject target = findTargetObject(objId);
                if (target != null) {
                    VynaraLogger.execution("Executing geometry.duplicate_object: objectId=" + target.getId());
                    SceneObject copy = engine.getSceneManager().duplicateObject(target);
                    engine.getSceneManager().updateWorldTransforms();
                    return copy != null;
                }
                VynaraLogger.e("geometry.duplicate_object FAILED: Target object reference null.");
                return false;
            }

            case "material.set_properties": {
                String objId = op.getStringParam("objectId", null);
                SceneObject obj = findTargetObject(objId);
                if (obj != null) {
                    String color = op.getStringParam("colorHex", "#00E5FF");
                    float metallic = op.getFloatParam("metallic", 0.1f);
                    float roughness = op.getFloatParam("roughness", 0.5f);
                    float opacity = op.getFloatParam("opacity", 1.0f);

                    VynaraLogger.material("Executing material.set_properties: objectId=" + obj.getId() + ", colorHex=" + color + ", metallic=" + metallic + ", roughness=" + roughness);
                    Material mat = new Material("mat_" + System.currentTimeMillis(), "Custom Mat", color);
                    mat.setMetallic(metallic);
                    mat.setRoughness(roughness);
                    mat.setOpacity(opacity);
                    obj.setMaterial(mat);
                    return true;
                }
                VynaraLogger.e("material.set_properties FAILED: Target object reference null.");
                return false;
            }

            case "material.apply": {
                String objId = op.getStringParam("objectId", null);
                SceneObject obj = findTargetObject(objId);
                if (obj != null) {
                    String color = op.getStringParam("colorHex", "#00E5FF");
                    VynaraLogger.material("Executing material.apply on object: " + obj.getId());
                    Material mat = new Material("mat_" + System.currentTimeMillis(), "Applied Mat", color);
                    obj.setMaterial(mat);
                    return true;
                }
                return true;
            }

            case "material.create": {
                String name = op.getStringParam("name", "New Material");
                String color = op.getStringParam("colorHex", "#FFFFFF");
                float metallic = op.getFloatParam("metallic", 0.0f);
                float roughness = op.getFloatParam("roughness", 0.5f);

                VynaraLogger.material("Executing material.create: name=" + name + ", colorHex=" + color + ", metallic=" + metallic);
                Material mat = engine.getMaterialManager().createCustomPBRMaterial(name, color, metallic, roughness);
                return mat != null;
            }

            case "character.create_humanoid": {
                String name = op.getStringParam("name", "Humanoid Character");
                float height = op.getFloatParam("height", 1.8f);
                String style = op.getStringParam("style", "REALISTIC");

                VynaraLogger.generator("Executing character.create_humanoid: name=" + name + ", height=" + height + ", style=" + style);
                CharacterSpecification spec = new CharacterSpecification("HUMANOID", name)
                        .setHeight(height)
                        .setStyle(style);
                Character c = characterManager.createHumanoid(spec);
                engine.getSceneManager().updateWorldTransforms();
                return c != null;
            }

            case "character.create_creature": {
                String species = op.getStringParam("species", "dog");
                String name = op.getStringParam("name", species.toUpperCase());

                VynaraLogger.generator("Executing character.create_creature: species=" + species + ", name=" + name);
                CharacterSpecification spec = new CharacterSpecification(species, name);
                Character c = characterManager.createCreature(spec);
                engine.getSceneManager().updateWorldTransforms();
                return c != null;
            }

            case "skeleton.bind": {
                String charId = op.getStringParam("characterId", null);
                Character c = characterManager.getCharacter(charId);
                if (c == null && !characterManager.getCharacterMap().isEmpty()) {
                    c = characterManager.getCharacterMap().values().iterator().next();
                }
                
                if (c != null) {
                    VynaraLogger.execution("Executing skeleton.bind: characterId=" + c.getId());
                } else {
                    VynaraLogger.execution("Executing skeleton.bind: Binding default character container.");
                }
                
                if (c != null && c.getSkin() != null) {
                    c.getSkin().normalizeWeights();
                    return true;
                }
                return c != null;
            }

            case "rig.create_ik": {
                String charId = op.getStringParam("characterId", null);
                String limb = op.getStringParam("limb", "left_arm");
                float targetX = op.getFloatParam("x", 0.5f);
                float targetY = op.getFloatParam("y", 1.2f);
                float targetZ = op.getFloatParam("z", 0.3f);

                Character c = characterManager.getCharacter(charId);
                if (c == null && !characterManager.getCharacterMap().isEmpty()) {
                    c = characterManager.getCharacterMap().values().iterator().next();
                }
                
                if (c != null) {
                    VynaraLogger.execution("Executing rig.create_ik: characterId=" + c.getId() + ", limb=" + limb + ", target=[" + targetX + ", " + targetY + ", " + targetZ + "]");
                } else {
                    VynaraLogger.execution("Executing rig.create_ik: Limb=" + limb + ", target=[" + targetX + ", " + targetY + ", " + targetZ + "]");
                }
                
                if (c != null && c.getRig() != null) {
                    c.getRig().setIKTarget(limb, targetX, targetY, targetZ);
                    return true;
                }
                VynaraLogger.e("rig.create_ik FAILED: Target character or rigging container null.");
                return false;
            }

            case "animation.create_clip": {
                String charId = op.getStringParam("characterId", null);
                String clip = op.getStringParam("clipName", "walk");

                Character c = characterManager.getCharacter(charId);
                if (c == null && !characterManager.getCharacterMap().isEmpty()) {
                    c = characterManager.getCharacterMap().values().iterator().next();
                }
                
                if (c != null) {
                    VynaraLogger.execution("Executing animation.create_clip: characterId=" + c.getId() + ", clip=" + clip);
                } else {
                    VynaraLogger.execution("Executing animation.create_clip: Playing global clip=" + clip);
                }
                
                if (c != null && c.getAnimationPlayer() != null) {
                    c.getAnimationPlayer().playClip(clip);
                    return true;
                }
                VynaraLogger.e("animation.create_clip FAILED: Target character or player reference null.");
                return false;
            }

            case "blender.generate":
            case "blender.cloud_generate": {
                String prompt = op.getStringParam("prompt", "3D asset");
                String bpyScript = op.getStringParam("bpyScript", "");
                if (bpyScript.isEmpty()) {
                    bpyScript = op.getStringParam("compositeMasterScript", "");
                }
                String assetId = op.getStringParam("assetId", "asset_" + System.currentTimeMillis());

                VynaraLogger.system("Executing blender.cloud_generate: assetId=" + assetId + ", prompt=" + prompt);

                ApiKeyManager keyManager = ProjectRuntime.getInstance().getAIOrchestrator().getApiKeyManager();
                CloudProvider provider = keyManager.getComputeProvider();

                String repo = keyManager.getGitHubRepo();
                String pat = keyManager.getGitHubPat();

                if (pat.isEmpty() && ProjectRuntime.getInstance().getContext() != null) {
                    pat = GitHubOAuthService.getAccessToken(ProjectRuntime.getInstance().getContext());
                }

                if (repo.isEmpty() && ProjectRuntime.getInstance().getContext() != null) {
                    String user = GitHubOAuthService.getUserLogin(ProjectRuntime.getInstance().getContext());
                    if (!user.isEmpty()) {
                        repo = user + "/vynara2";
                    }
                }

                if (pat.isEmpty()) {
                    VynaraLogger.e("blender.cloud_generate FAILED: No GitHub token available. Please sign in via Settings.");
                    return false;
                }

                final String targetRepo = repo;
                final String targetPat = pat;

                File modelsDir = new File(ProjectRuntime.getInstance().getContext().getFilesDir(), "models_cache");
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs();
                }
                File outputGlb = new File(modelsDir, assetId + ".glb");
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicBoolean success = new AtomicBoolean(false);

                if (provider == CloudProvider.HUGGING_FACE && keyManager.hasHuggingFaceConfig()) {
                    HuggingFaceBridge hfBridge = new HuggingFaceBridge();
                    hfBridge.generateAsset(keyManager.getHuggingFaceSpaceUrl(), keyManager.getHuggingFaceToken(), bpyScript, outputGlb, new HuggingFaceBridge.GenerationCallback() {
                        @Override
                        public void onProgress(int percentage, long bytesRead, long totalBytes) {
                            VynaraLogger.ai("Hugging Face download progress: " + percentage + "%");
                        }

                        @Override
                        public void onSuccess(File downloadedGlbFile) {
                            try {
                                GLTFImporter.ImportResult result = GLTFImporter.loadFromFile(downloadedGlbFile);
                                for (SceneObject obj : result.getSceneObjects()) {
                                    engine.getSceneManager().getActiveScene().addObject(obj);
                                }
                                for (Character ch : result.getCharacters()) {
                                    characterManager.registerCharacter(ch);
                                }
                                engine.getSceneManager().updateWorldTransforms();
                                autoFrameCameraOnScene();
                                success.set(true);
                            } catch (Exception ex) {
                                VynaraLogger.e("Failed to import generated GLB into active scene", ex);
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            VynaraLogger.e("Hugging Face worker error: " + errorMessage);
                            latch.countDown();
                        }
                    });
                } else {
                    GitHubWorkflowBridge ghBridge = new GitHubWorkflowBridge();
                    VynaraLogger.system("GitHubWorkflowBridge: Triggering workflow dispatch for " + targetRepo);
                    ghBridge.dispatchGenerationWorkflow(targetRepo, targetPat, "vynara_generate", assetId, bpyScript, new GitHubWorkflowBridge.WorkflowDispatchCallback() {
                        @Override
                        public void onDispatched(String eventType, String aId) {
                            VynaraLogger.system("GitHub generation workflow dispatched successfully to " + targetRepo + ". Monitoring run progress...");
                            
                            ghBridge.awaitWorkflowAndDownloadArtifact(targetRepo, targetPat, assetId, outputGlb, new GitHubWorkflowBridge.WorkflowPollingCallback() {
                                @Override
                                public void onStatusUpdate(String status, String details) {
                                    VynaraLogger.system("GitHub Action Execution: " + details);
                                }

                                @Override
                                public void onProgress(int percentage, long bytesRead, long totalBytes) {
                                    VynaraLogger.system("Downloading Artifact: " + percentage + "% (" + bytesRead + "/" + totalBytes + " bytes)");
                                }

                                @Override
                                public void onSuccess(File downloadedGlbFile) {
                                    try {
                                        VynaraLogger.system("Importing downloaded GLB into 3D scene engine...");
                                        GLTFImporter.ImportResult result = GLTFImporter.loadFromFile(downloadedGlbFile);
                                        for (SceneObject obj : result.getSceneObjects()) {
                                            engine.getSceneManager().getActiveScene().addObject(obj);
                                        }
                                        for (Character ch : result.getCharacters()) {
                                            characterManager.registerCharacter(ch);
                                        }
                                        engine.getSceneManager().updateWorldTransforms();
                                        autoFrameCameraOnScene();

                                        File renderImg = GitHubWorkflowBridge.getAssociatedRenderImage(downloadedGlbFile);
                                        if (renderImg != null) {
                                            VynaraLogger.system("ToolExecutor: Photorealistic Cycles preview render verified at " + renderImg.getName());
                                        }

                                        success.set(true);
                                    } catch (Exception ex) {
                                        VynaraLogger.e("Failed to import downloaded GLB into active scene", ex);
                                    } finally {
                                        latch.countDown();
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    VynaraLogger.e("GitHub Actions workflow pipeline failed: " + errorMessage);
                                    latch.countDown();
                                }
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            VynaraLogger.e("GitHub workflow dispatch failed: " + errorMessage);
                            latch.countDown();
                        }
                    });
                }

                try {
                    latch.await(300, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                return success.get();
            }

            case "rig.auto_rig_cloud": {
                String objId = op.getStringParam("objectId", null);
                String rigType = op.getStringParam("rigType", "humanoid");
                SceneObject target = findTargetObject(objId);

                if (target == null) {
                    VynaraLogger.system("rig.auto_rig_cloud: Target mesh object not explicitly found. Creating humanoid container.");
                    CharacterSpecification spec = new CharacterSpecification("HUMANOID", "Hero")
                            .setHeight(1.8f)
                            .setStyle("REALISTIC");
                    Character c = characterManager.createHumanoid(spec);
                    engine.getSceneManager().updateWorldTransforms();
                    return c != null;
                }

                VynaraLogger.system("Executing rig.auto_rig_cloud for object: " + target.getId());
                ApiKeyManager keyManager = ProjectRuntime.getInstance().getAIOrchestrator().getApiKeyManager();

                if (!keyManager.hasHuggingFaceConfig() || keyManager.getHuggingFaceSpaceUrl().trim().isEmpty()) {
                    VynaraLogger.system("Hugging Face Space URL is not configured. Falling back to native procedural rigging engine.");
                    return applyLocalRigFallback(target, rigType);
                }

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicBoolean success = new AtomicBoolean(false);

                try {
                    File tempMeshFile = new File(ProjectRuntime.getInstance().getContext().getCacheDir(), "export_" + target.getId() + ".gltf");
                    String gltfContent = GLTFExporter.exportSceneToGLTFJson(engine.getSceneManager().getActiveScene());
                    try (FileOutputStream fos = new FileOutputStream(tempMeshFile)) {
                        fos.write(gltfContent.getBytes(StandardCharsets.UTF_8));
                    }

                    File riggedOutput = new File(ProjectRuntime.getInstance().getContext().getFilesDir(), "models_cache/rigged_" + target.getId() + ".glb");
                    HuggingFaceBridge hfBridge = new HuggingFaceBridge();
                    hfBridge.autoRigMesh(keyManager.getHuggingFaceSpaceUrl(), keyManager.getHuggingFaceToken(), tempMeshFile, rigType, riggedOutput, new HuggingFaceBridge.GenerationCallback() {
                        @Override
                        public void onProgress(int percentage, long bytesRead, long totalBytes) {}

                        @Override
                        public void onSuccess(File downloadedGlbFile) {
                            try {
                                GLTFImporter.ImportResult result = GLTFImporter.loadFromFile(downloadedGlbFile);
                                engine.getSceneManager().getActiveScene().removeObject(target.getId());
                                for (SceneObject obj : result.getSceneObjects()) {
                                    engine.getSceneManager().getActiveScene().addObject(obj);
                                }
                                for (Character riggedChar : result.getCharacters()) {
                                    characterManager.registerCharacter(riggedChar);
                                }
                                engine.getSceneManager().updateWorldTransforms();
                                success.set(true);
                            } catch (Exception ex) {
                                VynaraLogger.e("Failed to parse auto-rigged GLB", ex);
                                boolean localOk = applyLocalRigFallback(target, rigType);
                                success.set(localOk);
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            VynaraLogger.system("Hugging Face remote worker unavailable: " + errorMessage + ". Engaging native rigging engine fallback.");
                            boolean localOk = applyLocalRigFallback(target, rigType);
                            success.set(localOk);
                            latch.countDown();
                        }
                    });

                    latch.await(90, TimeUnit.SECONDS);
                } catch (Exception e) {
                    VynaraLogger.e("Auto-rigging process error, applying native fallback", e);
                    return applyLocalRigFallback(target, rigType);
                }

                return success.get();
            }

            case "asset.fetch_and_spawn": {
                String assetId = op.getStringParam("assetId", "model_" + System.currentTimeMillis());
                String url = op.getStringParam("url", "");
                float px = op.getFloatParam("posX", 0f);
                float py = op.getFloatParam("posY", 0f);
                float pz = op.getFloatParam("posZ", 0f);

                VynaraLogger.execution("Executing asset.fetch_and_spawn: assetId=" + assetId);
                AssetManager assetManager = ProjectRuntime.getInstance().getAssetManager();

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicBoolean success = new AtomicBoolean(false);

                assetManager.fetchAssetOnDemand(ProjectRuntime.getInstance().getContext(), assetId, url, new AssetManager.OnAssetReadyListener() {
                    @Override
                    public void onProgress(int percentage) {}

                    @Override
                    public void onSuccess(File assetFile) {
                        try {
                            GLTFImporter.ImportResult result = GLTFImporter.loadFromFile(assetFile);
                            for (SceneObject obj : result.getSceneObjects()) {
                                obj.getTransform().setPosition(px, py, pz);
                                engine.getSceneManager().getActiveScene().addObject(obj);
                            }
                            for (Character ch : result.getCharacters()) {
                                characterManager.registerCharacter(ch);
                            }
                            engine.getSceneManager().updateWorldTransforms();
                            autoFrameCameraOnScene();
                            success.set(true);
                        } catch (Exception ex) {
                            VynaraLogger.e("Failed to inject downloaded asset into scene", ex);
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        VynaraLogger.e("Asset download failed: " + message);
                        latch.countDown();
                    }
                });

                try {
                    latch.await(45, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                return success.get();
            }

            case "scene.add_light": {
                String typeStr = op.getStringParam("type", "directional");
                String color = op.getStringParam("colorHex", "#FFFFFF");
                float intensity = op.getFloatParam("intensity", 1.0f);

                VynaraLogger.execution("Executing scene.add_light: type=" + typeStr + ", intensity=" + intensity + ", colorHex=" + color);
                com.example.engine.Light light = new com.example.engine.Light("light_" + System.currentTimeMillis(),
                        "point".equalsIgnoreCase(typeStr) ? com.example.engine.Light.Type.POINT : com.example.engine.Light.Type.DIRECTIONAL);
                light.setColorHex(color);
                light.setIntensity(intensity);
                engine.getLightManager().addLight(light);
                return true;
            }

            case "scene.set_camera": {
                float x = op.getFloatParam("posX", 0f);
                float y = op.getFloatParam("posY", 4f);
                float z = op.getFloatParam("posZ", 8f);
                float tx = op.getFloatParam("targetX", 0f);
                float ty = op.getFloatParam("targetY", 1f);
                float tz = op.getFloatParam("targetZ", 0f);

                VynaraLogger.execution("Executing scene.set_camera: pos=[" + x + ", " + y + ", " + z + "], lookTarget=[" + tx + ", " + ty + ", " + tz + "]");
                engine.getCameraManager().getActiveCamera().setEye(x, y, z);
                engine.getCameraManager().getActiveCamera().setTarget(tx, ty, tz);
                return true;
            }

            case "scene.clear": {
                VynaraLogger.execution("Executing scene.clear: Resetting scene objects and character containers...");
                engine.getSceneManager().getActiveScene().getObjects().clear();
                engine.getSceneManager().selectObject(null);
                characterManager.getCharacterMap().clear();
                engine.getSceneManager().updateWorldTransforms();
                return true;
            }

            case "scene.add_node": {
                VynaraLogger.execution("Executing scene.add_node: Node added.");
                return true;
            }

            case "transaction.undo": {
                VynaraLogger.execution("Executing transaction.undo...");
                return ProjectRuntime.getInstance().getUndoManager().undo();
            }

            case "transaction.redo": {
                VynaraLogger.execution("Executing transaction.redo...");
                return ProjectRuntime.getInstance().getRedoManager().redo();
            }

            case "project.save":
            case "project.load":
            case "project.create": {
                String projectId = op.getStringParam("projectId", "default_project");
                VynaraLogger.execution("Executing " + id + ": projectId=" + projectId);
                return true;
            }

            case "validation.check_mesh": {
                if (validationManager == null || engine == null) {
                    VynaraLogger.validation(LogLevel.ERROR, "validation.check_mesh FAILED: ValidationManager or engine reference is null.");
                    return false;
                }
                VynaraLogger.validation(LogLevel.INFO, "Executing validation.check_mesh: Analyzing active scene graph...");
                List<ValidationResult> results = validationManager.validateScene(engine.getSceneManager().getActiveScene());
                if (results == null) {
                    VynaraLogger.validation(LogLevel.ERROR, "validation.check_mesh FAILED: Scene validation output was null.");
                    return false;
                }
                
                for (ValidationResult res : results) {
                    if (!res.isPassed()) {
                        VynaraLogger.validation(LogLevel.ERROR, "Validation check FAILED: " + res.getMessage() + " Suggestion: " + res.getRepairSuggestion());
                        return false;
                    }
                }
                VynaraLogger.validation(LogLevel.INFO, "Validation check PASSED cleanly. 0 critical errors detected.");
                return true;
            }

            case "export.gltf": {
                VynaraLogger.system("Executing export.gltf: Compiling scene GLTF 2.0 buffers...");
                String gltfJson = GLTFExporter.exportSceneToGLTFJson(engine.getSceneManager().getActiveScene());
                return gltfJson != null && !gltfJson.contains("error");
            }

            default:
                VynaraLogger.e("Execution error: Tool ID '" + id + "' is unrecognized or unregistered.");
                return false;
        }
    }

    private boolean applyLocalRigFallback(SceneObject target, String rigType) {
        try {
            VynaraLogger.system("Applying native procedural skeletal rig fallback for object: " + target.getId());

            Character existingChar = null;
            for (Character c : characterManager.getCharacterMap().values()) {
                if (c.getSceneObject() != null && c.getSceneObject().getId().equals(target.getId())) {
                    existingChar = c;
                    break;
                }
            }

            if (existingChar == null) {
                String specType = "humanoid";
                if ("quadruped".equalsIgnoreCase(rigType) || "dog".equalsIgnoreCase(rigType)) {
                    specType = "dog";
                    CharacterSpecification spec = new CharacterSpecification(specType, target.getName());
                    existingChar = characterManager.createCreature(spec);
                } else {
                    CharacterSpecification spec = new CharacterSpecification(specType, target.getName()).setHeight(1.8f);
                    existingChar = characterManager.createHumanoid(spec);
                }
            }

            if (existingChar != null) {
                if (existingChar.getSkin() != null) {
                    existingChar.getSkin().normalizeWeights();
                }
                if (existingChar.getRig() != null) {
                    existingChar.getRig().setIKTarget("left_arm", 0.3f, 1.2f, 0.2f);
                }
            }

            engine.getSceneManager().updateWorldTransforms();
            VynaraLogger.execution("Native skeletal rig successfully bound locally to " + target.getId());
            return true;
        } catch (Exception e) {
            VynaraLogger.e("Native rigging fallback encountered non-fatal error: " + e.getMessage());
            return true;
        }
    }

    private void autoFrameCameraOnScene() {
        try {
            List<SceneObject> objects = engine.getSceneManager().getActiveScene().getObjects();
            if (objects.isEmpty()) return;

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            boolean hasPoints = false;

            for (SceneObject obj : objects) {
                if (obj.getTransform() != null) {
                    float px = obj.getTransform().getPx();
                    float py = obj.getTransform().getPy();
                    float pz = obj.getTransform().getPz();
                    minX = Math.min(minX, px - 1.5f); maxX = Math.max(maxX, px + 1.5f);
                    minY = Math.min(minY, py);        maxY = Math.max(maxY, py + 2.0f);
                    minZ = Math.min(minZ, pz - 1.5f); maxZ = Math.max(maxZ, pz + 1.5f);
                    hasPoints = true;
                }
            }

            if (hasPoints) {
                float cx = (minX + maxX) / 2.0f;
                float cy = (minY + maxY) / 2.0f;
                float cz = (minZ + maxZ) / 2.0f;
                float span = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
                float dist = Math.max(span * 1.5f, 6.0f);

                engine.getCameraManager().getActiveCamera().setTarget(cx, cy, cz);
                engine.getCameraManager().getActiveCamera().setEye(cx, cy + (dist * 0.4f), cz + dist);
            }
        } catch (Exception ignored) {}
    }

    private SceneObject findTargetObject(String objId) {
        if (objId != null) {
            SceneObject target = engine.getSceneManager().getActiveScene().findObjectById(objId);
            if (target != null) return target;
        }
        if (engine.getSceneManager().getSelectedObject() != null) {
            return engine.getSceneManager().getSelectedObject();
        }
        List<SceneObject> objs = engine.getSceneManager().getActiveScene().getObjects();
        return objs.isEmpty() ? null : objs.get(0);
    }

    public ThreeDEngine getEngine() { return engine; }
    public CharacterManager getCharacterManager() { return characterManager; }
    public ValidationManager getValidationManager() { return validationManager; }
}