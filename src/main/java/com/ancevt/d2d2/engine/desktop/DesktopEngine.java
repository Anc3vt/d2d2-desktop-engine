package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.NodeFactory;
import com.ancevt.d2d2.engine.desktop.node.DesktopNodeFactory;
import com.ancevt.d2d2.engine.desktop.render.DesktopRenderer;
import com.ancevt.d2d2.engine.desktop.render.ShaderProgramImpl;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.core.EventDispatcherImpl;
import com.ancevt.d2d2.exception.NotImplementedException;
import com.ancevt.d2d2.log.Logger;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.scene.shader.ShaderProgram;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.FontBuilder;
import com.ancevt.d2d2.scene.texture.TextureManager;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class DesktopEngine extends EventDispatcherImpl implements Engine {

    @Getter
    private final int initialWidth;
    @Getter
    private final int initialHeight;

    private Stage stage;
    @Getter
    private DesktopRenderer renderer;
    @Getter
    private DesktopDisplayManager displayManager;
    @Getter
    private DesktopSoundManager soundManager;
    @Getter
    private NodeFactory nodeFactory;
    @Getter
    private TextureManager textureManager;
    private int timerCheckFrameFrequency;

    public DesktopEngine(int initialWidth, int initialHeight, String initialTitle) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        CanvasControl.init(initialWidth, initialHeight, initialTitle);

    }

    @Override
    public void init() {
        stage = new Stage();
        stage.setSize(initialWidth, initialHeight);
        renderer = new DesktopRenderer(this);
        displayManager = new DesktopDisplayManager();
        soundManager = new DesktopSoundManager();
        nodeFactory = new DesktopNodeFactory();
        textureManager = new DesktopTextureManager();

        CanvasControl.createAndSetupGlfwWindow(this);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setAlwaysOnTop(boolean b) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAlwaysOnTop() {
        return false;
    }

    @Override
    public void setFrameRate(int value) {
        renderer.setFrameRate(value);
    }

    @Override
    public int getFrameRate() {
        return renderer.getFrameRate();
    }

    @Override
    public int getActualFps() {
        return renderer.getActualFps();
    }

    @Override
    public void start() {
        renderer.setRunning(true);
        stage.dispatchEvent(CommonEvent.Start.create());
        renderer.startRenderLoop();
        stage.dispatchEvent(CommonEvent.Stop.create());
    }

    @Override
    public void stop() {
        renderer.setRunning(false);
    }

    @Override
    public void putStringToClipboard(String string) {
        StringSelection stringSelection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    @Override
    public String getStringFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            e.printStackTrace(); // или логировать
        }
        return "";
    }


    @Override
    public BitmapFont generateBitmapFont(FontBuilder fontBuilder) {
        return AwtBitmapFontGenerator.generate(fontBuilder);
    }

    @Override
    public void setTimerCheckFrameFrequency(int v) {
        timerCheckFrameFrequency = v;
    }

    @Override
    public int getTimerCheckFrameFrequency() {
        return timerCheckFrameFrequency;
    }

    @Override
    public void setCursorXY(int x, int y) {
        long windowId = CanvasControl.getWindowId();
        GLFW.glfwSetCursorPos(windowId, x, y);
    }

    @Override
    public void setCanvasSize(int width, int height) {
        CanvasControl.setSize(width, height);
    }

    @Override
    public int getCanvasWidth() {
        return CanvasControl.getWidth();
    }

    @Override
    public int getCanvasHeight() {
        return CanvasControl.getHeight();
    }

    @Override
    public Logger logger() {
        return DesktopLogger.getInstance();
    }

    @Override
    public ShaderProgram createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        return new ShaderProgramImpl(vertexShaderSource, fragmentShaderSource);
    }
}
