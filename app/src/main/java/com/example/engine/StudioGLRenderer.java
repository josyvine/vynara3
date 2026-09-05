package com.example.engine;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class StudioGLRenderer implements GLSurfaceView.Renderer {
    private final SceneManager sceneManager;
    private final CameraManager cameraManager;
    private final LightManager lightManager;

    private int programHandle;
    private int uMVPMatrixHandle;
    private int uModelMatrixHandle;
    private int uColorHandle;
    private int uLightPosHandle;
    private int uLightColorHandle;
    private int uAmbientColorHandle;
    private int uCameraPosHandle;
    private int uMetallicHandle;
    private int uRoughnessHandle;
    private int uEmissionHandle;
    private int uIsSelectedHandle;
    private int uTimeHandle;
    private int uIsWaterHandle;
    private int uTextureHandle;
    private int uHasTextureHandle;

    private int aPositionHandle;
    private int aNormalHandle;
    private int aTexCoordHandle;

    // Runtime clock tracking for procedural shaders
    private float runTime = 0.0f;

    // Studio Grid Buffer
    private FloatBuffer gridBuffer;
    private int gridVertexCount = 0;

    private final float[] mvpMatrix = new float[16];

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uModelMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec3 aNormal;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec3 vNormal;\n" +
            "varying vec3 vFragPos;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vFragPos = vec3(uModelMatrix * aPosition);\n" +
            "    vNormal = normalize(mat3(uModelMatrix) * aNormal);\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "}\n";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec3 vNormal;\n" +
            "varying vec3 vFragPos;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform vec4 uColor;\n" +
            "uniform vec3 uLightPos;\n" +
            "uniform vec3 uLightColor;\n" +
            "uniform vec3 uAmbientColor;\n" +
            "uniform vec3 uCameraPos;\n" +
            "uniform float uMetallic;\n" +
            "uniform float uRoughness;\n" +
            "uniform vec4 uEmission;\n" +
            "uniform float uIsSelected;\n" +
            "uniform float uTime;\n" +
            "uniform float uIsWater;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uHasTexture;\n" +
            "void main() {\n" +
            "    vec3 norm = normalize(vNormal);\n" +
            "    if (!gl_FrontFacing) {\n" +
            "        norm = -norm;\n" +
            "    }\n" +
            "    vec3 viewDir = normalize(uCameraPos - vFragPos);\n" +
            "    \n" +
            "    // Sample diffuse texture map if available, otherwise use base color\n" +
            "    vec4 baseColor = uColor;\n" +
            "    if (uHasTexture > 0.5) {\n" +
            "        vec4 texColor = texture2D(uTexture, vTexCoord);\n" +
            "        baseColor = texColor * uColor;\n" +
            "    }\n" +
            "    \n" +
            "    // Dynamic procedural water wave calculations\n" +
            "    if (uIsWater > 0.5) {\n" +
            "        float waveX = sin(vFragPos.x * 3.0 + uTime * 2.5) * 0.12;\n" +
            "        float waveZ = cos(vFragPos.z * 3.0 + uTime * 2.0) * 0.12;\n" +
            "        norm = normalize(norm + vec3(waveX, 0.0, waveZ));\n" +
            "    }\n" +
            "    \n" +
            "    vec3 lightDir = normalize(uLightPos - vFragPos);\n" +
            "    vec3 halfDir = normalize(lightDir + viewDir);\n" +
            "    \n" +
            "    // Diffuse Reflection with natural wrap lighting\n" +
            "    float diff = max(dot(norm, lightDir), 0.0) + 0.15 * max(dot(-norm, lightDir), 0.0);\n" +
            "    vec3 diffuse = diff * uLightColor;\n" +
            "    \n" +
            "    // Specular Reflection (Cook-Torrance Approximation)\n" +
            "    float specAngle = max(dot(norm, halfDir), 0.0);\n" +
            "    float specPower = mix(8.0, 128.0, 1.0 - uRoughness);\n" +
            "    float spec = pow(specAngle, specPower) * mix(0.1, 1.0, uMetallic);\n" +
            "    vec3 specular = spec * uLightColor;\n" +
            "    \n" +
            "    // Hemisphere Image-Based Ambient Lighting (Sky/Ground gradient)\n" +
            "    float hemiFactor = clamp(norm.y * 0.5 + 0.5, 0.0, 1.0);\n" +
            "    vec3 hemiLight = mix(vec3(0.25, 0.22, 0.2), vec3(0.65, 0.75, 0.9), hemiFactor);\n" +
            "    vec3 ambient = uAmbientColor * baseColor.rgb * hemiLight * 1.6;\n" +
            "    \n" +
            "    vec3 finalColor = ambient + baseColor.rgb * diffuse + specular + uEmission.rgb * uEmission.a;\n" +
            "    \n" +
            "    // Dynamic Fresnel factor for water transmission\n" +
            "    if (uIsWater > 0.5) {\n" +
            "        float fresnel = pow(1.0 - max(dot(norm, viewDir), 0.0), 3.0);\n" +
            "        vec3 waterColor = mix(vec3(0.12, 0.65, 0.95), vec3(0.05, 0.35, 0.75), fresnel);\n" +
            "        finalColor = waterColor + specular;\n" +
            "    }\n" +
            "    \n" +
            "    // Cyan Highlight Overlay when Selected\n" +
            "    if (uIsSelected > 0.5) {\n" +
            "        finalColor = mix(finalColor, vec3(0.0, 0.9, 1.0), 0.4);\n" +
            "    }\n" +
            "    \n" +
            "    // sRGB Gamma Correction for rich, realistic depth\n" +
            "    finalColor = pow(finalColor, vec3(1.0 / 2.2));\n" +
            "    \n" +
            "    float alpha = uIsWater > 0.5 ? 0.65 : baseColor.a;\n" +
            "    gl_FragColor = vec4(finalColor, alpha);\n" +
            "}\n";

    public StudioGLRenderer(SceneManager sceneManager, CameraManager cameraManager, LightManager lightManager) {
        this.sceneManager = sceneManager;
        this.cameraManager = cameraManager;
        this.lightManager = lightManager;
        initGridBuffer();
    }

    private void initGridBuffer() {
        int gridSize = 24;
        float[] gridVertices = new float[(gridSize * 2 + 1) * 4 * 3];
        int idx = 0;
        for (int i = -gridSize; i <= gridSize; i++) {
            // X lines
            gridVertices[idx++] = -gridSize; gridVertices[idx++] = 0f; gridVertices[idx++] = i;
            gridVertices[idx++] = gridSize;  gridVertices[idx++] = 0f; gridVertices[idx++] = i;
            // Z lines
            gridVertices[idx++] = i; gridVertices[idx++] = 0f; gridVertices[idx++] = -gridSize;
            gridVertices[idx++] = i; gridVertices[idx++] = 0f; gridVertices[idx++] = gridSize;
        }
        gridVertexCount = gridVertices.length / 3;

        ByteBuffer bb = ByteBuffer.allocateDirect(gridVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        gridBuffer = bb.asFloatBuffer();
        gridBuffer.put(gridVertices);
        gridBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.07f, 0.08f, 0.11f, 1.0f); // Dark studio canvas #12131C
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, vertexShader);
        GLES20.glAttachShader(programHandle, fragmentShader);
        GLES20.glLinkProgram(programHandle);

        uMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        uModelMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uModelMatrix");
        uColorHandle = GLES20.glGetUniformLocation(programHandle, "uColor");
        uLightPosHandle = GLES20.glGetUniformLocation(programHandle, "uLightPos");
        uLightColorHandle = GLES20.glGetUniformLocation(programHandle, "uLightColor");
        uAmbientColorHandle = GLES20.glGetUniformLocation(programHandle, "uAmbientColor");
        uCameraPosHandle = GLES20.glGetUniformLocation(programHandle, "uCameraPos");
        uMetallicHandle = GLES20.glGetUniformLocation(programHandle, "uMetallic");
        uRoughnessHandle = GLES20.glGetUniformLocation(programHandle, "uRoughness");
        uEmissionHandle = GLES20.glGetUniformLocation(programHandle, "uEmission");
        uIsSelectedHandle = GLES20.glGetUniformLocation(programHandle, "uIsSelected");
        uTimeHandle = GLES20.glGetUniformLocation(programHandle, "uTime");
        uIsWaterHandle = GLES20.glGetUniformLocation(programHandle, "uIsWater");
        uTextureHandle = GLES20.glGetUniformLocation(programHandle, "uTexture");
        uHasTextureHandle = GLES20.glGetUniformLocation(programHandle, "uHasTexture");

        aPositionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition");
        aNormalHandle = GLES20.glGetAttribLocation(programHandle, "aNormal");
        aTexCoordHandle = GLES20.glGetAttribLocation(programHandle, "aTexCoord");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        cameraManager.getActiveCamera().updateProjectionMatrix(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(programHandle);

        Camera camera = cameraManager.getActiveCamera();
        float[] viewMatrix = camera.getViewMatrix();
        float[] projMatrix = camera.getProjectionMatrix();
        float[] cameraEye = camera.getEye();

        GLES20.glUniform3fv(uCameraPosHandle, 1, cameraEye, 0);

        // Update runtime frame time tick
        runTime += 0.016f;
        GLES20.glUniform1f(uTimeHandle, runTime);

        // Bind Lighting Uniforms
        Light mainLight = lightManager.getPrimaryDirectionalLight();
        Light ambientLight = lightManager.getAmbientLight();

        if (mainLight != null) {
            GLES20.glUniform3fv(uLightPosHandle, 1, mainLight.getPosition(), 0);
            GLES20.glUniform3f(uLightColorHandle, 
                    mainLight.getColorRGB()[0] * mainLight.getIntensity(),
                    mainLight.getColorRGB()[1] * mainLight.getIntensity(),
                    mainLight.getColorRGB()[2] * mainLight.getIntensity());
        } else {
            GLES20.glUniform3f(uLightPosHandle, 8f, 15f, 10f);
            GLES20.glUniform3f(uLightColorHandle, 1f, 1f, 1f);
        }

        if (ambientLight != null) {
            GLES20.glUniform3f(uAmbientColorHandle, 
                    ambientLight.getColorRGB()[0] * ambientLight.getIntensity(),
                    ambientLight.getColorRGB()[1] * ambientLight.getIntensity(),
                    ambientLight.getColorRGB()[2] * ambientLight.getIntensity());
        } else {
            GLES20.glUniform3f(uAmbientColorHandle, 0.35f, 0.35f, 0.4f);
        }

        // 1. Render Ground Grid (Opaque pass, no textures)
        drawGrid(viewMatrix, projMatrix);

        // 2. Render Active 3D Scene Graph Nodes
        Scene scene = sceneManager.getActiveScene();
        if (scene != null) {
            List<SceneObject> flatList = scene.getFlatObjectList();
            List<RenderTask> opaqueTasks = new ArrayList<>();
            List<RenderTask> translucentTasks = new ArrayList<>();

            for (SceneObject obj : flatList) {
                if (obj.getMesh() == null || !obj.isVisible()) continue;
                
                float[] modelMatrix = obj.getTransform().getWorldMatrix(null);
                boolean isTranslucent = obj.getMaterial() != null && obj.getMaterial().getOpacity() < 1.0f;
                
                RenderTask task = new RenderTask(obj, modelMatrix);
                if (isTranslucent) {
                    float dx = obj.getTransform().getPx() - cameraEye[0];
                    float dy = obj.getTransform().getPy() - cameraEye[1];
                    float dz = obj.getTransform().getPz() - cameraEye[2];
                    task.distanceToCamera = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                    translucentTasks.add(task);
                } else {
                    opaqueTasks.add(task);
                }
            }

            // Opaque Pass (Depth Writing Enabled)
            GLES20.glDepthMask(true);
            for (RenderTask task : opaqueTasks) {
                drawTask(task, viewMatrix, projMatrix);
            }

            // Depth-sorted Translucent Pass (Depth Writing Disabled)
            Collections.sort(translucentTasks, (t1, t2) -> Float.compare(t2.distanceToCamera, t1.distanceToCamera));
            GLES20.glDepthMask(false);
            for (RenderTask task : translucentTasks) {
                drawTask(task, viewMatrix, projMatrix);
            }
            GLES20.glDepthMask(true);
        }
    }

    private void drawGrid(float[] viewMatrix, float[] projMatrix) {
        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, identity, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, identity, 0);
        GLES20.glUniform4f(uColorHandle, 0.2f, 0.25f, 0.35f, 0.5f);
        GLES20.glUniform1f(uMetallicHandle, 0.0f);
        GLES20.glUniform1f(uRoughnessHandle, 1.0f);
        GLES20.glUniform4f(uEmissionHandle, 0f, 0f, 0f, 0f);
        GLES20.glUniform1f(uIsSelectedHandle, 0.0f);
        GLES20.glUniform1f(uIsWaterHandle, 0.0f);
        GLES20.glUniform1f(uHasTextureHandle, 0.0f);

        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, gridBuffer);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexCount);
        GLES20.glDisableVertexAttribArray(aPositionHandle);
    }

    private void drawTask(RenderTask task, float[] viewMatrix, float[] projMatrix) {
        SceneObject obj = task.obj;
        Mesh mesh = obj.getMesh();
        float[] modelMatrix = task.modelMatrix;

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);

        // Bind Material Uniforms
        Material mat = obj.getMaterial();
        float isWater = 0.0f;

        if (mat != null) {
            float[] color = mat.getBaseColorRGBA();
            GLES20.glUniform4f(uColorHandle, color[0], color[1], color[2], color[3]);
            GLES20.glUniform1f(uMetallicHandle, mat.getMetallic());
            GLES20.glUniform1f(uRoughnessHandle, mat.getRoughness());
            
            float[] emissiveRGB = mat.getEmissionRGB();
            GLES20.glUniform4f(uEmissionHandle, emissiveRGB[0], emissiveRGB[1], emissiveRGB[2], mat.getEmissionIntensity());

            // Broad check: activate wave simulation for any water, pool, or ocean material
            String matId = mat.getId() != null ? mat.getId().toLowerCase() : "";
            String matName = mat.getName() != null ? mat.getName().toLowerCase() : "";
            if (matId.contains("water") || matId.contains("ocean") || matName.contains("water") || matName.contains("ocean")) {
                isWater = 1.0f;
            }

            // GPU Texture Staging: If a bitmap was decoded on a background thread, upload it now on the GL thread
            if (mat.hasTextureBitmap() && mat.getTextureId() == 0) {
                int[] texIds = new int[1];
                GLES20.glGenTextures(1, texIds, 0);
                if (texIds[0] > 0) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0]);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mat.getTextureBitmap(), 0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                    mat.setTextureId(texIds[0]);
                    mat.clearTextureBitmap(); // Free RAM memory now that it's in GPU VRAM
                }
            }

            // Bind Texture Map if present on material
            if (mat.hasTexture() && mat.getTextureId() > 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mat.getTextureId());
                GLES20.glUniform1i(uTextureHandle, 0);
                GLES20.glUniform1f(uHasTextureHandle, 1.0f);
            } else {
                GLES20.glUniform1f(uHasTextureHandle, 0.0f);
            }
        } else {
            GLES20.glUniform4f(uColorHandle, 0.8f, 0.8f, 0.8f, 1.0f);
            GLES20.glUniform1f(uMetallicHandle, 0.1f);
            GLES20.glUniform1f(uRoughnessHandle, 0.5f);
            GLES20.glUniform4f(uEmissionHandle, 0f, 0f, 0f, 0f);
            GLES20.glUniform1f(uHasTextureHandle, 0.0f);
        }

        GLES20.glUniform1f(uIsWaterHandle, isWater);
        GLES20.glUniform1f(uIsSelectedHandle, obj.isSelected() ? 1.0f : 0.0f);

        // Bind Buffers & Draw Mesh
        if (mesh.getVertexBuffer() != null) {
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.getVertexBuffer());
        }

        if (mesh.getNormalBuffer() != null) {
            GLES20.glEnableVertexAttribArray(aNormalHandle);
            GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.getNormalBuffer());
        }

        // Bind UV Texture coordinates
        if (mesh.getTexBuffer() != null) {
            GLES20.glEnableVertexAttribArray(aTexCoordHandle);
            GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mesh.getTexBuffer());
        }

        if (mesh.getIndexBuffer() != null) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.getIndices().length, GLES20.GL_UNSIGNED_SHORT, mesh.getIndexBuffer());
        } else if (mesh.getVertexBuffer() != null) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.getVertexCount());
        }

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aNormalHandle);
        GLES20.glDisableVertexAttribArray(aTexCoordHandle);

        // Unbind texture unit to prevent state bleed
        if (mat != null && mat.hasTexture()) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private static class RenderTask {
        final SceneObject obj;
        final float[] modelMatrix;
        float distanceToCamera = 0.0f;

        RenderTask(SceneObject obj, float[] modelMatrix) {
            this.obj = obj;
            this.modelMatrix = modelMatrix;
        }
    }
}