package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.Sprite;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;

import java.nio.FloatBuffer;

class SpriteDrawInfo implements DrawInfo {
    private final Sprite sprite;
    private final float a, b, c, d, e, f;
    private final float alpha;

    public SpriteDrawInfo(Sprite sprite, float a, float b, float c, float d, float e, float f, float alpha) {
        this.sprite = sprite;
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
        return sprite.getTextureRegion() != null ? sprite.getTextureRegion().getTexture().getId() : -1;
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer r) {
        TextureRegion region = sprite.getTextureRegion();
        float u0 = 0f, v0 = 1f, u1 = 1f, v1 = 0f;

        float w = sprite.getWidth();
        float h = sprite.getHeight();

        if (region != null) {
            Texture tex = region.getTexture();
            float texW = tex.getWidth();
            float texH = tex.getHeight();
            float rx = region.getX(), ry = region.getY();
            float rw = region.getWidth(), rh = region.getHeight();

            u0 = rx / texW;
            u1 = (rx + rw) / texW;
            v1 = (texH - ry - rh) / texH;
            v0 = (texH - ry) / texH;
        }

        float x0 = c, y0 = f;
        float x1 = a * w + c, y1 = d * w + f;
        float x2 = a * w + b * h + c, y2 = d * w + e * h + f;
        float x3 = b * h + c, y3 = e * h + f;

        float rColor = 1f, g = 1f, bColor = 1f;
        Color color = sprite.getColor();
        rColor = color.getR() / 255f;
        g = color.getG() / 255f;
        bColor = color.getB() / 255f;

        buffer.put(new float[]{
                x0, y0, u0, v0, rColor, g, bColor, alpha,
                x1, y1, u1, v0, rColor, g, bColor, alpha,
                x2, y2, u1, v1, rColor, g, bColor, alpha,
                x3, y3, u0, v1, rColor, g, bColor, alpha
        });

        return 1;
    }
}
