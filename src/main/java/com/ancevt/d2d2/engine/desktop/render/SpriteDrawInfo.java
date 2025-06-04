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
    public ShaderProgramImpl getCustomShader() {
        return (ShaderProgramImpl) sprite.getShaderProgram();
    }

    @Override
    public int getTextureId() {
        return sprite.getTextureRegion() != null ? sprite.getTextureRegion().getTexture().getId() : -1;
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer r) {
        TextureRegion region = sprite.getTextureRegion();
        if (region == null) return 0;

        Texture texture = region.getTexture();
        if (texture == null) return 0;

        float texW = texture.getWidth();
        float texH = texture.getHeight();

        float rx = region.getX();
        float ry = region.getY();
        float rw = region.getWidth();
        float rh = region.getHeight();

        float u0 = rx / texW;
        float u1 = (rx + rw) / texW;
        float v1 = (texH - ry - rh) / texH;
        float v0 = (texH - ry) / texH;

        Color color = sprite.getColor();
        float rColor = color.getR() / 255f;
        float g = color.getG() / 255f;
        float bColor = color.getB() / 255f;

        float repeatX = Math.max(0.01f, sprite.getRepeatX());
        float repeatY = Math.max(0.01f, sprite.getRepeatY());

        float tileW = rw;
        float tileH = rh;

        int triangleCount = 0;

        for (float ix = 0; ix < repeatX; ix += 1f) {
            for (float iy = 0; iy < repeatY; iy += 1f) {
                float dx = ix * tileW;
                float dy = iy * tileH;

                // Доля плитки, если не до конца
                float xRatio = Math.min(1f, repeatX - ix);
                float yRatio = Math.min(1f, repeatY - iy);

                float localW = tileW * xRatio;
                float localH = tileH * yRatio;

                // UV для частичной плитки
                float u0x = u0;
                float u1x = u0 + (u1 - u0) * xRatio;
                float v0y = v0;
                float v1y = v0 - (v0 - v1) * yRatio;

                float x0 = a * dx + b * dy + c;
                float y0 = d * dx + e * dy + f;
                float x1 = a * (dx + localW) + b * dy + c;
                float y1 = d * (dx + localW) + e * dy + f;
                float x2 = a * (dx + localW) + b * (dy + localH) + c;
                float y2 = d * (dx + localW) + e * (dy + localH) + f;
                float x3 = a * dx + b * (dy + localH) + c;
                float y3 = d * dx + e * (dy + localH) + f;

                buffer.put(new float[]{
                        x0, y0, u0x, v0y, rColor, g, bColor, alpha,
                        x1, y1, u1x, v0y, rColor, g, bColor, alpha,
                        x2, y2, u1x, v1y, rColor, g, bColor, alpha,
                        x3, y3, u0x, v1y, rColor, g, bColor, alpha
                });

                triangleCount++;
            }
        }

        return triangleCount;
    }


}
