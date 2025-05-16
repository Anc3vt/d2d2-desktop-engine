/**
 * Copyright (C) 2025 the original author or authors.
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
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.ITextureEngine;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;
import com.ancevt.d2d2.scene.texture.TextureRegionCombinerCell;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class LwjglTextureEngine implements ITextureEngine {

    private final TextureLoadQueue loadQueue;
    private final Queue<Texture> unloadQueue;
    private final TextureMapping mapping;
    private int textureIdCounter;

    public LwjglTextureEngine() {
        mapping = new TextureMapping();
        loadQueue = new TextureLoadQueue();
        unloadQueue = new LinkedList<>();
    }

    @Override
    public boolean bind(Texture texture) {
        if (mapping.ids().containsKey(texture.getId())) {
            glBindTexture(GL_TEXTURE_2D, mapping.ids().get(texture.getId()));
            return true;
        }
        return false;
    }

    @Override
    public void enable(Texture texture) {
        GL30.glEnable(GL_TEXTURE_2D);
    }

    @Override
    public void disable(Texture texture) {
        GL30.glDisable(GL_TEXTURE_2D);
    }

    @Override
    public Texture createTexture(int width, int height, TextureRegionCombinerCell[] cells) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = (Graphics2D) image.getGraphics();

        for (final TextureRegionCombinerCell cell : cells) {
            // Установка альфа-композита
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cell.getAlpha());
            g.setComposite(alphaComposite);

            // Установка цвета
            if (cell.getColor() != null) {
                Color cellColor = cell.getColor();
                java.awt.Color awtColor = new java.awt.Color(
                    cellColor.getR(),
                    cellColor.getG(),
                    cellColor.getB()
                );
                g.setColor(awtColor);
            }

            // Установка аффинного преобразования для вращения
            AffineTransform affineTransform = g.getTransform();
            affineTransform.rotate(Math.toRadians(cell.getRotation()), cell.getX(), cell.getY());
            g.setTransform(affineTransform);

            // Отрисовка ячейки
            drawCell(g, cell);

            // Восстановление начального аффинного преобразования
            affineTransform.rotate(Math.toRadians(-cell.getRotation()), cell.getX(), cell.getY());
            g.setTransform(affineTransform);
        }

        final Texture texture = createTextureFromBufferedImage(image);
        mapping.images().put(texture.getId(), image);
        D2D2.textureManager().addTextureRegion("_texture_" + texture.getId(), texture.createTextureRegion());
        return texture;
    }



    private void drawCell(Graphics2D g, final TextureRegionCombinerCell cell) {
        int x = cell.getX();
        int y = cell.getY();
        float repeatX = cell.getRepeatX();
        float repeatY = cell.getRepeatY();
        float scaleX = cell.getScaleX();
        float scaleY = cell.getScaleY();

        BufferedImage fullImageRegion = textureRegionToImage(cell.getTextureRegion());

        // Определяем цветовые масштабы и смещения, если цвет задан
        float[] scales = null;
        float[] offsets = null;
        if (cell.getColor() != null) {
            Color cellColor = cell.getColor();
            scales = new float[] {
                cellColor.getR() / 255.0f,
                cellColor.getG() / 255.0f,
                cellColor.getB() / 255.0f,
                1.0f
            };
            offsets = new float[4];
            RescaleOp rop = new RescaleOp(scales, offsets, null);
            fullImageRegion = rop.filter(fullImageRegion, null);
        }

        BufferedImage imageRegion;

        float texWidth = cell.getTextureRegion().getWidth();
        float texHeight = cell.getTextureRegion().getHeight();

        int originWidth = fullImageRegion.getWidth(null);
        int originHeight = fullImageRegion.getHeight(null);

        for (int rY = 0; rY < repeatY; rY += 1.0f) {
            for (int rX = 0; rX < repeatX; rX += 1.0f) {
                int w = (int) (originWidth * scaleX);
                int h = (int) (originHeight * scaleY);

                imageRegion = fullImageRegion;

                if (repeatX - rX < 1.0f || repeatY - rY < 1.0f) {
                    float valX = 1.0f;
                    float valY = 1.0f;

                    if (repeatX - rX < 1.0f) {
                        valX = repeatX - rX;
                        w *= valX;
                    }

                    if (repeatY - rY < 1.0f) {
                        valY = repeatY - rY;
                        h *= valY;
                    }

                    imageRegion = textureRegionToImage(
                        cell.getTextureRegion().createSubregion(
                            0,
                            0,
                            (int) (texWidth * valX),
                            (int) (texHeight * valY)
                        )
                    );

                    // Применение цвета к подизображению, если он задан
                    if (cell.getColor() != null) {
                        RescaleOp rop = new RescaleOp(scales, offsets, null);
                        imageRegion = rop.filter(imageRegion, null);
                    }
                }

                g.drawImage(imageRegion,
                    (int) (x + texWidth * rX * scaleX),
                    (int) (y + texHeight * rY * scaleY),
                    w,
                    h,
                    null
                );
            }
        }
    }



    public Texture createTexture(InputStream pngInputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(pngInputStream);
            Texture texture = createTextureFromBufferedImage(bufferedImage);
            mapping.images().put(texture.getId(), bufferedImage);
            return texture;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Texture createTexture(String assetPath) {
        try {
            InputStream pngInputStream = Assets.getAsset(assetPath).getInputStream();
            return createTextureFromBufferedImage(ImageIO.read(pngInputStream));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Texture createTextureFromBufferedImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4); //4 for RGBA, 3 for RGB

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                byteBuffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
                byteBuffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
                byteBuffer.put((byte) (pixel & 0xFF));             // Blue component
                byteBuffer.put((byte) ((pixel >> 24) & 0xFF));     // Alpha component. Only for RGBA
            }
        }

        byteBuffer.flip();

        Texture texture = createTextureFromByteBuffer(byteBuffer, width, height);
        mapping.images().put(texture.getId(), image);
        D2D2.textureManager().addTexture(texture);
        return texture;
    }

    private Texture createTextureFromByteBuffer(ByteBuffer byteBuffer, int width, int height) {
        Texture texture = new Texture(++textureIdCounter, width, height);
        loadQueue.putLoad(new TextureLoadQueue.LoadTask(texture, width, height, byteBuffer));
        return texture;
    }

    public void loadTextures() {
        while (loadQueue.hasTasks()) {
            TextureLoadQueue.LoadTask loadTask = loadQueue.poll();

            Texture texture = loadTask.getTexture();
            ByteBuffer byteBuffer = loadTask.getByteBuffer();
            int width = loadTask.getWidth();
            int height = loadTask.getHeight();

            int openGlTextureId = glGenTextures();

            mapping.ids().put(texture.getId(), openGlTextureId);

            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, openGlTextureId);

            // Tell OpenGL how to unpack the RGBA bytes. Each component pngInputStream 1 byte size
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            // Upload the texture data
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
            // Generate Mip Map
            glGenerateMipmap(GL_TEXTURE_2D);
        }
    }

    @Override
    public void unloadTexture(Texture texture) {
        mapping.images().remove(texture.getId());
        // TODO: repair creating new textures after unloading
        if (texture.isDisposed()) {
            return;
            //throw new IllegalStateException("Texture atlas is already unloaded " + textureAtlas);
        }

        unloadQueue.add(texture);
    }

    public void unloadTexture() {
        while (!unloadQueue.isEmpty()) {
            Texture texture = unloadQueue.poll();
            glDeleteTextures(mapping.ids().get(texture.getId()));
            mapping.ids().remove(texture.getId());
            mapping.images().remove(texture.getId());
        }
    }

    @Override
    public Texture bitmapTextToTexture(BitmapText bitmapText) {
        int width = (int) bitmapText.getWidth();
        int height = (int) bitmapText.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        BitmapTextDrawHelper.draw(
                bitmapText,
            bitmapText.getAlpha(),
            bitmapText.getScaleX(),
            bitmapText.getScaleY(),
            (atlas, c, letter, drawX, drawY, textureAtlasWidth, textureAtlasHeight, charInfo, scX, scY, textureBleedingFix, vertexBleedingFix) -> {

                if (c != '\n') {
                    int charX = charInfo.x();
                    int charY = charInfo.y();

                    int offsetX = 0;
                    int offsetY = 0;

                    if (charX < 0) {
                        offsetX = -charX;
                        charX = 0;
                    }
                    if (charY < 0) {
                        offsetY = -charY;
                        charY = 0;
                    }

                    BufferedImage charImage = textureRegionToImage(
                        atlas, charX, charY, charInfo.width(), charInfo.height()
                    );

                    charImage = copyImage(charImage);

                    Color letterColor = letter == null ? bitmapText.getColor() : letter.getColor();

                    applyColorFilter(
                        charImage,
                        letterColor.getR(),
                        letterColor.getG(),
                        letterColor.getB()
                    );

                    g.drawImage(charImage, (int) drawX + offsetX, (int) drawY - charInfo.height() + offsetY, null);
                }

            },
            null
        );


        final Texture texture = createTextureFromBufferedImage(image);
        D2D2.textureManager().addTextureRegion("_texture_text_" + texture.getId(), texture.createTextureRegion());
        return texture;
    }

    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    private static void applyColorFilter(BufferedImage image, int redPercent, int greenPercent, int bluePercent) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                pixel = (alpha << 24) | (redPercent * red / 255 << 16) | (greenPercent * green / 255 << 8) | (bluePercent * blue / 255);

                image.setRGB(x, y, pixel);
            }
        }
    }

    private BufferedImage textureRegionToImage(Texture texture, int x, int y, int width, int height) {
        BufferedImage bufferedImage = mapping.images().get(texture.getId());
        return bufferedImage.getSubimage(x, y, width, height);
    }

    private BufferedImage textureRegionToImage(TextureRegion textureRegion) {
        return textureRegionToImage(
            textureRegion.getTexture(),
            textureRegion.getX(),
            textureRegion.getY(),
            textureRegion.getWidth(),
            textureRegion.getHeight()
        );
    }
}
