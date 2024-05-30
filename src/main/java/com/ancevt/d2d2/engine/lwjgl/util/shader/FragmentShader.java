package com.ancevt.d2d2.engine.lwjgl.util.shader;

import org.lwjgl.opengl.GL20;

public class FragmentShader extends Shader {
    public FragmentShader(String source) {
        super(source, GL20.GL_FRAGMENT_SHADER);
    }

    public static FragmentShader createFromResources(String resourcesPath) {
        return new FragmentShader(readStringFromResources(resourcesPath));
    }
}
