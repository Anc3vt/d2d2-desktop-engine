package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.shape.FreeShape;
import com.ancevt.d2d2.scene.shape.Triangle;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;

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
        Texture texture = shape.getTextureRegion() != null ? shape.getTextureRegion().getTexture() : null;
        return texture != null ? texture.getId() : GlContextManager.getWhiteTexture().getId();
    }

    private float[] applyUVTransform(float x, float y, FreeShape shape, TextureRegion region) {
        float repeatU = shape.getTextureURepeat();
        float repeatV = shape.getTextureVRepeat();

        float scaleX = shape.getTextureScaleX();
        float scaleY = shape.getTextureScaleY();
        float angle = shape.getTextureRotation();

        // 1. Центруем по фигуре (локально)
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (var v : shape.getVertices()) {
            minX = Math.min(minX, v.getX());
            minY = Math.min(minY, v.getY());
            maxX = Math.max(maxX, v.getX());
            maxY = Math.max(maxY, v.getY());
        }

        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;

        // 2. Центрируем координаты
        float cx = (x - centerX) * scaleX;
        float cy = (y - centerY) * scaleY;

        // 3. Вращаем
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);

        float tx = cx * cos - cy * sin;
        float ty = cx * sin + cy * cos;

        // 4. Смещаем обратно и применяем repeat
        float u = (tx + 0.5f * (maxX - minX)) / (maxX - minX) * repeatU;
        float v = (ty + 0.5f * (maxY - minY)) / (maxY - minY) * repeatV;

        return new float[]{u, v};
    }


    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
        Color color = shape.getColor();
        float red   = color.getR() / 255f;
        float green = color.getG() / 255f;
        float blue  = color.getB() / 255f;

        int triangleCount = 0;

        TextureRegion region = shape.getTextureRegion();
        Texture texture = region != null ? region.getTexture() : null;

        float texW = texture != null ? texture.getWidth() : 1f;
        float texH = texture != null ? texture.getHeight() : 1f;

        float u0 = 0f, v0 = 0f;

        if (region != null) {
            u0 = region.getX() / texW;
            v0 = (texH - region.getY()) / texH;
        }

        for (Triangle t : shape.getTriangles()) {
            float[] transformed = transformTriangle(t);

            float x1 = t.getX1(), y1 = t.getY1();
            float x2 = t.getX2(), y2 = t.getY2();
            float x3 = t.getX3(), y3 = t.getY3();

            float[] uv1 = applyUVTransform(x1, y1, shape, region);
            float[] uv2 = applyUVTransform(x2, y2, shape, region);
            float[] uv3 = applyUVTransform(x3, y3, shape, region);

            buffer.put(new float[]{
                    transformed[0], transformed[1], u0 + uv1[0], v0 - uv1[1], red, green, blue, alpha,
                    transformed[2], transformed[3], u0 + uv2[0], v0 - uv2[1], red, green, blue, alpha,
                    transformed[4], transformed[5], u0 + uv3[0], v0 - uv3[1], red, green, blue, alpha,
                    transformed[0], transformed[1], u0 + uv1[0], v0 - uv1[1], red, green, blue, alpha
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
