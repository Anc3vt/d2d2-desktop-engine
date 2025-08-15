package com.ancevt.d2d2.engine.desktop.node;

import com.ancevt.d2d2.engine.desktop.DesktopTextureManager;
import com.ancevt.d2d2.scene.AbstractNode;
import com.ancevt.d2d2.scene.Bitmap;
import com.ancevt.d2d2.scene.texture.Texture;
import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class BitmapGpu extends AbstractNode implements Bitmap {

    private final int width, height;
    @Getter
    private final ByteBuffer buffer;
    @Getter
    private boolean dirty = true;

    @Getter
    private Texture texture;

    public BitmapGpu(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = BufferUtils.createByteBuffer(width * height * 4);

        texture = DesktopTextureManager.loadTextureInternal(width, height);
    }

    @Override
    public void clear() {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i, (byte) 0);
        }
        dirty = true;
    }


    @Override
    public void setPixel(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;

        int i = (y * width + x) * 4;

        int current =
                ((buffer.get(i + 3) & 0xFF) << 24) |
                        ((buffer.get(i) & 0xFF) << 16) |
                        ((buffer.get(i + 1) & 0xFF) << 8) |
                        ((buffer.get(i + 2) & 0xFF));

        if (current != color) {
            buffer.put(i, (byte) ((color >> 16) & 0xFF)); // R
            buffer.put(i + 1, (byte) ((color >> 8) & 0xFF)); // G
            buffer.put(i + 2, (byte) (color & 0xFF)); // B
            buffer.put(i + 3, (byte) ((color >> 24) & 0xFF)); // A
            dirty = true;
        }
    }

    @Override
    public int getPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0;

        int i = (y * width + x) * 4;
        int r = buffer.get(i) & 0xFF;
        int g = buffer.get(i + 1) & 0xFF;
        int b = buffer.get(i + 2) & 0xFF;
        int a = buffer.get(i + 3) & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public byte[] getPixels(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x + w > width || y + h > height) {
            throw new IllegalArgumentException("Region out of bounds");
        }

        byte[] pixels = new byte[w * h * 4];

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int srcIndex = ((y + row) * width + (x + col)) * 4;
                int dstIndex = (row * w + col) * 4;

                pixels[dstIndex] = buffer.get(srcIndex);     // R
                pixels[dstIndex + 1] = buffer.get(srcIndex + 1); // G
                pixels[dstIndex + 2] = buffer.get(srcIndex + 2); // B
                pixels[dstIndex + 3] = buffer.get(srcIndex + 3); // A
            }
        }

        return pixels;
    }

    @Override
    public void setPixels(int x, int y, int w, int h, byte[] pixels) {
        if (x < 0 || y < 0 || x + w > width || y + h > height) {
            throw new IllegalArgumentException("Region out of bounds");
        }

        if (pixels.length != w * h * 4) {
            throw new IllegalArgumentException("Pixel array size mismatch");
        }

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int dstIndex = ((y + row) * width + (x + col)) * 4;
                int srcIndex = (row * w + col) * 4;

                buffer.put(dstIndex, pixels[srcIndex]);     // R
                buffer.put(dstIndex + 1, pixels[srcIndex + 1]); // G
                buffer.put(dstIndex + 2, pixels[srcIndex + 2]); // B
                buffer.put(dstIndex + 3, pixels[srcIndex + 3]); // A
            }
        }

        dirty = true;
    }


    public void markClean() {
        dirty = false;
    }

    public int getWidthInt() {
        return width;
    }

    public int getHeightInt() {
        return height;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public void dispose() {
        super.dispose();
        GL11.glDeleteTextures(texture.getId());
    }
}
