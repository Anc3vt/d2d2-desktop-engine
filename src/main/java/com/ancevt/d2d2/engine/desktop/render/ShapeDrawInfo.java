package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.shape.RectangleShape;

import java.nio.FloatBuffer;

class ShapeDrawInfo implements DrawInfo {
    private final RectangleShape shape;
    private final float a, b, c, d, e, f;
    private final float alpha;

    public ShapeDrawInfo(RectangleShape shape, float a, float b, float c, float d, float e, float f, float alpha) {
        this.shape = shape;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
        this.alpha = alpha;
    }

    @Override
    public int getTextureId() {
        return GlContextManager.getWhiteTexture().getId();
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
        float w = shape.getWidth();
        float h = shape.getHeight();

        float x0 = c, y0 = f;
        float x1 = a * w + c, y1 = d * w + f;
        float x2 = a * w + b * h + c, y2 = d * w + e * h + f;
        float x3 = b * h + c, y3 = e * h + f;

        float r = 1f, g = 1f, bColor = 1f;
        if (shape.getColor() != null) {
            Color col = shape.getColor();
            r = col.getR() / 255f;
            g = col.getG() / 255f;
            bColor = col.getB() / 255f;
        }

        buffer.put(new float[]{
                x0, y0, 0f, 0f, r, g, bColor, alpha,
                x1, y1, 1f, 0f, r, g, bColor, alpha,
                x2, y2, 1f, 1f, r, g, bColor, alpha,
                x3, y3, 0f, 1f, r, g, bColor, alpha
        });

        return 1;
    }
}
