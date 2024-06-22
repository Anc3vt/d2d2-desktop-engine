/**
 * Copyright (C) 2024 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.display.shape.FreeShape;
import com.ancevt.d2d2.display.shape.Shape;
import com.ancevt.d2d2.display.shape.RectangleShape;
import com.ancevt.d2d2.display.shape.Triangle;
import com.ancevt.d2d2.display.shape.Vertex;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

class LwjglShapeRenderer {


    public static void drawShape(Shape shape, float alpha) {
        if (shape instanceof RectangleShape s) {
            drawRectangleShape(s, alpha);
        } else if (shape instanceof FreeShape s) {
            drawFreeShape(s, alpha);
        }
    }

    private static void drawFreeShape(FreeShape s, float alpha) {

        for (Triangle triangle : s.getTriangles()) {

            glBegin(GL11.GL_TRIANGLES);

            glVertex2f(triangle.getX1(), triangle.getY1());
            glVertex2f(triangle.getX2(), triangle.getY2());
            glVertex2f(triangle.getX3(), triangle.getY3());

            glEnd();

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
