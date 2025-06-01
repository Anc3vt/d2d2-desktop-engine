package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.core.EventDispatcherImpl;
import com.ancevt.d2d2.log.Logger;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.FontBuilder;
import lombok.Getter;

public class DesktopEngine extends EventDispatcherImpl implements Engine {

    @Getter
    private final int initialWidth;
    @Getter
    private final int initialHeight;

    private Stage stage;
    private DesktopRenderer renderer;

    public DesktopEngine(int initialWidth, int initialHeight, String initialTitle) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        CanvasControl.init(initialWidth, initialHeight, initialTitle);
        D2D2.textureManager().setTextureEngine(new DesktopTextureEngine());
    }

    @Override
    public void create() {
        stage = new Stage();
        stage.setSize(initialWidth, initialHeight);
        renderer = new DesktopRenderer(this);
        CanvasControl.createAndSetupGlfwWindow(this);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setAlwaysOnTop(boolean b) {

    }

    @Override
    public boolean isAlwaysOnTop() {
        return false;
    }

    @Override
    public void setFrameRate(int value) {

    }

    @Override
    public int getFrameRate() {
        return 0;
    }

    @Override
    public int getActualFps() {
        return 0;
    }

    @Override
    public void start() {
        renderer.running = true;
        stage.dispatchEvent(CommonEvent.Start.create());
        renderer.startRenderLoop();
        stage.dispatchEvent(CommonEvent.Stop.create());
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public void stop() {
        renderer.running = false;
    }

    @Override
    public void putToClipboard(String string) {

    }

    @Override
    public String getStringFromClipboard() {
        return "";
    }

    @Override
    public BitmapFont generateBitmapFont(FontBuilder fontBuilder) {
        return null;
    }

    @Override
    public void setTimerCheckFrameFrequency(int v) {

    }

    @Override
    public int getTimerCheckFrameFrequency() {
        return 0;
    }

    @Override
    public DisplayManager displayManager() {
        return null;
    }

    @Override
    public SoundManager soundManager() {
        return null;
    }

    @Override
    public void setCursorXY(int x, int y) {

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
}
