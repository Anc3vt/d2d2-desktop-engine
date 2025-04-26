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
package com.ancevt.d2d2.engine.lwjgl.shader;

import com.ancevt.d2d2.scene.shader.Shader;
import lombok.Getter;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class LwjglShader implements Shader {
    private final int id;

    @Getter
    private final String source;

    public LwjglShader(String source, int type) {
        this.source = source;
        this.id = GL20.glCreateShader(type);
    }

    public LwjglShader(InputStream inputStream, int type) {
        this(convertInputStreamToString(inputStream), type);
    }

    @Override
    public void compile() {
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Error compiling shader: " + GL20.glGetShaderInfoLog(id));
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void delete() {
        GL20.glDeleteShader(id);
    }

    protected static String readStringFromResources(String resourcePath) {
        try(InputStream inputStream = LwjglShader.class.getClassLoader()
            .getResourceAsStream(resourcePath)) {
            return convertInputStreamToString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String convertInputStreamToString(InputStream inputStream)  {
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
