/**
 * Copyright (C) 2024 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.display.Color;
import com.ancevt.d2d2.display.Colored;
import com.ancevt.d2d2.display.Container;
import com.ancevt.d2d2.display.DisplayObject;
import com.ancevt.d2d2.display.Playable;
import com.ancevt.d2d2.display.Renderer;
import com.ancevt.d2d2.display.Sprite;
import com.ancevt.d2d2.display.Stage;
import com.ancevt.d2d2.display.shape.Shape;
import com.ancevt.d2d2.display.text.BitmapCharInfo;
import com.ancevt.d2d2.display.text.BitmapFont;
import com.ancevt.d2d2.display.text.BitmapText;
import com.ancevt.d2d2.display.texture.TextureClip;
import com.ancevt.d2d2.display.texture.TextureAtlas;
import com.ancevt.d2d2.engine.lwjgl.util.Vao;
import com.ancevt.d2d2.engine.lwjgl.util.Vbo;
import com.ancevt.d2d2.engine.lwjgl.util.shader.ShaderProgram;
import com.ancevt.d2d2.engine.lwjgl.util.shader.VertexShader;
import com.ancevt.d2d2.event.Event;
import com.ancevt.d2d2.event.EventPool;
import com.ancevt.d2d2.input.Mouse;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import static java.lang.Math.round;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTexCoord2d;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2d;
import static org.lwjgl.opengl.GL11.glViewport;


// TODO: rewrite with VBO abd refactor
public class LwjglRenderer implements Renderer {

    private final Stage stage;
    private final LwjglEngine lwjglEngine;
    boolean smoothMode = false;
    private LwjglTextureEngine textureEngine;
    private int zOrderCounter;

    @Getter
    @Setter
    private int frameRate = 60;

    @Getter
    @Setter
    private int fps = frameRate;

    private Vao vao;

    private ShaderProgram shaderProgram;

    public LwjglRenderer(Stage stage, LwjglEngine lwjglStarter) {
        this.stage = stage;
        this.lwjglEngine = lwjglStarter;
    }

    @Override
    public void init(long windowId) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glMatrixMode(GL11.GL_MODELVIEW);

        VertexShader vertexShader = VertexShader.createFromResources("d2d2shader/shader.vert");
        vertexShader.compile();

        shaderProgram = new ShaderProgram();
        shaderProgram.attachShader(vertexShader);
        shaderProgram.link();

        float[] vertices = {
            0f, 0f, 0.0f,
            1f, 0f, 0.0f,
            1f, 1f, 0.0f,
            0f, 1f, 0.0f,
        };

        vao = new Vao();
        vao.addVbo(new Vbo(vertices), 0);
    }

    @Override
    public void reshape() {
        glViewport(0, 0, lwjglEngine.getCanvasWidth(), lwjglEngine.getCanvasHeight());
        glMatrixMode(GL11.GL_PROJECTION);
        glLoadIdentity();
        GLU.gluOrtho2D(0, D2D2.stage().getWidth(), D2D2.stage().getHeight(), 0);
        glMatrixMode(GL11.GL_MODELVIEW);
        glLoadIdentity();
        lwjglEngine.dispatchEvent(EventPool.simpleEventSingleton(Event.RESIZE, lwjglEngine));
    }


    private long lastTime = System.currentTimeMillis();
    private double delta = 0;

    private int frames;
    private long lastFpsTime = System.currentTimeMillis();

    @Override
    public void renderFrame() {
        double nsPerUpdate = 1000.0 / this.frameRate;
        long now = System.currentTimeMillis();
        delta += (now - lastTime) / nsPerUpdate;

        // Выполнить обновление игровой логики, даже если кадры пропущены
        while (delta >= 1) {
            dispatchLoopUpdate(stage);
            delta--;
        }

        if (D2D2.getCursor() != null) {
            dispatchLoopUpdate(D2D2.getCursor());
        }

        render();
        frames++;

        if (now - lastFpsTime > 1000) {
            fps = Math.min(frames, frameRate);
            frames = 0;
            lastFpsTime = System.currentTimeMillis();
        }

        lastTime = now;
    }

    // Метод для рендеринга кадра
    private void render() {
        textureEngine.loadTextureAtlases();

        zOrderCounter = 0;

        clear();
        glLoadIdentity();

        renderDisplayObject(stage,
            0,
            stage.getX(),
            stage.getY(),
            stage.getScaleX(),
            stage.getScaleY(),
            stage.getAlpha()
        );

        DisplayObject cursor = D2D2.getCursor();
        if (cursor != null) {
            renderDisplayObject(cursor, 0, 0, 0, 1, 1, 1);
        }

        textureEngine.unloadTextureAtlases();

        GLFW.glfwGetCursorPos(lwjglEngine.displayManager().getWindowId(), mouseX, mouseY);
        //Mouse.setXY((int) mouseX[0], (int) mouseY[0]);
    }

    private void dispatchLoopUpdate(DisplayObject o) {
        if (!o.isVisible()) return;

        if (o instanceof Container c) {
            for (int i = 0; i < c.getNumChildren(); i++) {
                DisplayObject child = c.getChild(i);
                dispatchLoopUpdate(child);
            }
        }

        o.dispatchEvent(EventPool.simpleEventSingleton(Event.LOOP_UPDATE, o));
        o.onLoopUpdate();
    }

    private final double[] mouseX = new double[1];
    private final double[] mouseY = new double[1];

    private void clear() {
        Color backgroundColor = stage.getBackgroundColor();
        float backgroundColorRed = backgroundColor.getR() / 255.0f;
        float backgroundColorGreen = backgroundColor.getG() / 255.0f;
        float backgroundColorBlue = backgroundColor.getB() / 255.0f;
        glClearColor(backgroundColorRed, backgroundColorGreen, backgroundColorBlue, 1.0f);
        glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    private synchronized void renderDisplayObject(DisplayObject displayObject,
                                                  int level,
                                                  float toX,
                                                  float toY,
                                                  float toScaleX,
                                                  float toScaleY,
                                                  float toAlpha) {

        if (!displayObject.isVisible()) return;

        displayObject.onEnterFrame();
        displayObject.dispatchEvent(EventPool.simpleEventSingleton(Event.ENTER_FRAME, displayObject));

        zOrderCounter++;
        displayObject.setAbsoluteZOrderIndex(zOrderCounter);

        float scX = displayObject.getScaleX() * toScaleX;
        float scY = displayObject.getScaleY() * toScaleY;
        float r = displayObject.getRotation();

        float x = toScaleX * displayObject.getX();
        float y = toScaleY * displayObject.getY();

        float a = displayObject.getAlpha() * toAlpha;

        if (displayObject.isIntegerPixelAlignmentEnabled()) {
            x = round(x);
            y = round(y);
        }

        glPushMatrix();
        glTranslatef(x, y, 0);
        glRotatef(r, 0, 0, 1);
        glScalef(scX, scY, 1);

        if (displayObject instanceof Colored colored) {
            Color color = colored.getColor();

            if (color != null) {
                glColor4f(
                    color.getR() / 255f,
                    color.getG() / 255f,
                    color.getB() / 255f,
                    a
                );
            }
        }

        if (displayObject instanceof Container container) {
            for (int i = 0; i < container.getNumChildren(); i++) {
                renderDisplayObject(container.getChild(i), level + 1, x + toX, y + toY, toScaleX, toScaleY, a);
            }

        } else if (displayObject instanceof Sprite s) {
            renderSprite(s);
        } else if (displayObject instanceof BitmapText btx) {
            if (btx.isCacheAsSprite()) {
                renderSprite(btx.cachedSprite());
            } else {
                renderBitmapText(btx, a);
            }
        } else if (displayObject instanceof Shape s) {
            renderShape(s, a);
        }

        if (displayObject instanceof Playable fs) {
            fs.processFrame();
        }

        glPopMatrix();

        displayObject.onExitFrame();
        displayObject.dispatchEvent(EventPool.simpleEventSingleton(Event.EXIT_FRAME, displayObject));
    }

    private void renderShape(Shape s, float alpha) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        LwjglShapeRenderer.drawShape(s, alpha);
        glDisable(GL_BLEND);
    }

    private void renderSprite(Sprite sprite) {

        TextureClip textureClip = sprite.getTextureClip();

        if (textureClip == null) return;
        if (textureClip.getTextureAtlas().isDisposed()) return;

        TextureAtlas textureAtlas = textureClip.getTextureAtlas();


        boolean bindResult = D2D2.textureManager().getTextureEngine().bind(textureAtlas);

        if (!bindResult) {
            return;
        }

        D2D2.textureManager().getTextureEngine().enable(textureAtlas);

        int tX = textureClip.getX();
        int tY = textureClip.getY();
        int tW = textureClip.getWidth();
        int tH = textureClip.getHeight();

        float totalW = textureAtlas.getWidth();
        float totalH = textureAtlas.getHeight();

        float x = tX / totalW;
        float y = tY / totalH;
        float w = tW / totalW;
        float h = tH / totalH;

        float repeatX = sprite.getRepeatX();
        float repeatY = sprite.getRepeatY();

        double vertexBleedingFix = sprite.getVertexBleedingFix();
        double textureBleedingFix = sprite.getTextureBleedingFix();

        for (int rY = 0; rY < repeatY; rY++) {
            for (float rX = 0; rX < repeatX; rX++) {
                float px = round(rX * tW * (float) 1);
                float py = round(rY * tH * (float) 1);

                double textureTop = y + textureBleedingFix;
                double textureBottom = (h + y) - textureBleedingFix;
                double textureLeft = x + textureBleedingFix;
                double textureRight = (w + x) - textureBleedingFix;

                double vertexTop = py - vertexBleedingFix;
                double vertexBottom = py + tH + vertexBleedingFix;
                double vertexLeft = px - vertexBleedingFix;
                double vertexRight = px + tW + vertexBleedingFix;

                if (repeatX - rX < 1.0) {
                    double val = repeatX - rX;
                    vertexRight = px + tW * val + vertexBleedingFix;
                    textureRight *= val;
                }

                if (repeatY - rY < 1.0) {
                    double val = repeatY - rY;
                    vertexBottom = py + tH * val + vertexBleedingFix;
                    textureBottom = (h * val + y) - textureBleedingFix;
                }
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glBegin(GL11.GL_QUADS);

                // L
                glTexCoord2d(textureLeft, textureBottom);
                glVertex2d(vertexLeft, vertexBottom);

                // _|
                glTexCoord2d(textureRight, textureBottom);
                glVertex2d(vertexRight, vertexBottom);

                // ^|
                glTexCoord2d(textureRight, textureTop);
                glVertex2d(vertexRight, vertexTop);

                // Г
                glTexCoord2d(textureLeft, textureTop);
                glVertex2d(vertexLeft, vertexTop);

                glEnd();
                glDisable(GL_BLEND);
            }
        }

        glDisable(GL_BLEND);
        D2D2.textureManager().getTextureEngine().disable(textureAtlas);
    }

    private void renderBitmapText(BitmapText bitmapText, float alpha) {
        if (bitmapText.isEmpty()) return;

        BitmapFont bitmapFont = bitmapText.getBitmapFont();
        TextureAtlas textureAtlas = bitmapFont.getTextureAtlas();

        D2D2.textureManager().getTextureEngine().enable(textureAtlas);

        boolean bindResult = D2D2.textureManager().getTextureEngine().bind(textureAtlas);

        if (!bindResult) return;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL11.GL_QUADS);

        BitmapTextDrawHelper.draw(bitmapText,
            alpha,
            1,
            1,
            LwjglRenderer::drawChar,
            LwjglRenderer::applyColor
        );

        glEnd();

        glDisable(GL_BLEND);
        D2D2.textureManager().getTextureEngine().disable(textureAtlas);
    }

    private static void applyColor(float r, float g, float b, float a) {
        glColor4f(r, g, b, a);
    }

    private static float nextHalf(float v) {
        return (float) (Math.ceil(v * 2) / 2);
    }

    private static void drawChar(
        TextureAtlas textureAtlas,
        char c,
        BitmapText.ColorTextData.Letter letter,
        float x,
        float y,
        int textureAtlasWidth,
        int textureAtlasHeight,
        BitmapCharInfo charInfo,
        float scX,
        float scY,
        double textureBleedingFix,
        double vertexBleedingFix) {

        //scX = nextHalf(scX) ;
        scY = nextHalf(scY);

        float charWidth = charInfo.width();
        float charHeight = charInfo.height();

        float xOnTexture = charInfo.x();
        float yOnTexture = charInfo.y() + charHeight;

        float cx = xOnTexture / textureAtlasWidth;
        float cy = -yOnTexture / textureAtlasHeight;
        float cw = charWidth / textureAtlasWidth;
        float ch = -charHeight / textureAtlasHeight;

        double tf = textureBleedingFix;
        double vf = vertexBleedingFix;

        glTexCoord2d(cx - tf, -cy + tf);
        glVertex2d(x - vf, y + vf);

        glTexCoord2d(cx + cw + tf, -cy + tf);
        glVertex2d(charWidth * scX + x + vf, y + vf);

        glTexCoord2d(cx + cw + tf, -cy + ch - tf);
        glVertex2d(charWidth * scX + x + vf, charHeight * -scY + y - vf);

        glTexCoord2d(cx - tf, -cy + ch - tf);
        glVertex2d(x - vf, charHeight * -scY + y - vf);
    }

    public void setLWJGLTextureEngine(LwjglTextureEngine textureEngine) {
        this.textureEngine = textureEngine;
    }

}
