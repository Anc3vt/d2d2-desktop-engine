package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.desktop.AwtBitmapTextDrawHelper;
import com.ancevt.d2d2.scene.BitmapCanvas;
import com.ancevt.d2d2.scene.texture.Texture;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.FloatBuffer;

public class BitmapCanvasDrawInfo implements DrawInfo {
    private final BitmapCanvas canvas;
    private final float a, b, c, d, e, f;
    private final float alpha;

    private Texture texture;
    private boolean dirty = true;

    public BitmapCanvasDrawInfo(BitmapCanvas canvas, float a, float b, float c, float d, float e, float f, float alpha) {
        this.canvas = canvas;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
        this.alpha = alpha;
    }

    private void updateTextureIfNeeded() {
        if (!dirty && texture != null) return;

        BufferedImage image = new BufferedImage((int) canvas.getWidth(), (int) canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < canvas.getWidth(); x++) {
            for (int y = 0; y < canvas.getHeight(); y++) {
                image.setRGB(x, y, canvas.getPixel(x, y));
            }
        }

        try (InputStream is = AwtBitmapTextDrawHelper.bufferedImageToPngInputStream(image)) {
            if (texture != null) D2D2.textureManager().unloadTexture(texture);
            texture = D2D2.textureManager().loadTexture(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    @Override
    public int getTextureId() {
        updateTextureIfNeeded();
        return texture != null ? texture.getId() : GlContextManager.getWhiteTexture().getId();
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
        updateTextureIfNeeded();

        float w = canvas.getWidth();
        float h = canvas.getHeight();

        float x0 = c, y0 = f;
        float x1 = a * w + c, y1 = d * w + f;
        float x2 = a * w + b * h + c, y2 = d * w + e * h + f;
        float x3 = b * h + c, y3 = e * h + f;

        float r = 1f, g = 1f, bCol = 1f;

        buffer.put(new float[]{
                x0, y0, 0f, 0f, r, g, bCol, alpha,
                x1, y1, 1f, 0f, r, g, bCol, alpha,
                x2, y2, 1f, 1f, r, g, bCol, alpha,
                x3, y3, 0f, 1f, r, g, bCol, alpha
        });

        return 1;
    }
}

