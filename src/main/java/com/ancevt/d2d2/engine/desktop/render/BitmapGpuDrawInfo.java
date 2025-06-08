package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.engine.desktop.DesktopTextureManager;
import com.ancevt.d2d2.engine.desktop.node.BitmapGpu;
import com.ancevt.d2d2.scene.texture.Texture;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class BitmapGpuDrawInfo implements DrawInfo {

    private final BitmapGpu canvas;
    private final float a, b, c, d, e, f;
    private final float alpha;

    private Texture texture;

    public BitmapGpuDrawInfo(BitmapGpu canvas,
                             float a, float b, float c,
                             float d, float e, float f,
                             float alpha) {
        this.canvas = canvas;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
        this.alpha = alpha;

        this.texture = DesktopTextureManager.loadTextureInternal(
                canvas.getWidthInt(), canvas.getHeightInt());
        DesktopTextureManager.bindTexture(texture);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                canvas.getWidthInt(), canvas.getHeightInt(),
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, canvas.getBuffer());
    }

    @Override
    public ShaderProgramImpl getCustomShader() {
        return (ShaderProgramImpl) canvas.getShaderProgram();
    }

    private void updateTexture() {
        if (canvas.isDirty()) {
            DesktopTextureManager.bindTexture(texture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                    canvas.getWidthInt(), canvas.getHeightInt(),
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    canvas.getBuffer());

            canvas.markClean();
        }
    }

    @Override
    public int getTextureId() {
        updateTexture();
        return texture.getId();
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
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
                x3, y3, 0f, 1f, r, g, bCol, alpha,
        });

        return 1;
    }
}
