package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.shape.RectangleShape;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;

import java.nio.FloatBuffer;

class RectangleShapeDrawInfo implements DrawInfo {
    private final RectangleShape shape;
    private final float a, b, c, d, e, f;
    private final float alpha;

    public RectangleShapeDrawInfo(RectangleShape shape, float a, float b, float c, float d, float e, float f, float alpha) {
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

    private float[] transformUV(float x, float y, RectangleShape shape, TextureRegion region) {
        float width = shape.getWidth();
        float height = shape.getHeight();

        // Нормализуем координаты
        float normX = x / width;
        float normY = y / height;

        // Применяем масштаб
        float scaledX = (normX - 0.5f) * shape.getTextureScaleX();
        float scaledY = (normY - 0.5f) * shape.getTextureScaleY();

        // Вращаем вокруг центра
        float angle = shape.getTextureRotation();
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);

        float rotatedX = scaledX * cos - scaledY * sin;
        float rotatedY = scaledX * sin + scaledY * cos;

        // Смещаем обратно от центра и применяем repeat
        float u = (rotatedX + 0.5f) * shape.getTextureURepeat();
        float v = (rotatedY + 0.5f) * shape.getTextureVRepeat();

        return new float[]{u, v};
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

        float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;

        float[] uv0 = new float[]{0f, 0f};
        float[] uv1 = new float[]{w, 0f};
        float[] uv2 = new float[]{w, h};
        float[] uv3 = new float[]{0f, h};

        TextureRegion region = shape.getTextureRegion();
        if (region != null) {
            uv0 = transformUV(0f, 0f, shape, region);
            uv1 = transformUV(w, 0f, shape, region);
            uv2 = transformUV(w, h, shape, region);
            uv3 = transformUV(0f, h, shape, region);
        }

        float uBase = 0f, vBase = 0f;
        if (region != null) {
            Texture tex = region.getTexture();
            float texW = tex.getWidth();
            float texH = tex.getHeight();

            uBase = region.getX() / texW;
            vBase = (texH - region.getY()) / texH;
        }

        buffer.put(new float[]{
                x0, y0, uBase + uv0[0], vBase - uv0[1], r, g, bColor, alpha,
                x1, y1, uBase + uv1[0], vBase - uv1[1], r, g, bColor, alpha,
                x2, y2, uBase + uv2[0], vBase - uv2[1], r, g, bColor, alpha,
                x3, y3, uBase + uv3[0], vBase - uv3[1], r, g, bColor, alpha
        });

        return 1;
    }

}
