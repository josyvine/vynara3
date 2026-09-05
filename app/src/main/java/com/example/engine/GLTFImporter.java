package com.example.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.character.Bone;
import com.example.character.Character;
import com.example.character.CharacterSpecification;
import com.example.character.Skeleton;
import com.example.utils.VynaraLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GLTFImporter {
    private static final int GLB_MAGIC = 0x46546C67; // 'glTF' in ASCII Little-Endian
    private static final int CHUNK_TYPE_JSON = 0x4E4F534A; // 'JSON' in ASCII Little-Endian
    private static final int CHUNK_TYPE_BIN = 0x004E4942;  // 'BIN\0' in ASCII Little-Endian

    public static class ImportResult {
        private final List<SceneObject> sceneObjects;
        private final List<Character> characters;

        public ImportResult(List<SceneObject> sceneObjects, List<Character> characters) {
            this.sceneObjects = sceneObjects;
            this.characters = characters;
        }

        public List<SceneObject> getSceneObjects() {
            return sceneObjects;
        }

        public List<Character> getCharacters() {
            return characters;
        }

        public boolean isEmpty() {
            return sceneObjects.isEmpty() && characters.isEmpty();
        }
    }

    public static ImportResult loadFromFile(File glbFile) throws Exception {
        if (glbFile == null || !glbFile.exists()) {
            throw new IllegalArgumentException("Target GLB file does not exist.");
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(glbFile))) {
            return loadFromStream(is);
        }
    }

    public static ImportResult loadFromStream(InputStream inputStream) throws Exception {
        byte[] fullBytes = readAllBytes(inputStream);
        ByteBuffer buffer = ByteBuffer.wrap(fullBytes).order(ByteOrder.LITTLE_ENDIAN);

        if (buffer.remaining() < 12) {
            throw new IllegalArgumentException("Invalid GLB container: File is too small for standard header.");
        }

        int magic = buffer.getInt();
        if (magic != GLB_MAGIC) {
            throw new IllegalArgumentException("Invalid GLB header magic. Expected 0x46546C67, got: 0x" + Integer.toHexString(magic));
        }

        int version = buffer.getInt();
        int totalLength = buffer.getInt();

        VynaraLogger.system("GLTFImporter: Parsing GLB binary version: " + version + ", bytes: " + totalLength);

        JSONObject jsonMetadata = null;
        byte[] binaryDataChunk = null;

        while (buffer.hasRemaining()) {
            if (buffer.remaining() < 8) break;
            int chunkLength = buffer.getInt();
            int chunkType = buffer.getInt();

            if (chunkLength < 0 || chunkLength > buffer.remaining()) {
                break;
            }

            byte[] chunkData = new byte[chunkLength];
            buffer.get(chunkData);

            if (chunkType == CHUNK_TYPE_JSON && jsonMetadata == null) {
                String jsonStr = new String(chunkData, StandardCharsets.UTF_8);
                jsonMetadata = new JSONObject(jsonStr);
            } else if (chunkType == CHUNK_TYPE_BIN && binaryDataChunk == null) {
                binaryDataChunk = chunkData;
            }
        }

        if (jsonMetadata == null) {
            throw new IllegalArgumentException("Corrupted GLB file: JSON chunk missing.");
        }
        if (binaryDataChunk == null) {
            binaryDataChunk = new byte[0];
        }

        return parseGLTFStructure(jsonMetadata, binaryDataChunk);
    }

    private static ImportResult parseGLTFStructure(JSONObject json, byte[] binaryBuffer) throws Exception {
        List<SceneObject> sceneObjects = new ArrayList<>();
        List<Character> characters = new ArrayList<>();

        JSONArray bufferViewsJson = json.optJSONArray("bufferViews");
        JSONArray accessorsJson = json.optJSONArray("accessors");
        JSONArray meshesJson = json.optJSONArray("meshes");
        JSONArray materialsJson = json.optJSONArray("materials");
        JSONArray nodesJson = json.optJSONArray("nodes");
        JSONArray skinsJson = json.optJSONArray("skins");
        JSONArray imagesJson = json.optJSONArray("images");
        JSONArray texturesJson = json.optJSONArray("textures");

        // 1. Decode Embedded Image Buffers into Bitmaps
        List<Bitmap> decodedBitmaps = new ArrayList<>();
        if (imagesJson != null && bufferViewsJson != null) {
            for (int i = 0; i < imagesJson.length(); i++) {
                JSONObject imgObj = imagesJson.getJSONObject(i);
                Bitmap bitmap = null;

                if (imgObj.has("bufferView")) {
                    int bvIdx = imgObj.getInt("bufferView");
                    if (bvIdx < bufferViewsJson.length()) {
                        JSONObject bv = bufferViewsJson.getJSONObject(bvIdx);
                        int byteOffset = bv.optInt("byteOffset", 0);
                        int byteLength = bv.getInt("byteLength");

                        if (byteOffset + byteLength <= binaryBuffer.length) {
                            try {
                                bitmap = BitmapFactory.decodeByteArray(binaryBuffer, byteOffset, byteLength);
                            } catch (Exception e) {
                                VynaraLogger.e("GLTFImporter: Failed decoding embedded texture #" + i, e);
                            }
                        }
                    }
                }
                decodedBitmaps.add(bitmap);
            }
        }

        // 2. Parse Materials & Link Diffuse/Albedo Textures
        List<Material> parsedMaterials = new ArrayList<>();
        if (materialsJson != null) {
            for (int i = 0; i < materialsJson.length(); i++) {
                JSONObject matObj = materialsJson.getJSONObject(i);
                String matName = matObj.optString("name", "Mat_" + i);
                float r = 0.8f, g = 0.8f, b = 0.8f, a = 1.0f;
                float metallic = 0.1f, roughness = 0.5f;
                Bitmap baseTextureBitmap = null;

                JSONObject pbr = matObj.optJSONObject("pbrMetallicRoughness");
                if (pbr != null) {
                    JSONArray baseColorArr = pbr.optJSONArray("baseColorFactor");
                    if (baseColorArr != null && baseColorArr.length() >= 3) {
                        r = (float) baseColorArr.getDouble(0);
                        g = (float) baseColorArr.getDouble(1);
                        b = (float) baseColorArr.getDouble(2);
                        if (baseColorArr.length() >= 4) {
                            a = (float) baseColorArr.getDouble(3);
                        }
                    }
                    metallic = (float) pbr.optDouble("metallicFactor", 0.1);
                    roughness = (float) pbr.optDouble("roughnessFactor", 0.5);

                    JSONObject baseTexObj = pbr.optJSONObject("baseColorTexture");
                    if (baseTexObj != null && texturesJson != null) {
                        int texIdx = baseTexObj.optInt("index", -1);
                        if (texIdx >= 0 && texIdx < texturesJson.length()) {
                            JSONObject texObj = texturesJson.getJSONObject(texIdx);
                            int sourceImgIdx = texObj.optInt("source", -1);
                            if (sourceImgIdx >= 0 && sourceImgIdx < decodedBitmaps.size()) {
                                baseTextureBitmap = decodedBitmaps.get(sourceImgIdx);
                            }
                        }
                    }
                }

                Material material = new Material("mat_" + i, matName, r, g, b, a);
                material.setMetallic(metallic);
                material.setRoughness(roughness);
                if (baseTextureBitmap != null) {
                    material.setTextureBitmap(baseTextureBitmap);
                }
                parsedMaterials.add(material);
            }
        }

        // 3. Parse Mesh Primitives & Material Indices
        List<Mesh> parsedMeshes = new ArrayList<>();
        List<Integer> meshMaterialIndices = new ArrayList<>();

        if (meshesJson != null) {
            for (int m = 0; m < meshesJson.length(); m++) {
                JSONObject meshObj = meshesJson.getJSONObject(m);
                JSONArray primitives = meshObj.optJSONArray("primitives");

                if (primitives != null && primitives.length() > 0) {
                    JSONObject prim = primitives.getJSONObject(0);
                    JSONObject attributes = prim.optJSONObject("attributes");

                    float[] positions = null;
                    float[] normals = null;
                    float[] uvs = null;
                    short[] indices = null;

                    if (attributes != null) {
                        if (attributes.has("POSITION")) {
                            int posAccessorIdx = attributes.getInt("POSITION");
                            positions = readFloatAccessor(posAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                        }

                        if (attributes.has("NORMAL")) {
                            int normAccessorIdx = attributes.getInt("NORMAL");
                            normals = readFloatAccessor(normAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                        }

                        if (attributes.has("TEXCOORD_0")) {
                            int uvAccessorIdx = attributes.getInt("TEXCOORD_0");
                            uvs = readFloatAccessor(uvAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                        }
                    }

                    if (prim.has("indices")) {
                        int indicesAccessorIdx = prim.getInt("indices");
                        indices = readShortAccessor(indicesAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                    }

                    if (positions == null) {
                        positions = new float[]{-0.5f, 0, 0,  0.5f, 0, 0,  0, 1.0f, 0};
                    }
                    if (normals == null) {
                        normals = new float[positions.length];
                        for (int n = 0; n < normals.length; n += 3) {
                            normals[n] = 0; normals[n+1] = 1.0f; normals[n+2] = 0;
                        }
                    }
                    if (uvs == null) {
                        uvs = new float[(positions.length / 3) * 2];
                    }
                    if (indices == null) {
                        indices = new short[(short) (positions.length / 3)];
                        for (short s = 0; s < indices.length; s++) indices[s] = s;
                    }

                    Mesh mesh = new Mesh(positions, normals, uvs, indices);
                    parsedMeshes.add(mesh);

                    int matIdx = prim.optInt("material", -1);
                    meshMaterialIndices.add(matIdx);
                }
            }
        }

        // 4. Parse Bone Skeletons
        Map<Integer, Bone> boneNodeMap = new HashMap<>();
        List<Skeleton> parsedSkeletons = new ArrayList<>();

        if (skinsJson != null && nodesJson != null) {
            for (int s = 0; s < skinsJson.length(); s++) {
                JSONObject skinObj = skinsJson.getJSONObject(s);
                JSONArray joints = skinObj.optJSONArray("joints");

                if (joints != null && joints.length() > 0) {
                    Bone rootBone = null;
                    for (int j = 0; j < joints.length(); j++) {
                        int nodeIdx = joints.getInt(j);
                        JSONObject nodeObj = nodesJson.getJSONObject(nodeIdx);
                        String boneName = nodeObj.optString("name", "bone_" + nodeIdx);

                        Bone bone = new Bone("bone_" + nodeIdx, boneName);
                        boneNodeMap.put(nodeIdx, bone);
                        if (j == 0) {
                            rootBone = bone;
                        }
                    }

                    for (int j = 0; j < joints.length(); j++) {
                        int nodeIdx = joints.getInt(j);
                        JSONObject nodeObj = nodesJson.getJSONObject(nodeIdx);
                        JSONArray children = nodeObj.optJSONArray("children");
                        if (children != null) {
                            Bone parentBone = boneNodeMap.get(nodeIdx);
                            for (int c = 0; c < children.length(); c++) {
                                int childNodeIdx = children.getInt(c);
                                Bone childBone = boneNodeMap.get(childNodeIdx);
                                if (parentBone != null && childBone != null) {
                                    parentBone.addChild(childBone);
                                }
                            }
                        }
                    }

                    if (rootBone != null) {
                        parsedSkeletons.add(new Skeleton(rootBone));
                    }
                }
            }
        }

        // 5. Assemble Scene Nodes with Matching Specific Materials
        if (nodesJson != null) {
            for (int n = 0; n < nodesJson.length(); n++) {
                JSONObject nodeObj = nodesJson.getJSONObject(n);
                String nodeName = nodeObj.optString("name", "node_" + n);

                if (nodeObj.has("mesh")) {
                    int meshIdx = nodeObj.getInt("mesh");
                    if (meshIdx < parsedMeshes.size()) {
                        Mesh mesh = parsedMeshes.get(meshIdx);
                        int assignedMatIdx = (meshIdx < meshMaterialIndices.size()) ? meshMaterialIndices.get(meshIdx) : -1;

                        Material mat;
                        if (assignedMatIdx >= 0 && assignedMatIdx < parsedMaterials.size()) {
                            mat = parsedMaterials.get(assignedMatIdx);
                        } else if (!parsedMaterials.isEmpty()) {
                            mat = parsedMaterials.get(0);
                        } else {
                            mat = new Material("mat_def_" + n, "Default", 0.8f, 0.8f, 0.8f, 1.0f);
                        }

                        SceneObject sceneObject = new SceneObject("obj_" + n, nodeName, "MESH", mesh, mat);
                        applyNodeTransformToObject(nodeObj, sceneObject);

                        if (nodeObj.has("skin") && !parsedSkeletons.isEmpty()) {
                            CharacterSpecification spec = new CharacterSpecification("HUMANOID", nodeName);
                            Character character = new Character("char_" + n, spec, sceneObject, parsedSkeletons.get(0));
                            characters.add(character);
                        } else {
                            sceneObjects.add(sceneObject);
                        }
                    }
                }
            }
        }

        if (sceneObjects.isEmpty() && characters.isEmpty() && !parsedMeshes.isEmpty()) {
            for (int i = 0; i < parsedMeshes.size(); i++) {
                int assignedMatIdx = (i < meshMaterialIndices.size()) ? meshMaterialIndices.get(i) : -1;
                Material mat = (assignedMatIdx >= 0 && assignedMatIdx < parsedMaterials.size()) 
                        ? parsedMaterials.get(assignedMatIdx) 
                        : (parsedMaterials.isEmpty() ? new Material("mat_def", "Default", 0.8f, 0.8f, 0.8f, 1.0f) : parsedMaterials.get(0));

                SceneObject obj = new SceneObject("imported_obj_" + i, "Imported Mesh " + i, "MESH", parsedMeshes.get(i), mat);
                sceneObjects.add(obj);
            }
        }

        VynaraLogger.system("GLTFImporter: Import complete (" + sceneObjects.size() + " objects, " + characters.size() + " rigged characters)");
        return new ImportResult(sceneObjects, characters);
    }

    private static float[] readFloatAccessor(int accessorIndex, JSONArray accessors, JSONArray bufferViews, byte[] binaryData) throws Exception {
        JSONObject accessor = accessors.getJSONObject(accessorIndex);
        int count = accessor.getInt("count");
        String type = accessor.getString("type");
        int bufferViewIndex = accessor.getInt("bufferView");
        int byteOffset = accessor.optInt("byteOffset", 0);

        JSONObject bufferView = bufferViews.getJSONObject(bufferViewIndex);
        int viewByteOffset = bufferView.optInt("byteOffset", 0);

        int componentsPerElement = getComponentCount(type);
        float[] result = new float[count * componentsPerElement];

        ByteBuffer bb = ByteBuffer.wrap(binaryData).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(viewByteOffset + byteOffset);

        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getFloat();
        }
        return result;
    }

    private static short[] readShortAccessor(int accessorIndex, JSONArray accessors, JSONArray bufferViews, byte[] binaryData) throws Exception {
        JSONObject accessor = accessors.getJSONObject(accessorIndex);
        int count = accessor.getInt("count");
        int componentType = accessor.getInt("componentType");
        int bufferViewIndex = accessor.getInt("bufferView");
        int byteOffset = accessor.optInt("byteOffset", 0);

        JSONObject bufferView = bufferViews.getJSONObject(bufferViewIndex);
        int viewByteOffset = bufferView.optInt("byteOffset", 0);

        short[] result = new short[count];
        ByteBuffer bb = ByteBuffer.wrap(binaryData).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(viewByteOffset + byteOffset);

        for (int i = 0; i < count; i++) {
            if (componentType == 5123) { // UNSIGNED_SHORT
                result[i] = (short) (bb.getShort() & 0xFFFF);
            } else if (componentType == 5125) { // UNSIGNED_INT
                result[i] = (short) bb.getInt();
            } else if (componentType == 5121) { // UNSIGNED_BYTE
                result[i] = (short) (bb.get() & 0xFF);
            } else {
                result[i] = bb.getShort();
            }
        }
        return result;
    }

    private static int getComponentCount(String type) {
        switch (type) {
            case "SCALAR": return 1;
            case "VEC2": return 2;
            case "VEC3": return 3;
            case "VEC4":
            case "MAT2": return 4;
            case "MAT3": return 9;
            case "MAT4": return 16;
            default: return 1;
        }
    }

    private static void applyNodeTransformToObject(JSONObject nodeObj, SceneObject object) {
        Transform transform = object.getTransform();
        if (transform == null) return;

        JSONArray translation = nodeObj.optJSONArray("translation");
        if (translation != null && translation.length() >= 3) {
            transform.setPosition(
                    (float) translation.optDouble(0, 0.0),
                    (float) translation.optDouble(1, 0.0),
                    (float) translation.optDouble(2, 0.0)
            );
        }

        JSONArray scale = nodeObj.optJSONArray("scale");
        if (scale != null && scale.length() >= 3) {
            transform.setScale(
                    (float) scale.optDouble(0, 1.0),
                    (float) scale.optDouble(1, 1.0),
                    (float) scale.optDouble(2, 1.0)
            );
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[16384];
        int bytesRead;
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}