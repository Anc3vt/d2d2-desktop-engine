package com.ancevt.d2d2.engine.desktop.node;

import com.ancevt.d2d2.scene.AbstractNode;
import com.ancevt.d2d2.scene.BitmapCanvas;
import lombok.Getter;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class BitmapCanvasGpu extends AbstractNode implements BitmapCanvas {

    private final int width, height;
    @Getter
    private final ByteBuffer buffer;
    @Getter
    private boolean dirty = true;

    public BitmapCanvasGpu(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = BufferUtils.createByteBuffer(width * height * 4);
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


}
