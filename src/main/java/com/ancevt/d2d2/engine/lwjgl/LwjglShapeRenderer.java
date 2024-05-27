package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.display.shape.IShape;
import com.ancevt.d2d2.display.shape.RectangleShape;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

class LwjglShapeRenderer {


    public static void drawShape(IShape shape, float alpha) {
        if (shape instanceof RectangleShape s) {
            drawRectangleShape(s, alpha);
        }
    }

    private static void drawRectangleShape(RectangleShape s, float alpha) {
        float l = 0;
        float r = s.getWidth();
        float b = s.getHeight();
        float t = 0;



        glBegin(GL11.GL_QUADS);
        glVertex2f(l, b);
        glVertex2f(r, b);
        glVertex2f(r, t);
        glVertex2f(l, t);
        glEnd();

        glEnd();
    }
}
