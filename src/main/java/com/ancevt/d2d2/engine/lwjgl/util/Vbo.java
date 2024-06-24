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
package com.ancevt.d2d2.engine.lwjgl.util;

import com.ancevt.d2d2.display.Color;
import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

public class Vbo {
    private final int id;
    @Getter
    private final float[] source;
    public Color color;

    public Vbo(float[] source, int usage) {
        this.source = source;
        this.id = glGenBuffers();
        bind();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(source.length);
        buffer.put(source).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        unbind();
    }

    public Vbo(float[] data) {
        this(data, GL_STATIC_DRAW);
    }

    public void update(float[] data) {
        bind();
        FloatBuffer vertexData = MemoryUtil.memAllocFloat(data.length);
        vertexData.put(data).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexData);
        unbind();
    }

    public void bind() {
        glBindBuffer(GL_ARRAY_BUFFER, id);
    }

    public void unbind() {
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public int getId() {
        return id;
    }

    public void delete() {
        glDeleteBuffers(id);
    }
}
