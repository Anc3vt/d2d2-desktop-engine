/**
 * Copyright (C) 2025 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.engine.desktop.awt.BitmapTextAwtHelper;
import com.ancevt.d2d2.engine.desktop.lwjgl.WindowHelper;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.core.EventDispatcherImpl;
import com.ancevt.d2d2.log.Logger;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Root;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.TrueTypeFontBuilder;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;

// TODO: rewrite with VBO and refactor
public class DesktopEngine extends EventDispatcherImpl implements Engine {

    private DesktopRenderer renderer;
    private final int initialWidth;
    private final int initialHeight;
    private final String initialTitle;
    private Root root;
    private int frameRate = 60;
    private boolean alwaysOnTop;

    private final DesktopDisplayManager displayManager = new DesktopDisplayManager();

    @Getter
    @Setter
    private int timerCheckFrameFrequency = 1;

    public DesktopEngine(int initialWidth, int initialHeight, String initialTitle) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.initialTitle = initialTitle;
        setCanvasSize(initialWidth, initialHeight);
        D2D2.textureManager().setTextureEngine(new DesktopTextureEngine());
    }

    @Override
    public SoundManager soundManager() {
        return DesktopSoundManager.getInstance();
    }

    @Override
    public void setCanvasSize(int width, int height) {
        WindowHelper.setCanvasSize(width, height);
    }

    @Override
    public int getCanvasWidth() {
        return WindowHelper.getCanvasWidth();
    }

    @Override
    public int getCanvasHeight() {
        return WindowHelper.getCanvasHeight();
    }

    @Override
    public Logger logger() {
        return DesktopLogger.getInstance();
    }

    @Override
    public DisplayManager displayManager() {
        return displayManager;
    }

    @Override
    public void setAlwaysOnTop(boolean b) {
        this.alwaysOnTop = b;
        glfwWindowHint(GLFW_FLOATING, alwaysOnTop ? GLFW_TRUE : GLFW_FALSE);
    }

    @Override
    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    @Override
    public void stop() {
        if (!WindowHelper.isRunning()) return;
        WindowHelper.setRunning(false);
    }

    @Override
    public void create() {
        root = new Root();
        renderer = new DesktopRenderer(root, this);
        renderer.setDesktopTextureEngine((DesktopTextureEngine) D2D2.textureManager().getTextureEngine());
        WindowHelper.init(this, initialWidth, initialHeight, initialTitle);
        displayManager.setVisible(true);
        root.setSize(initialWidth, initialHeight);
        renderer.reshape();
    }

    @Override
    public void setSmoothMode(boolean value) {
        renderer.smoothMode = value;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        if (value) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        }
    }

    @Override
    public boolean isSmoothMode() {
        return renderer.smoothMode;
    }

    @Override
    public void start() {
        WindowHelper.setRunning(true);
        root.dispatchEvent(CommonEvent.Start.create());
        WindowHelper.startRenderLoop(this);
        root.dispatchEvent(CommonEvent.Stop.create());
    }

    @Override
    public Root root() {
        return root;
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public void setCursorXY(int x, int y) {
        GLFW.glfwSetCursorPos(WindowHelper.getWindowId(), x, y);
    }

    @Override
    public void putToClipboard(String string) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(
                        new StringSelection(string),
                        null
                );
    }

    @Override
    public String getStringFromClipboard() {
        try {
            return Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor).toString();
        } catch (UnsupportedFlavorException e) {
            return "";
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
        renderer.setFrameRate(frameRate);
    }

    @Override
    public int getFrameRate() {
        return frameRate;
    }

    @Override
    public int getActualFps() {
        return renderer.getFps();
    }


    @Override
    public BitmapFont generateBitmapFont(TrueTypeFontBuilder builder) {
        return BitmapTextAwtHelper.generateBitmapFont(builder);
    }
}
