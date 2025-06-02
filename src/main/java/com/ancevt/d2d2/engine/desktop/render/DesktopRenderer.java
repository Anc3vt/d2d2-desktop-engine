package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.engine.desktop.CanvasControl;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.StageEvent;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.shape.FreeShape;
import com.ancevt.d2d2.scene.shape.LineBatch;
import com.ancevt.d2d2.scene.shape.RectangleShape;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.time.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class DesktopRenderer implements Renderer {

    private final DesktopEngine engine;

    @Getter
    @Setter
    private boolean running = true;

    public static final int BATCH_SIZE = 80000;

    private static final int FLOATS_PER_VERTEX = 8; // x, y, u, v, r, g, b, a
    private static final int VERTICES_PER_SPRITE = 4;

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(BATCH_SIZE * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);

    @Getter
    private int actualFps;

    @Getter
    @Setter
    private int frameRate = 60;
    private GlContextManager glContextManager;

    @Override
    public void init(long windowId) {
        glContextManager = new GlContextManager(BATCH_SIZE, vertexBuffer);
        glContextManager.init();
        glContextManager.setProjection(engine.getCanvasWidth(), engine.getCanvasHeight());
    }

    @Override
    public void reshape() {
        glContextManager.setProjection(engine.getCanvasWidth(), engine.getCanvasHeight());

        var s = engine.getStage();
        s.dispatchEvent(CommonEvent.Resize.create(engine.getCanvasWidth(), engine.getCanvasHeight()));
        s.setSize(engine.getCanvasWidth(), engine.getCanvasHeight());
    }


    @Override
    public void renderFrame() {
        List<DrawInfo> drawQueue = new ArrayList<>();

        Stage stage = engine.getStage();

        collectNodes(stage, 1f, 0f, 0f, 0f, 1f, 0f, 1f, drawQueue);

        glContextManager.prepareRenderFrame(stage.getBackgroundColor());

        int spritesInBatch = 0;
        int currentTextureId = -1;

        for (DrawInfo info : drawQueue) {
            int texId = info.getTextureId();

            if (currentTextureId != texId || spritesInBatch >= BATCH_SIZE) {
                if (spritesInBatch > 0) glContextManager.flushBatch(spritesInBatch);
                currentTextureId = texId;
                glContextManager.setTextureFilter(texId, GL11.GL_NEAREST);
                spritesInBatch = 0;
                vertexBuffer.clear();
            }

            int addedSprites = info.render(vertexBuffer, this);
            spritesInBatch += addedSprites;
        }

        if (spritesInBatch > 0) glContextManager.flushBatch(spritesInBatch);


        glContextManager.postRenderFrame();
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
                //throw new IllegalStateException("cache as sprite not supported. "+ btx.toString());
                drawQueue.add(new SpriteDrawInfo(btx.cachedSprite(), na, nb, nc, nd, ne, nf, newAlpha));
            } else {
                drawQueue.add(new BitmapTextDrawInfo(btx, na, nb, nc, nd, ne, nf, newAlpha));
            }
        } else if (node instanceof RectangleShape rect) {
            drawQueue.add(new ShapeDrawInfo(rect, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof FreeShape freeShape) {
            drawQueue.add(new FreeShapeDrawInfo(freeShape, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof LineBatch lineBatch) {
            drawQueue.add(new LineBatchDrawInfo(lineBatch, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof BitmapCanvas bitmapCanvas) {
            drawQueue.add(new BitmapCanvasDrawInfo(bitmapCanvas, na, nb, nc, nd, ne, nf, newAlpha));
        }


        if (node instanceof Group group) {
            for (Node child : group.children().toList()) {
                collectNodes(child, na, nb, nc, nd, ne, nf, newAlpha, drawQueue);
            }
        }
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


}
