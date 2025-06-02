package com.ancevt.d2d2.engine.desktop.render;

public class ShaderSources {

    public static final String VERTEX_SHADER = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec4 aColor;
            uniform mat4 uProjection;
            out vec2 vTexCoord;
            out vec4 vColor;
            void main() {
                vTexCoord = aTexCoord;
                vColor = aColor;
                gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
            }
            """;
    public static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec2 vTexCoord;
            in vec4 vColor;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            void main() {
                vec4 texColor = texture(uTexture, vTexCoord);
                FragColor = texColor * vColor;
            }
            """;
}
