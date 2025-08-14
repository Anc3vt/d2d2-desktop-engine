package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.desktop.CanvasControl;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.engine.desktop.DesktopTextureManager;
import com.ancevt.d2d2.engine.desktop.node.BitmapGpu;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.StageEvent;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.shader.ShaderProgram;
import com.ancevt.d2d2.scene.shape.FreeShape;
import com.ancevt.d2d2.scene.shape.LineBatch;
import com.ancevt.d2d2.scene.shape.RectangleShape;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.time.Timer;
import com.ancevt.d2d2.util.Tickable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.glUseProgram;

@RequiredArgsConstructor
public class DesktopRenderer implements Renderer {

    private final DesktopEngine engine;

    @Getter
    @Setter
    private boolean running = true;

    public static final int BATCH_SIZE = 300000;

    private static final int FLOATS_PER_VERTEX = 8; // x, y, u, v, r, g, b, a
    private static final int VERTICES_PER_SPRITE = 4;

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(BATCH_SIZE * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);

    @Getter
    private int actualFps;

    @Getter
    @Setter
    private int frameRate = 60;

    @Getter
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

    public void renderGroupToCurrentFrameBuffer(Group group, int width, int height) {
        List<DrawInfo> drawQueue = new ArrayList<>();
        zOrderCounter = -1;
        collectNodes(group, 1f, 0f, 0f, 0f, 1f, 0f, 1f, drawQueue);

        glContextManager.setProjection(width, height);
        glContextManager.prepareRenderFrame(Color.NO_COLOR);

        int currentTex = -1;
        int batch = 0;
        vertexBuffer.clear();

        for (DrawInfo info : drawQueue) {
            int texId = info.getTextureId();

            if (batch >= BATCH_SIZE || texId != currentTex) {
                if (batch > 0) glContextManager.flushBatch(batch);
                batch = 0;
                currentTex = texId;
                glContextManager.setTextureFilter(texId, GL11.GL_NEAREST);
                vertexBuffer.clear();
            }

            batch += info.render(vertexBuffer, this);
        }

        if (batch > 0) glContextManager.flushBatch(batch);

        glContextManager.setProjection(engine.getCanvasWidth(), engine.getCanvasHeight()); // Восстанови
    }


    @Override
    public void renderFrame() {
        List<DrawInfo> drawQueue = new ArrayList<>();
        Stage stage = engine.getStage();

        zOrderCounter = -1;
        collectNodes(stage, 1f, 0f, 0f, 0f, 1f, 0f, 1f, drawQueue);

        glContextManager.prepareRenderFrame(stage.getBackgroundColor());

        int currentTextureId = -1;
        ShaderProgram currentShader = null;
        int batchSize = 0;

        vertexBuffer.clear();

        for (DrawInfo info : drawQueue) {
            int textureId = info.getTextureId();
            ShaderProgram shader = info.getCustomShader();

            boolean flushNeeded =
                    (textureId != currentTextureId) ||
                            (shader != currentShader) ||
                            (batchSize >= BATCH_SIZE);

            if (flushNeeded) {
                if (batchSize > 0) {
                    glContextManager.flushBatch(batchSize);
                }

                currentTextureId = textureId;
                currentShader = shader;

                int shaderId = (shader == null) ? glContextManager.shaderProgram : shader.getId();
                glUseProgram(shaderId);

                if (shader instanceof ShaderProgramImpl impl) {
                    impl.uploadUniforms();
                }

                glContextManager.setTextureFilter(textureId, GL11.GL_NEAREST);
                vertexBuffer.clear();
                batchSize = 0;
            }

            batchSize += info.render(vertexBuffer, this);
        }

        if (batchSize > 0) {
            glContextManager.flushBatch(batchSize);
        }

        glContextManager.postRenderFrame();
    }

    private static int zOrderCounter;

    private static void collectNodes(Node node, float a, float b, float c, float d, float e, float f, float alpha, List<DrawInfo> drawQueue) {

        zOrderCounter++;
        node.setGlobalZOrderIndex(zOrderCounter);

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
            drawQueue.add(new RectangleShapeDrawInfo(rect, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof FreeShape freeShape) {
            drawQueue.add(new FreeShapeDrawInfo(freeShape, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof LineBatch lineBatch) {
            drawQueue.add(new LineBatchDrawInfo(lineBatch, na, nb, nc, nd, ne, nf, newAlpha));
        } else if (node instanceof BitmapGpu canvasGPU) {
            drawQueue.add(new BitmapGpuDrawInfo(canvasGPU, na, nb, nc, nd, ne, nf, newAlpha));
        }

        if (node instanceof Tickable it) it.onTick();

        if (node instanceof Group group) {
            for (Node child : group.children().toList()) {
                collectNodes(child, na, nb, nc, nd, ne, nf, newAlpha, drawQueue);
            }
        }

        if (node instanceof Animated animated) animated.processFrame();
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

            while (accumulator >= tickInterval) {
                Timer.processTimers();
                stage.dispatchEvent(StageEvent.Tick.create());
                accumulator -= tickInterval;
            }

            if (now - lastRenderTime >= frameInterval) {
                stage.dispatchEvent(StageEvent.PreFrame.create());
                renderFrame();
                stage.dispatchEvent(StageEvent.PostFrame.create());
                GLFW.glfwSwapBuffers(windowId);
                lastRenderTime = now;
                frames++;
            }

            GLFW.glfwPollEvents();

            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                actualFps = frames;
                frames = 0;
                fpsTimer += 1000;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ((DesktopTextureManager) D2D2.getTextureManager()).shutdown();
        GLFW.glfwTerminate();
    }


}
