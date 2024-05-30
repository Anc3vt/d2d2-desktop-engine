package com.ancevt.d2d2.engine.lwjgl.util.shader;

import org.lwjgl.opengl.GL20;

public class VertexShader extends Shader {
    public VertexShader(String source) {
        super(source, GL20.GL_VERTEX_SHADER);
    }

    public static VertexShader createFromResources(String resourcesPath) {
        return new VertexShader(readStringFromResources(resourcesPath));
    }
}
