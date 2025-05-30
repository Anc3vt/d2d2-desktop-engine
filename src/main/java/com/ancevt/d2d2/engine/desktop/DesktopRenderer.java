package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.engine.desktop.lwjgl.CanvasControl;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.time.Timer;
import lombok.RequiredArgsConstructor;
import org.lwjgl.glfw.GLFW;

@RequiredArgsConstructor
public class DesktopRenderer implements Renderer {

    private final DesktopEngine engine;

    private boolean running = true;

    @Override
    public void init(long windowId) {

    }

    @Override
    public void reshape() {

    }

    @Override
    public void renderFrame() {

    }

    public void startRenderLoop() {

        long windowId = CanvasControl.getWindowId();

        while (!GLFW.glfwWindowShouldClose(windowId) && running) {
            GLFW.glfwPollEvents();
            renderFrame();
            GLFW.glfwSwapBuffers(windowId);
            Timer.processTimers();
        }

        GLFW.glfwTerminate();
    }
}
