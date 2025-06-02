package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.shape.FreeShape;
import com.ancevt.d2d2.scene.shape.Triangle;
import com.ancevt.d2d2.scene.texture.Texture;

import java.nio.FloatBuffer;

class FreeShapeDrawInfo implements DrawInfo {

    private final FreeShape shape;
    private final float a, b, c, d, e, f;
    private final float alpha;

    public FreeShapeDrawInfo(FreeShape shape, float a, float b, float c, float d, float e, float f, float alpha) {
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
        Texture texture = shape.getTexture();
        return texture != null ? texture.getId() : GlContextManager.getWhiteTexture().getId();
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
        Color color = shape.getColor();
        float r = color.getR() / 255f;
        float g = color.getG() / 255f;
        float bColor = color.getB() / 255f;

        int triangleCount = 0;

        for (Triangle t : shape.getTriangles()) {
            float[] transformed = transformTriangle(t);
            buffer.put(new float[]{
                    transformed[0], transformed[1], 0f, 0f, r, g, bColor, alpha,
                    transformed[2], transformed[3], 0f, 0f, r, g, bColor, alpha,
                    transformed[4], transformed[5], 0f, 0f, r, g, bColor, alpha,
                    transformed[0], transformed[1], 0f, 0f, r, g, bColor, alpha // degenerate quad
            });
            triangleCount++;
        }

        return triangleCount;
    }

    private float[] transformTriangle(Triangle t) {
        float x1 = t.getX1(), y1 = t.getY1();
        float x2 = t.getX2(), y2 = t.getY2();
        float x3 = t.getX3(), y3 = t.getY3();

        float tx1 = a * x1 + b * y1 + c;
        float ty1 = d * x1 + e * y1 + f;
        float tx2 = a * x2 + b * y2 + c;
        float ty2 = d * x2 + e * y2 + f;
        float tx3 = a * x3 + b * y3 + c;
        float ty3 = d * x3 + e * y3 + f;

        return new float[]{tx1, ty1, tx2, ty2, tx3, ty3};
    }
}
