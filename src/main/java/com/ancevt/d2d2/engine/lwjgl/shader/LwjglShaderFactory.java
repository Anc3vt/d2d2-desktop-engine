package com.ancevt.d2d2.engine.lwjgl.shader;

import com.ancevt.d2d2.display.shader.Shader;
import com.ancevt.d2d2.display.shader.ShaderProgram;
import com.ancevt.d2d2.engine.ShaderFactory;
import com.ancevt.d2d2.exception.NotImplementedException;

public class LwjglShaderFactory implements ShaderFactory {
    @Override
    public Shader createFragmentShader(String source) {
        throw new NotImplementedException("Not yet implemented");
        //return new LwjglFragmentShader(source);
    }

    @Override
    public Shader createVertexShader(String source) {
        throw new NotImplementedException("Not yet implemented");
        //return new LwjglVertexShader(source);
    }

    @Override
    public ShaderProgram createShaderProgram() {
        throw new NotImplementedException("Not yet implemented");
        //return new LwjglShaderProgram();
    }
}
