package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.shader.ShaderProgram;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgramImpl implements ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    @Getter
    private final String vertexSource;
    @Getter
    private final String fragmentSource;

    public ShaderProgramImpl(String vertexSource, String fragmentSource) {
        this.vertexSource = vertexSource;
        this.fragmentSource = fragmentSource;
        int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
        int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);

        programId = glCreateProgram();

        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private int compileShader(String source, int type) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        int status = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (status == 0) {
            String typeName = (type == GL_VERTEX_SHADER) ? "VERTEX" : "FRAGMENT";
            throw new RuntimeException(typeName + " SHADER COMPILE ERROR:\n" + glGetShaderInfoLog(shaderId));
        }

        return shaderId;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    @Override
    public int getId() {
        return programId;
    }

    @Override
    public int getUniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, n -> {
            int location = glGetUniformLocation(programId, n);
            if (location == -1) System.err.println("⚠️ Uniform not found: " + n);
            return location;
        });
    }

    // === Uniform Setters ===
    @Override
    public void setUniform(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(String name, float x, float y) {
        glUniform2f(getUniformLocation(name), x, y);
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    @Override
    public void setUniform(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    @Override
    public void destroy() {
        glDeleteProgram(programId);
    }

    @Override
    public ShaderProgram copy() {
        return new ShaderProgramImpl(getVertexSource(), getFragmentSource());
    }
}
