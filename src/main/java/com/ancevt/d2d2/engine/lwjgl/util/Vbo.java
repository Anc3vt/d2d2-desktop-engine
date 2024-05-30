package com.ancevt.d2d2.engine.lwjgl.util;

import lombok.Getter;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

public class Vbo {
    private final int id;
    @Getter
    private final float[] source;

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
