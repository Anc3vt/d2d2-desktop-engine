package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.asset.Asset;
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureEngine;
import com.ancevt.d2d2.scene.texture.TextureRegionCombinerCell;
import com.ancevt.d2d2.util.InputStreamFork;
import lombok.Getter;
import lombok.SneakyThrows;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class DesktopTextureEngine implements TextureEngine {

    @Getter
    private final Map<Integer, BufferedImage> bufferedImageMap = new HashMap<>();

    @Override
    public boolean bind(Texture texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
        return true;
    }

    @Override
    public void enable(Texture texture) {

    }

    @Override
    public void disable(Texture texture) {

    }

    public static void bindTexture(Texture texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
    }

    public static Texture createTexture(int width, int height) {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Настройки фильтрации и обёртки
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        // Резервируем память без данных (null)
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null
        );

        return new Texture(textureId, width, height);
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
        return AwtBitmapTextDrawHelper.bitmapTextToTexture(bitmapText);
    }

    @SneakyThrows
    private Texture loadTextureFromInputStream(InputStream pngInputStream) {

        InputStreamFork fork = InputStreamFork.fork(pngInputStream);
        InputStream inputStream = fork.left();

        BufferedImage bufferedImage = ImageIO.read(fork.right());

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
            bufferedImageMap.put(textureId, bufferedImage);
            return new Texture(textureId, w.get(0), h.get(0));

        } catch (IOException e) {
            throw new RuntimeException("Could not load texture resource", e);
        }
    }

    public Texture loadTextureFromResource(String resourcePath) {
        Asset asset = Assets.getAsset(resourcePath);
        return loadTextureFromInputStream(asset.getInputStream());
    }


}
