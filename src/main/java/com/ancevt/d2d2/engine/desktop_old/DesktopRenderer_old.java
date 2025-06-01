/**
 * Copyright (C) 2025 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ancevt.d2d2.engine.desktop_old;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.desktop_old.lwjgl.CanvasHelper_old;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.SceneEvent;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.shape.Shape;
import com.ancevt.d2d2.scene.text.BitmapCharInfo;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import static java.lang.Math.round;
import static org.lwjgl.opengl.GL11.*;


// TODO: rewrite with VBO abd refactor
public class DesktopRenderer_old implements Renderer {

    private final Stage stage;
    private final DesktopEngine_old desktopEngine;
    private DesktopTextureEngine_old textureEngine;
    private int zOrderCounter;

    @Getter
    @Setter
    private int frameRate = 60;

    @Getter
    @Setter
    private int fps = frameRate;

    public DesktopRenderer_old(Stage stage, DesktopEngine_old lwjglStarter) {
        this.stage = stage;
        this.desktopEngine = lwjglStarter;
    }

    @Override
    public void init(long windowId) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glMatrixMode(GL11.GL_MODELVIEW);

    }

    @Override
    public void reshape() {
        desktopEngine.dispatchEvent(CommonEvent.Resize.create(D2D2.stage().getWidth(), D2D2.stage().getHeight()));
        glViewport(0, 0, desktopEngine.getCanvasWidth(), desktopEngine.getCanvasHeight());
        glMatrixMode(GL11.GL_PROJECTION);
        glLoadIdentity();
        GLU.gluOrtho2D(0, D2D2.stage().getWidth(), D2D2.stage().getHeight(), 0);
        glMatrixMode(GL11.GL_MODELVIEW);
        glLoadIdentity();
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
        textureEngine.loadTextures();

        zOrderCounter = 0;

        clear();
        glLoadIdentity();

        renderNode(stage,
                0,
                stage.getX(),
                stage.getY(),
                stage.getScaleX(),
                stage.getScaleY(),
                stage.getAlpha()
        );

        Node cursor = D2D2.getCursor();
        if (cursor != null) {
            renderNode(cursor, 0, 0, 0, 1, 1, 1);
        }

        textureEngine.unloadTexture();

        GLFW.glfwGetCursorPos(CanvasHelper_old.getWindowId(), mouseX, mouseY);
        //Mouse.setXY((int) mouseX[0], (int) mouseY[0]);
    }

    private void dispatchLoopUpdate(Node o) {
        if (!o.isVisible()) return;

        if (o instanceof Group c) {
            for (int i = 0; i < c.getNumChildren(); i++) {
                Node child = c.getChild(i);
                dispatchLoopUpdate(child);
            }
        }

        o.dispatchEvent(SceneEvent.Tick.create());
        o.tick();
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

    private synchronized void renderNode(Node node,
                                         int level,
                                         float toX,
                                         float toY,
                                         float toScaleX,
                                         float toScaleY,
                                         float toAlpha) {

        if (!node.isVisible()) return;

        node.preFrame();
        node.dispatchEvent(SceneEvent.PreFrame.create());

        zOrderCounter++;
        node.setGlobalZOrderIndex(zOrderCounter);

        float scX = node.getScaleX() * toScaleX;
        float scY = node.getScaleY() * toScaleY;
        float r = node.getRotation();

        float x = toScaleX * node.getX();
        float y = toScaleY * node.getY();

        float a = node.getAlpha() * toAlpha;

        if (node.isIntegerPixelAlignmentEnabled()) {
            x = round(x);
            y = round(y);
        }

        glPushMatrix();
        glTranslatef(x, y, 0);
        glRotatef(r, 0, 0, 1);
        glScalef(scX, scY, 1);

        if (node instanceof Colored colored) {
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


        if (node instanceof Group group) {
            for (int i = 0; i < group.getNumChildren(); i++) {
                renderNode(group.getChild(i), level + 1, x + toX, y + toY, toScaleX, toScaleY, a);
            }

        } else if (node instanceof Sprite s) {
            renderSprite(s);
        } else if (node instanceof BitmapText btx) {
            if (btx.isCacheAsSprite()) {
                renderSprite(btx.cachedSprite());
            } else {
                renderBitmapText(btx, a);
            }
        } else if (node instanceof Shape s) {
            renderShape(s, a);
        }

        if (node instanceof Animated fs) {
            fs.processFrame();
        }

        glPopMatrix();

        node.postFrame();
        node.dispatchEvent(SceneEvent.PostFrame.create());
    }

    private void renderShape(Shape s, float alpha) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        ShapeRenderHelper.drawShape(s, alpha);
        glDisable(GL_BLEND);
    }

    private void renderSprite(Sprite sprite) {
        TextureRegion textureRegion = sprite.getTextureRegion();

        if (textureRegion == null) return;
        if (textureRegion.getTexture().isDisposed()) return;

        Texture texture = textureRegion.getTexture();

        boolean bindResult = D2D2.textureManager().getTextureEngine().bind(texture);

        if (!bindResult) {
            return;
        }

        D2D2.textureManager().getTextureEngine().enable(texture);

        int tX = textureRegion.getX();
        int tY = textureRegion.getY();
        int tW = textureRegion.getWidth();
        int tH = textureRegion.getHeight();

        float totalW = texture.getWidth();
        float totalH = texture.getHeight();

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
        D2D2.textureManager().getTextureEngine().disable(texture);
    }

    private static float nextHalf(float v) {
        return (float) (Math.ceil(v * 2) / 2);
    }

    public void setDesktopTextureEngine(DesktopTextureEngine_old textureEngine) {
        this.textureEngine = textureEngine;
    }

    private void renderBitmapText(BitmapText bitmapText, float alpha) {
        if (bitmapText.isEmpty()) return;

        BitmapFont bitmapFont = bitmapText.getBitmapFont();
        Texture texture = bitmapFont.getTexture();

        D2D2.textureManager().getTextureEngine().enable(texture);

        boolean bindResult = D2D2.textureManager().getTextureEngine().bind(texture);

        if (!bindResult) return;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL11.GL_QUADS);

        BitmapTextDrawHelper_old.draw(bitmapText,
                alpha,
                1,
                1,
                DesktopRenderer_old::drawChar,
                DesktopRenderer_old::applyColor
        );

        glEnd();

        glDisable(GL_BLEND);
        D2D2.textureManager().getTextureEngine().disable(texture);
    }

    private static void applyColor(float r, float g, float b, float a) {
        glColor4f(r, g, b, a);
    }

    private static void drawChar(
            Texture texture,
            char c,
            BitmapText.ColorTextData.Letter letter,
            float x,
            float y,
            int textureWidth,
            int textureHeight,
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

        float cx = xOnTexture / textureWidth;
        float cy = -yOnTexture / textureHeight;
        float cw = charWidth / textureWidth;
        float ch = -charHeight / textureHeight;

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

}
