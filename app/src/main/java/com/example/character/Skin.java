package com.example.character;

import com.example.engine.Mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Skin {
    private Skeleton skeleton;
    private final List<List<SkinWeight>> vertexSkinWeights = new ArrayList<>();
    private static final int MAX_BONE_INFLUENCES_PER_VERTEX = 4;

    public Skin(Skeleton skeleton, int vertexCount) {
        this.skeleton = skeleton;
        initMultiBoneSkinning(vertexCount);
    }

    public Skin(Skeleton skeleton, Mesh mesh) {
        this.skeleton = skeleton;
        if (mesh != null && mesh.getVertices() != null && mesh.getVertices().length >= 3) {
            bindMeshVerticesToSkeleton(mesh.getVertices());
            applyToMesh(mesh); // CRITICAL AUTO-RIG BINDER
        } else {
            initMultiBoneSkinning(mesh != null ? mesh.getVertexCount() : 100);
            if (mesh != null) {
                applyToMesh(mesh); // CRITICAL AUTO-RIG BINDER FOR SYSTEM DEFAULT
            }
        }
    }

    /**
     * Phase 8 Alignment: Binds 3D mesh vertex positions to the closest skeleton bones
     * using proximity and segment projection distance-decay functions across up to 4 bone influences.
     */
    public void bindMeshVerticesToSkeleton(float[] vertexPositions) {
        vertexSkinWeights.clear();
        if (skeleton == null || skeleton.getAllBones().isEmpty() || vertexPositions == null) return;

        List<Bone> allBones = skeleton.getAllBones();
        int vertexCount = vertexPositions.length / 3;

        for (int i = 0; i < vertexCount; i++) {
            float vx = vertexPositions[i * 3];
            float vy = vertexPositions[i * 3 + 1];
            float vz = vertexPositions[i * 3 + 2];

            List<SkinWeight> vertexWeights = new ArrayList<>();

            // Calculate distance from vertex to the nearest segment or origin of each bone
            for (Bone bone : allBones) {
                if (bone == null || bone.getLocalTransform() == null) continue;

                float distance;
                if (!bone.getChildren().isEmpty()) {
                    // Bone has children: Treat as line segment connecting joint A (parent) to joint B (child)
                    Bone child = bone.getChildren().get(0);
                    float[] posA = getBoneBindPoseWorldPosition(bone);
                    float[] posB = getBoneBindPoseWorldPosition(child);

                    float abx = posB[0] - posA[0];
                    float aby = posB[1] - posA[1];
                    float abz = posB[2] - posA[2];

                    float apx = vx - posA[0];
                    float apy = vy - posA[1];
                    float apz = vz - posA[2];

                    float abLenSq = abx * abx + aby * aby + abz * abz;
                    float t = 0f;
                    if (abLenSq > 0.0001f) {
                        t = (apx * abx + apy * aby + apz * abz) / abLenSq;
                        t = Math.max(0.0f, Math.min(1.0f, t)); // Clamp projection to segment bounds
                    }

                    // Compute closest point coordinates on the bone segment
                    float closestX = posA[0] + t * abx;
                    float closestY = posA[1] + t * aby;
                    float closestZ = posA[2] + t * abz;

                    float dx = vx - closestX;
                    float dy = vy - closestY;
                    float dz = vz - closestZ;
                    distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                } else {
                    // Leaf bone: Calculate simple Euclidean distance to the joint origin
                    float[] posA = getBoneBindPoseWorldPosition(bone);
                    float dx = vx - posA[0];
                    float dy = vy - posA[1];
                    float dz = vz - posA[2];
                    distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                }

                // Inverse-distance weighting (exponential decay)
                float rawWeight = 1.0f / (distance * distance + 0.05f);
                vertexWeights.add(new SkinWeight(bone.getId(), rawWeight));
            }

            // Keep top MAX_BONE_INFLUENCES_PER_VERTEX
            Collections.sort(vertexWeights, (w1, w2) -> Float.compare(w2.getWeight(), w1.getWeight()));
            if (vertexWeights.size() > MAX_BONE_INFLUENCES_PER_VERTEX) {
                vertexWeights = new ArrayList<>(vertexWeights.subList(0, MAX_BONE_INFLUENCES_PER_VERTEX));
            }

            vertexSkinWeights.add(vertexWeights);
        }

        normalizeWeights();
    }

    /**
     * Phase 8 Alignment: Purged 100% root-only weighting. Distributes initial weights
     * across primary structural joint links (Spine, Pelvis) cleanly rather than arbitrary modulos.
     */
    private void initMultiBoneSkinning(int vertexCount) {
        vertexSkinWeights.clear();
        if (skeleton == null || skeleton.getAllBones().isEmpty()) return;

        List<Bone> bones = skeleton.getAllBones();

        // Dynamically locate pelvis or spine structures to act as default structural anchors
        Bone pelvis = skeleton.getBoneBySemanticName("PELVIS");
        Bone spine = skeleton.getBoneBySemanticName("SPINE");
        Bone root = skeleton.getRootBone();

        String primaryBoneId = pelvis != null ? pelvis.getId() : (root != null ? root.getId() : bones.get(0).getId());
        String secondaryBoneId = spine != null ? spine.getId() : primaryBoneId;

        for (int i = 0; i < vertexCount; i++) {
            List<SkinWeight> weights = new ArrayList<>();
            weights.add(new SkinWeight(primaryBoneId, 0.7f));
            if (!primaryBoneId.equals(secondaryBoneId)) {
                weights.add(new SkinWeight(secondaryBoneId, 0.3f));
            } else {
                weights.add(new SkinWeight(bones.get(0).getId(), 0.3f));
            }
            vertexSkinWeights.add(weights);
        }

        normalizeWeights();
    }

    /**
     * Flattens the multi-influence vertex skin weights (up to 4 bones per vertex)
     * and binds them directly to the native GPU and GLTF buffers of the target Mesh.
     */
    public void applyToMesh(Mesh mesh) {
        if (mesh == null || skeleton == null || vertexSkinWeights.isEmpty()) return;

        int vertexCount = vertexSkinWeights.size();
        float[] flatWeights = new float[vertexCount * MAX_BONE_INFLUENCES_PER_VERTEX];
        float[] flatIndices = new float[vertexCount * MAX_BONE_INFLUENCES_PER_VERTEX];

        for (int i = 0; i < vertexCount; i++) {
            List<SkinWeight> weights = vertexSkinWeights.get(i);
            int size = Math.min(MAX_BONE_INFLUENCES_PER_VERTEX, weights.size());

            for (int j = 0; j < size; j++) {
                SkinWeight sw = weights.get(j);
                
                int boneIndex = 0;
                Bone targetBone = skeleton.getBoneById(sw.getBoneId());
                if (targetBone != null) {
                    boneIndex = skeleton.getBoneIndex(targetBone.getSemanticName());
                    if (boneIndex < 0) boneIndex = 0;
                }

                flatWeights[i * MAX_BONE_INFLUENCES_PER_VERTEX + j] = sw.getWeight();
                flatIndices[i * MAX_BONE_INFLUENCES_PER_VERTEX + j] = (float) boneIndex;
            }

            // Pad remaining with zeros if vertex has < 4 bone influences
            for (int j = size; j < MAX_BONE_INFLUENCES_PER_VERTEX; j++) {
                flatWeights[i * MAX_BONE_INFLUENCES_PER_VERTEX + j] = 0.0f;
                flatIndices[i * MAX_BONE_INFLUENCES_PER_VERTEX + j] = 0.0f;
            }
        }

        mesh.setSkinningData(flatWeights, flatIndices);
    }

    /**
     * Resolves the absolute 3D world coordinates of a joint in default bind-pose 
     * by accumulating local transform translations up to the root bone.
     */
    private float[] getBoneBindPoseWorldPosition(Bone bone) {
        float[] pos = new float[] { 0f, 0f, 0f };
        Bone current = bone;
        while (current != null) {
            pos[0] += current.getLocalTransform().getPx();
            pos[1] += current.getLocalTransform().getPy();
            pos[2] += current.getLocalTransform().getPz();
            current = current.getParent();
        }
        return pos;
    }

    public void normalizeWeights() {
        for (List<SkinWeight> weights : vertexSkinWeights) {
            float sum = 0f;
            for (SkinWeight sw : weights) {
                sum += sw.getWeight();
            }
            if (sum > 0f) {
                for (SkinWeight sw : weights) {
                    sw.setWeight(sw.getWeight() / sum);
                }
            } else if (!weights.isEmpty()) {
                weights.get(0).setWeight(1.0f);
            }
        }
    }

    /**
     * Phase 8 & 11 Alignment: Validates that every vertex has normalized weights
     * and valid bone ID references.
     */
    public boolean validateSkinning() {
        if (skeleton == null || vertexSkinWeights.isEmpty()) return false;

        for (List<SkinWeight> weights : vertexSkinWeights) {
            if (weights == null || weights.isEmpty()) return false;

            float sum = 0f;
            for (SkinWeight sw : weights) {
                if (sw.getBoneId() == null || skeleton.getBoneById(sw.getBoneId()) == null) {
                    return false; // Invalid bone reference
                }
                sum += sw.getWeight();
            }

            if (Math.abs(sum - 1.0f) > 0.02f) {
                return false; // Non-normalized weights
            }
        }

        return true;
    }

    public Skeleton getSkeleton() { return skeleton; }
    public List<List<SkinWeight>> getVertexSkinWeights() { return vertexSkinWeights; }
}