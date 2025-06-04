package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.shape.LineBatch;
import com.ancevt.d2d2.scene.shape.LineBatch.Line;
import com.ancevt.d2d2.scene.shape.Vertex;

import java.nio.FloatBuffer;

class LineBatchDrawInfo implements DrawInfo {

    private final LineBatch batch;
    private final float a, b, c, d, e, f;
    private final float alpha;

    public LineBatchDrawInfo(LineBatch batch, float a, float b, float c, float d, float e, float f, float alpha) {
        this.batch = batch;
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
        return (ShaderProgramImpl) batch.getShaderProgram();
    }

    @Override
    public int getTextureId() {
        return GlContextManager.getWhiteTexture().getId();
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
        Color color = batch.getColor();
        float r = color.getR() / 255f;
        float g = color.getG() / 255f;
        float bColor = color.getB() / 255f;

        int quadCount = 0;
        float lw = batch.getLineWidth() / 2f;

        for (Line line : batch.getLines()) {
            Vertex aV = line.getVertexA();
            Vertex bV = line.getVertexB();

            float x0 = aV.x, y0 = aV.y;
            float x1 = bV.x, y1 = bV.y;

            float dx = x1 - x0;
            float dy = y1 - y0;

            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length == 0f) continue;

            // Нормаль влево
            float nx = -dy / length;
            float ny = dx / length;

            // Смещённые точки
            float ax = x0 + nx * lw;
            float ay = y0 + ny * lw;
            float bx = x1 + nx * lw;
            float by = y1 + ny * lw;
            float cx = x1 - nx * lw;
            float cy = y1 - ny * lw;
            float dx2 = x0 - nx * lw;
            float dy2 = y0 - ny * lw;

            // Трансформация
            float pax = this.a * ax + this.b * ay + this.c;
            float pay = this.d * ax + this.e * ay + this.f;
            float pbx = this.a * bx + this.b * by + this.c;
            float pby = this.d * bx + this.e * by + this.f;
            float pcx = this.a * cx + this.b * cy + this.c;
            float pcy = this.d * cx + this.e * cy + this.f;
            float pdx = this.a * dx2 + this.b * dy2 + this.c;
            float pdy = this.d * dx2 + this.e * dy2 + this.f;

            // Подаём прямоугольник (2 треугольника)
            buffer.put(new float[]{
                    pax, pay, 0f, 0f, r, g, bColor, alpha,
                    pbx, pby, 0f, 0f, r, g, bColor, alpha,
                    pcx, pcy, 0f, 0f, r, g, bColor, alpha,
                    pdx, pdy, 0f, 0f, r, g, bColor, alpha
            });

            quadCount++;
        }

        return quadCount;
    }
}
