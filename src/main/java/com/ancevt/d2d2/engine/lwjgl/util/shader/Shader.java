package com.ancevt.d2d2.engine.lwjgl.util.shader;

import lombok.Getter;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class Shader {
    private final int id;

    @Getter
    private final String source;

    public Shader(String source, int type) {
        this.source = source;
        this.id = GL20.glCreateShader(type);
    }

    public Shader(InputStream inputStream, int type) {
        this(convertInputStreamToString(inputStream), type);
    }

    public void compile() {
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Error compiling shader: " + GL20.glGetShaderInfoLog(id));
        }
    }

    public int getId() {
        return id;
    }

    public void delete() {
        GL20.glDeleteShader(id);
    }

    protected static String readStringFromResources(String resourcePath) {
        try(InputStream inputStream = Shader.class.getClassLoader()
            .getResourceAsStream(resourcePath)) {
            return convertInputStreamToString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertInputStreamToString(InputStream inputStream)  {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringBuilder.toString();
    }



}
