package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.texture.Texture;
import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GlContextManager {

    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_SPRITE = 4;
    private static final int INDICES_PER_SPRITE = 6;

    private final int batchSize;
    private final FloatBuffer vertexBuffer;

    public int vaoId;
    public int vboId;
    public int eboId;
    public int shaderProgram;
    public int uProjectionLocation;
    public int uTextureLocation;

    @Getter
    private static Texture whiteTexture;

    @Getter
    private final float[] projectionMatrix = new float[16];

    public GlContextManager(int batchSize, FloatBuffer vertexBuffer) {
        this.batchSize = batchSize;
        this.vertexBuffer = vertexBuffer;
    }

    public void setTextureWrap(int textureId, int wrapMode) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapMode);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapMode);
    }

    public void init() {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, ShaderSources.VERTEX_SHADER);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, ShaderSources.FRAGMENT_SHADER);

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("Shader link failed: " + GL20.glGetProgramInfoLog(shaderProgram));
        }
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        uProjectionLocation = GL20.glGetUniformLocation(shaderProgram, "uProjection");
        uTextureLocation = GL20.glGetUniformLocation(shaderProgram, "uTexture");

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX * Float.BYTES,
                GL15.GL_DYNAMIC_DRAW);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        int[] indices = new int[batchSize * INDICES_PER_SPRITE];
        for (int i = 0; i < batchSize; i++) {
            int offset = i * VERTICES_PER_SPRITE;
            int indexOffset = i * INDICES_PER_SPRITE;
            indices[indexOffset] = offset;
            indices[indexOffset + 1] = offset + 1;
            indices[indexOffset + 2] = offset + 2;
            indices[indexOffset + 3] = offset + 2;
            indices[indexOffset + 4] = offset + 3;
            indices[indexOffset + 5] = offset;
        }
        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.length);
        indicesBuffer.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        // Атрибуты VAO
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 4 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1i(uTextureLocation, 0);
        GL20.glUseProgram(0);


        whiteTexture = createWhiteTexture();
    }

    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Shader compilation error: " + GL20.glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    public void flushBatch(int spriteCount) {
        if (spriteCount <= 0) return;

        // 💡 Убедимся, что привязаны все буферы
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        // 🧠 Установка лимита и позиции перед заливкой данных
        vertexBuffer.limit(spriteCount * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
        vertexBuffer.position(0);

        // 🚀 Отправка данных в VBO
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // ✅ Отрисовка индексов
        GL11.glDrawElements(GL11.GL_TRIANGLES, spriteCount * INDICES_PER_SPRITE, GL11.GL_UNSIGNED_INT, 0);

        // 🧹 Очистка буфера для следующего кадра
        vertexBuffer.clear();
    }

    public void prepareRenderFrame(Color backgroundColor) {
        if (backgroundColor == null) {
            GL11.glClearColor(0f, 0f, 0f, 0f);
        } else {
            GL11.glClearColor(
                    backgroundColor.getR() / 255f,
                    backgroundColor.getG() / 255f,
                    backgroundColor.getB() / 255f,
                    1f);
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        vertexBuffer.clear();
    }

    public void postRenderFrame() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    public void setProjection(int width, int height) {
        GL11.glViewport(0, 0, width, height);

        float l = 0;
        float r = width;
        float t = 0;
        float b = height;
        float n = -1;
        float f = 1;
        for (int i = 0; i < 16; i++) projectionMatrix[i] = 0.0f;
        projectionMatrix[0] = 2.0f / (r - l);
        projectionMatrix[5] = 2.0f / (t - b);
        projectionMatrix[10] = -2.0f / (f - n);
        projectionMatrix[12] = -(r + l) / (r - l);
        projectionMatrix[13] = -(t + b) / (t - b);
        projectionMatrix[14] = -(f + n) / (f - n);
        projectionMatrix[15] = 1.0f;
    }

    public void setTextureFilter(int textureId, int filter) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
    }

    private static Texture createWhiteTexture() {
        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        buffer.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255).flip(); // RGBA white

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        return new Texture(texId, 1, 1);
    }

}
