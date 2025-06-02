package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.engine.desktop.CanvasControl;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.StageEvent;
import com.ancevt.d2d2.scene.*;
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
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class DesktopRenderer implements Renderer {

    private final DesktopEngine engine;

    @Getter
    @Setter
    private boolean running = true;

    public static final int BATCH_SIZE = 20000;

    private static final int FLOATS_PER_VERTEX = 8; // x, y, u, v, r, g, b, a
    private static final int VERTICES_PER_SPRITE = 4;
    private static final int INDICES_PER_SPRITE = 6;

    private static Texture whiteTexture;

    private static final int FLOATS_PER_LINE_VERTEX = 8;
    private static final int MAX_LINES = 8192;

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(BATCH_SIZE * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
    private final float[] projectionMatrix = new float[16];

    @Getter
    private int actualFps;

    @Getter
    @Setter
    private int frameRate = 60;
    private GlContextManager glContextManager;

    @Override
    public void init(long windowId) {
        glContextManager = new GlContextManager(BATCH_SIZE, vertexBuffer);
        glContextManager.init(ShaderSources.VERTEX_SHADER, ShaderSources.FRAGMENT_SHADER);

        whiteTexture = createWhiteTexture();
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

        GL20.glUseProgram(glContextManager.shaderProgram);
        GL20.glUniformMatrix4fv(glContextManager.uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(glContextManager.vaoId);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glContextManager.vboId);
        vertexBuffer.clear();
        int spritesInBatch = 0;
        int currentTextureId = -1;

        for (DrawInfo info : drawQueue) {
            int texId = info.getTextureId();

            if (currentTextureId != texId || spritesInBatch >= BATCH_SIZE) {
                if (spritesInBatch > 0) glContextManager.flushBatch(spritesInBatch);
                currentTextureId = texId;
                setNearestFilter(texId);
                spritesInBatch = 0;
                vertexBuffer.clear();
            }

            int addedSprites = info.render(vertexBuffer, this);
            spritesInBatch += addedSprites;
        }

        if (spritesInBatch > 0) glContextManager.flushBatch(spritesInBatch);

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
        }

        if (node instanceof Group group) {
            for (Node child : group.children().toList()) {
                collectNodes(child, na, nb, nc, nd, ne, nf, newAlpha, drawQueue);
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

}
