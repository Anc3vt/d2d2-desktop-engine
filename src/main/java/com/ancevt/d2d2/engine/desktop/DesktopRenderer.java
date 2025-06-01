package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.StageEvent;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.shape.LineBatch;
import com.ancevt.d2d2.scene.shape.RectangleShape;
import com.ancevt.d2d2.scene.text.BitmapCharInfo;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;
import com.ancevt.d2d2.time.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
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

    private static Texture whiteTexture;

    private int lineVaoId;
    private int lineVboId;
    private static final int FLOATS_PER_LINE_VERTEX = 8;
    private static final int MAX_LINES = 8192;
    private final FloatBuffer lineBuffer = BufferUtils.createFloatBuffer(MAX_LINES * 2 * FLOATS_PER_LINE_VERTEX);

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
    private final float[] projectionMatrix = new float[16];

    @Getter
    private int actualFps;

    @Getter
    @Setter
    private int frameRate = 60;

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

        whiteTexture = createWhiteTexture();


        lineVaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(lineVaoId);

        lineVboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                MAX_LINES * 2 * FLOATS_PER_LINE_VERTEX * Float.BYTES,
                GL15.GL_DYNAMIC_DRAW);

// Атрибуты: x, y, u, v, r, g, b, a
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, FLOATS_PER_LINE_VERTEX * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, FLOATS_PER_LINE_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, FLOATS_PER_LINE_VERTEX * Float.BYTES, 4 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);


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

        var s = engine.getStage();
        s.dispatchEvent(CommonEvent.Resize.create(engine.getCanvasWidth(), engine.getCanvasHeight()));
        s.setSize(engine.getCanvasWidth(), engine.getCanvasHeight());

    }

    public static void setNearestFilter(int textureId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    @Override
    public void renderFrame() {
        List<DrawInfo> drawQueue = new ArrayList<>();

        Stage stage = engine.getStage();

        Color backgroundColor = stage.getBackgroundColor();
        GL11.glClearColor(
                backgroundColor.getR() / 255f,
                backgroundColor.getG() / 255f,
                backgroundColor.getB() / 255f,
                1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);


        collectNodes(stage, 1f, 0f, 0f, 0f, 1f, 0f, 1f, drawQueue);

        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(vaoId);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        vertexBuffer.clear();
        int spritesInBatch = 0;
        int currentTextureId = -1;

        for (DrawInfo info : drawQueue) {
            int texId = info.getTextureId();

            if (currentTextureId != texId || spritesInBatch >= batchSize) {
                if (spritesInBatch > 0) flushBatch(spritesInBatch);
                currentTextureId = texId;
                setNearestFilter(texId);
                spritesInBatch = 0;
                vertexBuffer.clear();
            }

            int addedSprites = info.render(vertexBuffer, this);
            spritesInBatch += addedSprites;
        }

        if (spritesInBatch > 0) flushBatch(spritesInBatch);


        ////-----

        if (spritesInBatch > 0) flushBatch(spritesInBatch);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);


    }

    private static void collectNodes(Node node, float a, float b, float c, float d, float e, float f, float alpha, List<DrawInfo> drawQueue) {
        float x = node.getX(), y = node.getY();
        float scaleX = node.getScaleX(), scaleY = node.getScaleY();
        float rad = (float) Math.toRadians(node.getRotation());
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);

        float a2 = cos * scaleX, b2 = -sin * scaleY, d2 = sin * scaleX, e2 = cos * scaleY;
        float c2 = x, f2 = y;

        float na = a * a2 + b * d2;
        float nb = a * b2 + b * e2;
        float nc = a * c2 + b * f2 + c;
        float nd = d * a2 + e * d2;
        float ne = d * b2 + e * e2;
        float nf = d * c2 + e * f2 + f;

        float newAlpha = alpha * node.getAlpha();

        if (node instanceof Sprite sprite) {
            drawQueue.add(new SpriteDrawInfo(sprite, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof BitmapText btx) {
            if (btx.isCacheAsSprite()) {
                collectNodes(btx.cachedSprite(), na, nb, nc, nd, ne, nf, newAlpha, drawQueue);
            } else {
                drawQueue.add(new BitmapTextDrawInfo(btx, na, nb, nc, nd, ne, nf, newAlpha));
            }
        } else if (node instanceof RectangleShape rect) {
            drawQueue.add(new ShapeDrawInfo(rect, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof LineBatch lb) {
            drawQueue.add(new LineDrawInfo(lb, na, nb, nc, nd, ne, nf, newAlpha));
        }

        if (node instanceof Group group) {
            for (Node child : group.children().toList()) {
                collectNodes(child, na, nb, nc, nd, ne, nf, newAlpha, drawQueue);
            }
        }
    }


    private void flushBatch(int spriteCount) {
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

    private static int getSpriteTextureId(Sprite sprite) {
        if (sprite != null && sprite.getTextureRegion() != null) {
            return sprite.getTextureRegion().getTexture().getId();
        }
        return -1;
    }

    public void startRenderLoop() {
        long windowId = CanvasControl.getWindowId();

        Stage stage = engine.getStage();

        long lastTime = System.nanoTime();
        long accumulator = 0L;
        long lastRenderTime = System.nanoTime();

        int frames = 0;
        long fpsTimer = System.currentTimeMillis();

        while (!GLFW.glfwWindowShouldClose(windowId) && running) {
            final long tickInterval = 1_000_000_000L / (frameRate + 10);
            final long frameInterval = 1_000_000_000L / (frameRate + 10);

            long now = System.nanoTime();
            long delta = now - lastTime;
            lastTime = now;
            accumulator += delta;

            // ✅ Tick логики
            while (accumulator >= tickInterval) {
                Timer.processTimers();
                stage.dispatchEvent(StageEvent.Tick.create());
                accumulator -= tickInterval;
            }

            // ✅ Ограничим рендер частотой frameRate
            if (now - lastRenderTime >= frameInterval) {
                stage.dispatchEvent(StageEvent.PreFrame.create());
                renderFrame();
                stage.dispatchEvent(StageEvent.PostFrame.create());
                GLFW.glfwSwapBuffers(windowId);
                lastRenderTime = now;
                frames++;
            }

            // ✅ Обрабатываем события независимо
            GLFW.glfwPollEvents();

            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                actualFps = frames;
                frames = 0;
                fpsTimer += 1000;
            }

            // 💡 Чтобы не сжигать CPU — делаем sleep на пару миллисекунд
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        GLFW.glfwTerminate();
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

    private void flushLineBuffer(FloatBuffer buffer) {
        if (buffer.position() == 0) return;

        buffer.flip();

        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(lineVaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVboId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, whiteTexture.getId());

        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);
        GL11.glDrawArrays(GL11.GL_LINES, 0, buffer.limit() / FLOATS_PER_LINE_VERTEX);

        buffer.clear();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    ////---------------------------------------------

    private interface DrawInfo {
        int render(FloatBuffer vertexBuffer, DesktopRenderer renderer);

        int getTextureId();
    }

    private static class SpriteDrawInfo implements DrawInfo {
        private final Sprite sprite;
        private final float a, b, c, d, e, f;
        private final float alpha;

        public SpriteDrawInfo(Sprite sprite, float a, float b, float c, float d, float e, float f, float alpha) {
            this.sprite = sprite;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.alpha = alpha;
        }

        @Override
        public int getTextureId() {
            return sprite.getTextureRegion() != null ? sprite.getTextureRegion().getTexture().getId() : -1;
        }

        @Override
        public int render(FloatBuffer buffer, DesktopRenderer r) {
            TextureRegion region = sprite.getTextureRegion();
            float u0 = 0f, v0 = 1f, u1 = 1f, v1 = 0f;

            float w = sprite.getWidth();
            float h = sprite.getHeight();

            if (region != null) {
                Texture tex = region.getTexture();
                float texW = tex.getWidth();
                float texH = tex.getHeight();
                float rx = region.getX(), ry = region.getY();
                float rw = region.getWidth(), rh = region.getHeight();

                u0 = rx / texW;
                u1 = (rx + rw) / texW;
                v1 = (texH - ry - rh) / texH;
                v0 = (texH - ry) / texH;
            }

            float x0 = c, y0 = f;
            float x1 = a * w + c, y1 = d * w + f;
            float x2 = a * w + b * h + c, y2 = d * w + e * h + f;
            float x3 = b * h + c, y3 = e * h + f;

            float rColor = 1f, g = 1f, bColor = 1f;
            Color color = sprite.getColor();
            rColor = color.getR() / 255f;
            g = color.getG() / 255f;
            bColor = color.getB() / 255f;

            buffer.put(new float[]{
                    x0, y0, u0, v0, rColor, g, bColor, alpha,
                    x1, y1, u1, v0, rColor, g, bColor, alpha,
                    x2, y2, u1, v1, rColor, g, bColor, alpha,
                    x3, y3, u0, v1, rColor, g, bColor, alpha
            });

            return 1;
        }
    }

    private static class BitmapTextDrawInfo implements DrawInfo {
        private final BitmapText text;
        private final float a, b, c, d, e, f;
        private final float alpha;

        public BitmapTextDrawInfo(BitmapText text, float a, float b, float c, float d, float e, float f, float alpha) {
            this.text = text;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.alpha = alpha;
        }

        @Override
        public int getTextureId() {
            return text.getBitmapFont().getTexture().getId();
        }

        @Override
        public int render(FloatBuffer buffer, DesktopRenderer renderer) {
            if (text.isEmpty()) return 0;

            int glyphCount = 0;

            BitmapFont font = text.getBitmapFont();
            Texture texture = font.getTexture();

            float texW = texture.getWidth();
            float texH = texture.getHeight();

            double tf = text.getTextureBleedingFix();
            double vf = text.getVertexBleedingFix();

            float cursorX = 0f;
            float cursorY = 0f;

            float spacing = text.getSpacing();
            float lineSpacing = text.getLineSpacing();
            float scaleX = text.getScaleX();
            float scaleY = text.getScaleY();

            String content = text.getPlainText();

            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);

                if (ch == '\n') {
                    cursorX = 0f;
                    cursorY += font.getZeroCharHeight() + lineSpacing;
                    continue;
                }

                BitmapCharInfo charInfo = font.getCharInfo(ch);
                if (charInfo == null) continue;

                float charW = charInfo.width();
                float charH = charInfo.height();

                float u0 = charInfo.x() / texW;
                float v0 = (texH - charInfo.y()) / texH;
                float u1 = (charInfo.x() + charW) / texW;
                float v1 = (texH - (charInfo.y() + charH)) / texH;

                float x = cursorX;
                float y = cursorY;
                float w = charW;
                float h = charH;

                float px = a * x + b * y + c;
                float py = d * x + e * y + f;
                float px1 = a * (x + w) + b * y + c;
                float py1 = d * (x + w) + e * y + f;
                float px2 = a * (x + w) + b * (y + h) + c;
                float py2 = d * (x + w) + e * (y + h) + f;
                float px3 = a * x + b * (y + h) + c;
                float py3 = d * x + e * (y + h) + f;

                float r = 1f, g = 1f, b = 1f;
                if (text.isMulticolor()) {
                    Color color = text.getColorTextData().getColoredLetter(i).getColor();
                    r = color.getR() / 255f;
                    g = color.getG() / 255f;
                    b = color.getB() / 255f;
                } else {
                    Color color = text.getColor();
                    r = color.getR() / 255f;
                    g = color.getG() / 255f;
                    b = color.getB() / 255f;
                }

                buffer.put(new float[]{
                        px - (float) vf, py - (float) vf, u0 + (float) tf, v0 - (float) tf, r, g, b, alpha,
                        px1 + (float) vf, py1 - (float) vf, u1 - (float) tf, v0 - (float) tf, r, g, b, alpha,
                        px2 + (float) vf, py2 + (float) vf, u1 - (float) tf, v1 + (float) tf, r, g, b, alpha,
                        px3 - (float) vf, py3 + (float) vf, u0 + (float) tf, v1 + (float) tf, r, g, b, alpha
                });

                glyphCount++;

                cursorX += charW + spacing;
            }

            return glyphCount;
        }


    }

    private static class ShapeDrawInfo implements DrawInfo {
        private final RectangleShape shape;
        private final float a, b, c, d, e, f;
        private final float alpha;

        public ShapeDrawInfo(RectangleShape shape, float a, float b, float c, float d, float e, float f, float alpha) {
            this.shape = shape;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.alpha = alpha;
        }

        @Override
        public int getTextureId() {
            return DesktopRenderer.whiteTexture.getId();
        }

        @Override
        public int render(FloatBuffer buffer, DesktopRenderer renderer) {
            float w = shape.getWidth();
            float h = shape.getHeight();

            float x0 = c, y0 = f;
            float x1 = a * w + c, y1 = d * w + f;
            float x2 = a * w + b * h + c, y2 = d * w + e * h + f;
            float x3 = b * h + c, y3 = e * h + f;

            float r = 1f, g = 1f, bColor = 1f;
            if (shape.getColor() != null) {
                Color col = shape.getColor();
                r = col.getR() / 255f;
                g = col.getG() / 255f;
                bColor = col.getB() / 255f;
            }

            buffer.put(new float[]{
                    x0, y0, 0f, 0f, r, g, bColor, alpha,
                    x1, y1, 1f, 0f, r, g, bColor, alpha,
                    x2, y2, 1f, 1f, r, g, bColor, alpha,
                    x3, y3, 0f, 1f, r, g, bColor, alpha
            });

            return 1;
        }
    }


    private static class LineDrawInfo implements DrawInfo {
        private final LineBatch lineBatch;
        private final float a, b, c, d, e, f;
        private final float alpha;

        public LineDrawInfo(LineBatch lineBatch, float a, float b, float c, float d, float e, float f, float alpha) {
            this.lineBatch = lineBatch;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.alpha = alpha;
        }

        @Override
        public int getTextureId() {
            return DesktopRenderer.whiteTexture.getId();
        }

        @Override
        public int render(FloatBuffer buffer, DesktopRenderer renderer) {
            if (lineBatch.getLines().isEmpty()) return 0;

            float r = 1f, g = 1f, b = 1f;
            Color color = lineBatch.getColor();
            if (color != null) {
                r = color.getR() / 255f;
                g = color.getG() / 255f;
                b = color.getB() / 255f;
            }

            for (LineBatch.Line line : lineBatch.getLines()) {
                float x1 = line.vertexA.x;
                float y1 = line.vertexA.y;
                float x2 = line.vertexB.x;
                float y2 = line.vertexB.y;

                float x1t = a * x1 + b * y1 + c;
                float y1t = d * x1 + e * y1 + f;
                float x2t = a * x2 + b * y2 + c;
                float y2t = d * x2 + e * y2 + f;

                buffer.put(new float[]{x1t, y1t, 0f, 0f, r, g, b, alpha});
                buffer.put(new float[]{x2t, y2t, 0f, 0f, r, g, b, alpha});
            }

            renderer.flushLineBuffer(buffer);

            return 1;
        }
    }


}
