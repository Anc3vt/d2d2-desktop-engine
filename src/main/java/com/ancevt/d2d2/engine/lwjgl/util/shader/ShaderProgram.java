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
package com.ancevt.d2d2.engine.lwjgl.util.shader;

import org.lwjgl.opengl.GL20;

public class ShaderProgram {
    private final int id;

    public ShaderProgram() {
        this.id = GL20.glCreateProgram();
    }

    public void attachShader(Shader shader) {
        GL20.glAttachShader(id, shader.getId());
    }

    public void detachShader(Shader shader) {
        GL20.glDetachShader(id, shader.getId());
    }

    public void link() {
        GL20.glLinkProgram(id);

        if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Error linking shader program: " + GL20.glGetProgramInfoLog(id));
        }
    }

    public void use() {
        GL20.glUseProgram(id);
    }

    public void stop() {
        GL20.glUseProgram(0);
    }

    public void delete() {
        GL20.glDeleteProgram(id);
    }

    public int getAttribLocation(String name) {
        return GL20.glGetAttribLocation(id, name);
    }

    public void setAttrib1f(String name, float value) {
        GL20.glVertexAttrib1f(getAttribLocation(name), value);
    }

    public void setAttrib2f(String name, float value1, float value2) {
        GL20.glVertexAttrib2f(getAttribLocation(name), value1, value2);
    }

    public void setAttrib3f(String name, float value1, float value2, float value3) {
        GL20.glVertexAttrib3f(getAttribLocation(name), value1, value2, value3);
    }

    public void setAttrib4f(String name, float value1, float value2, float value3, float value4) {
        GL20.glVertexAttrib4f(getAttribLocation(name), value1, value2, value3, value4);
    }

    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(id, name);
    }

    public void setUniform1f(String name, float value) {
        GL20.glUniform1f(getUniformLocation(name), value);
    }

    public void setUniform2f(String name, float value1, float value2) {
        GL20.glUniform2f(getUniformLocation(name), value1, value2);
    }

    public void setUniform3f(String name, float value1, float value2, float value3) {
        GL20.glUniform3f(getUniformLocation(name), value1, value2, value3);
    }

    public void setUniform4f(String name, float value1, float value2, float value3, float value4) {
        GL20.glUniform4f(getUniformLocation(name), value1, value2, value3, value4);
    }


    // Добавьте больше методов для установки других типов uniform переменных по необходимости.
}
