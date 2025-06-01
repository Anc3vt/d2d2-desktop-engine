package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.event.SceneEvent;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;
import com.ancevt.d2d2.time.Timer;
import lombok.RequiredArgsConstructor;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class DesktopRenderer implements Renderer {

    private final DesktopEngine engine;

    boolean running = true;

    public static int batchSize = 20000;

    private static final int FLOATS_PER_VERTEX = 8; // x, y, u, v, r, g, b, a
    private static final int VERTICES_PER_SPRITE = 4;
    private static final int INDICES_PER_SPRITE = 6;

    private int vaoId;
    private int vboId;
    private int eboId;
    private int shaderProgram;

    private int uProjectionLocation;
    private int uTextureLocation;

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
    private final float[] projectionMatrix = new float[16];

    @Override
    public void init(long windowId) {
        String vertexShaderSrc =
                "#version 330 core\n" +
                        "layout(location = 0) in vec2 aPos;\n" +
                        "layout(location = 1) in vec2 aTexCoord;\n" +
                        "layout(location = 2) in vec4 aColor;\n" +
                        "uniform mat4 uProjection;\n" +
                        "out vec2 vTexCoord;\n" +
                        "out vec4 vColor;\n" +
                        "void main() {\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "    vColor = aColor;\n" +
                        "    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);\n" +
                        "}\n";

        String fragmentShaderSrc =
                "#version 330 core\n" +
                        "in vec2 vTexCoord;\n" +
                        "in vec4 vColor;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main() {\n" +
                        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
                        "    FragColor = texColor * vColor;\n" +
                        "}\n";

        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexShaderSrc);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSrc);

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("Ошибка линковки шейдера: " + GL20.glGetProgramInfoLog(shaderProgram));
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
    }

    public void setProjection(int width, int height) {
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

    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Shader compilation error: " + GL20.glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    @Override
    public void reshape() {
        GL11.glViewport(0, 0, engine.getCanvasWidth(), engine.getCanvasHeight());
        setProjection(engine.getCanvasWidth(), engine.getCanvasHeight());
    }

    @Override
    public void renderFrame() {
        Stage stage = engine.getStage();

        Color backgroundColor = stage.getBackgroundColor();
        GL11.glClearColor(
                backgroundColor.getR() / 255f,
                backgroundColor.getG() / 255f,
                backgroundColor.getB() / 255f,
                1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        List<SpriteDrawInfo> spritesToDraw = new ArrayList<>();
        collectNodes(stage, 1f, 0f, 0f, 0f, 1f, 0f, spritesToDraw);

        spritesToDraw.sort(Comparator.comparingInt(info -> info.zOrder));

        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(vaoId);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        vertexBuffer.clear();

        int spritesInBatch = 0;
        int currentTextureId = -1;

        for (SpriteDrawInfo info : spritesToDraw) {

            Node node = info.node;
            node.preFrame();
            node.dispatchEvent(SceneEvent.PreFrame.create());

            int texId = info.textureId;
            if (currentTextureId == -1 || texId != currentTextureId || spritesInBatch >= batchSize) {
                if (spritesInBatch > 0) flushBatch(spritesInBatch);
                spritesInBatch = 0;
                currentTextureId = texId;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
            }

            float r = 1f, g = 1f, b = 1f, alpha = info.node != null ? info.node.getAlpha() : 1f;
            if (info.node instanceof Colored colored) {
                Color c = colored.getColor();
                r = c.getR() / 255f;
                g = c.getG() / 255f;
                b = c.getB() / 255f;
            }

            float x = info.x, y = info.y;
            float a = info.a, bb = info.b, d = info.d, e = info.e;
            float w = info.width, h = info.height;

            float x0 = x, y0 = y;
            float x1 = a * w + x, y1 = d * w + y;
            float x2 = a * w + bb * h + x, y2 = d * w + e * h + y;
            float x3 = bb * h + x, y3 = e * h + y;

            float u0 = 0f, v0 = 1f;
            float u1 = 1f, v1 = 1f;
            float u2 = 1f, v2 = 0f;
            float u3 = 0f, v3 = 0f;

            if (info.node instanceof Sprite sprite && sprite.getTextureRegion() != null) {
                TextureRegion region = sprite.getTextureRegion();
                Texture texture = region.getTexture();

                float texW = texture.getWidth();
                float texH = texture.getHeight();

                float regionX = region.getX();
                float regionY = region.getY();
                float regionW = region.getWidth();
                float regionH = region.getHeight();

                float uLeft = regionX / texW;
                float uRight = (regionX + regionW) / texW;

                // 🧠 Переворот по вертикали: берём сверху, как просили
                float vTop = (texH - regionY - regionH) / texH;
                float vBottom = (texH - regionY) / texH;

                u0 = uLeft;  v0 = vBottom;
                u1 = uRight; v1 = vBottom;
                u2 = uRight; v2 = vTop;
                u3 = uLeft;  v3 = vTop;
            }

            int baseIndex = spritesInBatch * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX;

            float[] data = {
                    x0, y0, u0, v0, r, g, b, alpha,
                    x1, y1, u1, v1, r, g, b, alpha,
                    x2, y2, u2, v2, r, g, b, alpha,
                    x3, y3, u3, v3, r, g, b, alpha
            };
            vertexBuffer.position(baseIndex);
            vertexBuffer.put(data);

            node.postFrame();
            node.dispatchEvent(SceneEvent.PostFrame.create());

            spritesInBatch++;
        }

        if (spritesInBatch > 0) flushBatch(spritesInBatch);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }



    private void flushBatch(int spriteCount) {
        if (spriteCount == 0) return;
        vertexBuffer.limit(spriteCount * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
        vertexBuffer.position(0);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);
        GL11.glDrawElements(GL11.GL_TRIANGLES, spriteCount * INDICES_PER_SPRITE, GL11.GL_UNSIGNED_INT, 0);
        vertexBuffer.clear();
    }

    private static void collectNodes(Node node,
                                     float parentA, float parentB, float parentC,
                                     float parentD, float parentE, float parentF,
                                     List<SpriteDrawInfo> outList) {
        float x = node.getX(), y = node.getY();
        float scaleX = node.getScaleX(), scaleY = node.getScaleY();
        float rad = (float) Math.toRadians(node.getRotation());
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);

        float a2 = cos * scaleX, b2 = -sin * scaleY, d2 = sin * scaleX, e2 = cos * scaleY;
        float c2 = x, f2 = y;

        float a = parentA * a2 + parentB * d2;
        float b = parentA * b2 + parentB * e2;
        float c = parentA * c2 + parentB * f2 + parentC;
        float d = parentD * a2 + parentE * d2;
        float e = parentD * b2 + parentE * e2;
        float f = parentD * c2 + parentE * f2 + parentF;

        if (node instanceof Sprite sprite) {
            SpriteDrawInfo info = new SpriteDrawInfo();
            info.a = a;
            info.b = b;
            info.c = c;
            info.d = d;
            info.e = e;
            info.f = f;
            info.x = c;
            info.y = f;
            info.width = sprite.getWidth();
            info.height = sprite.getHeight();
            info.textureId = getSpriteTextureId(sprite);
            info.zOrder = sprite.getGlobalZOrderIndex();
            info.node = sprite;
            outList.add(info);
        }
        if (node instanceof Group group) {
            for (Node child : group.children().toList()) {
                collectNodes(child, a, b, c, d, e, f, outList);
            }
        }
    }

    private static int getSpriteTextureId(Sprite sprite) {
        if (sprite != null && sprite.getTextureRegion() != null) {
            return sprite.getTextureRegion().getTexture().getId();
        }
        return -1;
    }

    public void startRenderLoop() {
        long windowId = CanvasControl.getWindowId();

        while (!GLFW.glfwWindowShouldClose(windowId) && running) {
            GLFW.glfwPollEvents();
            renderFrame();
            GLFW.glfwSwapBuffers(windowId);
            Timer.processTimers();
        }

        GLFW.glfwTerminate();
    }

    private static class SpriteDrawInfo {
        float a, b, c, d, e, f;
        float x, y;
        float width, height;
        int textureId;
        int zOrder;
        Node node;
    }
}
