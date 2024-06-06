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

import lombok.Getter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Vao {

    @Getter
    private final int id;
    private int count;
    private final List<Vbo> vboList = new ArrayList<>();

    public Vao() {
        this.id = glGenVertexArrays();
    }

    public Vbo addVbo(Vbo vbo,
                       int attributeIndex,
                       int size,
                       int type,
                       boolean normalized,
                       int stride,
                       int pointer) {
        bind();
        vbo.bind();
        glVertexAttribPointer(attributeIndex, size, type, normalized, stride, pointer);
        glEnableVertexAttribArray(attributeIndex);
        vbo.unbind();
        unbind();
        return vbo;
    }

    public Vbo addVbo(Vbo vbo, int attributeIndex) {
        return addVbo(vbo, attributeIndex, 3, GL_FLOAT, false, 0, 0);
    }

    public void disableAttribute(int attributeIndex) {
        bind();
        GL30.glDisableVertexAttribArray(attributeIndex);
        unbind();
    }

    public void removeVbo(Vbo vbo) {
        vbo.delete();
        vboList.remove(vbo);
    }

    public int size() {
        return vboList.size();
    }

    public Stream<Vbo> vboStream() {
        return vboList.stream();
    }

    public void delete() {
        glDeleteVertexArrays(id);
        for (Vbo vbo : vboList) {
            vbo.delete();
        }
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    public void draw(int mode, int count) {
        GL30.glBindVertexArray(id);
        GL11.glDrawArrays(mode, 0, count);
        GL30.glBindVertexArray(0);
    }
}
