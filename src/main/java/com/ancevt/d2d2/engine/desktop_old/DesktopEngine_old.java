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

package com.ancevt.d2d2.engine.desktop_old;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.NodeFactory;
import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.engine.desktop_old.awt.AwtBitmapFontGenerator_old;
import com.ancevt.d2d2.engine.desktop_old.lwjgl.CanvasHelper_old;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.core.EventDispatcherImpl;
import com.ancevt.d2d2.exception.NotImplementedException;
import com.ancevt.d2d2.log.Logger;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.FontBuilder;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class DesktopEngine_old extends EventDispatcherImpl implements Engine {

    private DesktopRenderer_old renderer;

    private Stage stage;

    private final DesktopDisplayManager_old displayManager = new DesktopDisplayManager_old();

    @Getter
    @Setter
    private int timerCheckFrameFrequency = 1;

    public DesktopEngine_old(int initialWidth, int initialHeight, String initialTitle) {
        CanvasHelper_old.init(initialWidth, initialHeight, initialTitle);
        D2D2.textureManager().setTextureEngine(new DesktopTextureEngine_old());
    }

    @Override
    public SoundManager soundManager() {
        return DesktopSoundManager_old.getInstance();
    }

    @Override
    public void setCanvasSize(int width, int height) {
        CanvasHelper_old.setCanvasSize(width, height);
    }

    @Override
    public int getCanvasWidth() {
        return CanvasHelper_old.getCanvasWidth();
    }

    @Override
    public int getCanvasHeight() {
        return CanvasHelper_old.getCanvasHeight();
    }

    @Override
    public Logger logger() {
        return DesktopLogger_old.getInstance();
    }

    @Override
    public DisplayManager displayManager() {
        return displayManager;
    }

    @Override
    public void setAlwaysOnTop(boolean b) {
        CanvasHelper_old.setAlwaysOnTop(b);
    }

    @Override
    public boolean isAlwaysOnTop() {
        return CanvasHelper_old.isAlwaysOnTop();
    }

    @Override
    public void stop() {
        if (!CanvasHelper_old.isRunning()) return;
        CanvasHelper_old.setRunning(false);
    }

    @Override
    public void create() {
        stage = new Stage();
        renderer = new DesktopRenderer_old(stage, this);
        renderer.setDesktopTextureEngine((DesktopTextureEngine_old) D2D2.textureManager().getTextureEngine());
        CanvasHelper_old.createAndSetupGLFWWindow(this);
        displayManager.setVisible(true);
        stage.setSize(CanvasHelper_old.getCanvasWidth(), CanvasHelper_old.getCanvasHeight());
        renderer.reshape();
    }

    @Override
    public void setSmoothMode(boolean value) {
        CanvasHelper_old.setSmoothMode(value);
    }

    @Override
    public boolean isSmoothMode() {
        return CanvasHelper_old.isSmoothMode();
    }

    @Override
    public void start() {
        CanvasHelper_old.setRunning(true);
        stage.dispatchEvent(CommonEvent.Start.create());
        CanvasHelper_old.startRenderLoop(this);
        stage.dispatchEvent(CommonEvent.Stop.create());
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public void setCursorXY(int x, int y) {
        CanvasHelper_old.setCursorXY(x, y);
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
        renderer.setFrameRate(frameRate);
    }

    @Override
    public int getFrameRate() {
        return renderer.getFrameRate();
    }

    @Override
    public int getActualFps() {
        return renderer.getFps();
    }


    @Override
    public BitmapFont generateBitmapFont(FontBuilder builder) {
        return AwtBitmapFontGenerator_old.generate(builder);
    }

    @Override
    public NodeFactory nodeFactory() {
        throw new NotImplementedException();
    }
}
