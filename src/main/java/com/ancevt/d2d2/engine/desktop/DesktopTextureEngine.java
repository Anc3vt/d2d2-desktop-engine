package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.asset.Asset;
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureEngine;
import com.ancevt.d2d2.scene.texture.TextureRegionCombinerCell;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class DesktopTextureEngine implements TextureEngine {
    @Override
    public boolean bind(Texture texture) {
        return false;
    }

    @Override
    public void enable(Texture texture) {

    }

    @Override
    public void disable(Texture texture) {

    }

    @Override
    public Texture createTexture(InputStream pngInputStream) {
        return loadTextureFromInputStream(pngInputStream);
    }

    @Override
    public Texture createTexture(String assetPath) {
        return loadTextureFromResource(assetPath);
    }

    @Override
    public Texture createTexture(int width, int height, TextureRegionCombinerCell[] cells) {
        return null;
    }

    @Override
    public void unloadTexture(Texture texture) {

    }

    @Override
    public Texture bitmapTextToTexture(BitmapText bitmapText) {
        return null;
    }

    private static Texture loadTextureFromInputStream(InputStream inputStream) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            byte[] imageBytes = inputStream.readAllBytes();

            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
            }

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    w.get(0),
                    h.get(0),
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    image
            );
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            STBImage.stbi_image_free(image);

            return new Texture(textureId, w.get(0), h.get(0));

        } catch (IOException e) {
            throw new RuntimeException("Could not load texture resource", e);
        }
    }

    public static Texture loadTextureFromResource(String resourcePath) {
        Asset asset = Assets.getAsset(resourcePath);
        return loadTextureFromInputStream(asset.getInputStream());
    }


    public static Texture loadTexture(String filePath) {
        // Используем MemoryStack для временных буферов под размеры
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Загружаем изображение в ByteBuffer (RGBA)
            STBImage.stbi_set_flip_vertically_on_load(true); // переворачиваем по вертикали для правильной ориентации
            ByteBuffer image = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (image == null) {
                throw new RuntimeException("Could not load texture: " + STBImage.stbi_failure_reason());
            }

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            // Параметры фильтрации и повторения (Clamp to Edge, чтобы избежать артефактов на границах прозрачности)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);

            // Загрузка пикселей в GPU память
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w.get(0), h.get(0), 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
            // Создаём mipmaps (необязательно, но на будущее для масштабирования)
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            // Освобождаем память, занятую изображением в RAM
            STBImage.stbi_image_free(image);

            // Возвращаем объект текстуры с параметрами
            return new Texture(textureId, w.get(0), h.get(0));
        }
    }
}
