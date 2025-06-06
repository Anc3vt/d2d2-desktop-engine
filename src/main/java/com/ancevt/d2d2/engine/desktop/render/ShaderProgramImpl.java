package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.scene.shader.ShaderProgram;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgramImpl implements ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    final Map<String, Value> values = new HashMap<>();

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
        values.computeIfAbsent(name, k -> new Value(value, 0f, 0f, 0f, Type.FLOAT1));
        glUniform1f(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(String name, int value) {
        values.computeIfAbsent(name, k -> new Value(value, 0f, 0f, 0f, Type.INT1));
        glUniform1i(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(String name, float x, float y) {
        values.computeIfAbsent(name, k -> new Value(x, y, 0f, 0f, Type.FLOAT2));
        glUniform2f(getUniformLocation(name), x, y);
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        values.computeIfAbsent(name, k -> new Value(x, y, z, 0f, Type.FLOAT3));
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    @Override
    public void setUniform(String name, float x, float y, float z, float w) {
        values.computeIfAbsent(name, k -> new Value(x, y, z, w, Type.FLOAT4));
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    enum Type {
        FLOAT1, INT1, FLOAT2, FLOAT3, FLOAT4
    }

    public void uploadAllUniforms() {
        for (Map.Entry<String, Value> entry : values.entrySet()) {
            final String name = entry.getKey();
            final Value val = entry.getValue();
            final int location = getUniformLocation(name);

            switch (val.type) {
                case FLOAT1 -> glUniform1f(location, val.x);
                case INT1 -> glUniform1i(location, (int) val.x);
                case FLOAT2 -> glUniform2f(location, val.x, val.y);
                case FLOAT3 -> glUniform3f(location, val.x, val.y, val.z);
                case FLOAT4 -> glUniform4f(location, val.x, val.y, val.z, val.w);
            }
        }
    }

    @Override
    public void destroy() {
        glDeleteProgram(programId);
    }

    @Override
    public ShaderProgram copy() {
        return new ShaderProgramImpl(getVertexSource(), getFragmentSource());
    }

    @AllArgsConstructor
    static class Value {
        float x;
        float y;
        float z;
        float w;
        Type type;
    }
}
