package com.ancevt.d2d2.engine.desktop.lwjgl;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.engine.desktop.WindowIconLoader;
import com.ancevt.d2d2.event.InputEvent;
import com.ancevt.d2d2.input.Mouse;
import com.ancevt.d2d2.lifecycle.D2D2PropertyConstants;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Root;
import com.ancevt.d2d2.scene.interactive.InteractiveManager;
import com.ancevt.d2d2.time.Timer;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryUtil.NULL;

public class WindowGLFWHelper {

    private static final String DEMO_TEXTURE_DATA_INF_FILE = "d2d2-core-demo-texture-data.inf";

    @Getter
    private static long windowId;

    @Getter
    @Setter
    private static int canvasWidth;

    @Getter
    @Setter
    private static int canvasHeight;

    private static int mouseX;
    private static int mouseY;
    private static boolean isDown;
    private static boolean control;
    private static boolean shift;
    private static boolean alt;

    @Getter
    @Setter
    private static boolean running;

    @Getter
    private static boolean smoothMode;
    @Getter
    private static boolean alwaysOnTop;

    public static void setCanvasSize(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
    }

    public static void init(DesktopEngine engine, int initialWidth, int initialHeight, String initialTitle) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();

        if (Objects.equals(System.getProperty(D2D2PropertyConstants.D2D2_ALWAYS_ON_TOP), "true")) {
            glfwWindowHint(GLFW_FLOATING, 1);
        }

        windowId = glfwCreateWindow(initialWidth, initialHeight, initialTitle, NULL, NULL);

        if (windowId == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        WindowIconLoader.loadIcons(windowId);

        Root root = engine.root();
        Renderer renderer = engine.getRenderer();

        glfwSetWindowSizeCallback(windowId, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long l, int width, int height) {
                engine.setCanvasSize(width, height);
                renderer.reshape();
            }
        });

        glfwSetScrollCallback(windowId, new GLFWScrollCallback() {
            @Override
            public void invoke(long win, double dx, double dy) {
                root.dispatchEvent(InputEvent.MouseWheel.create(
                        (int) dy,
                        Mouse.getX(),
                        Mouse.getY(),
                        alt,
                        control,
                        shift
                ));

            }
        });

        glfwSetMouseButtonCallback(windowId, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int mouseButton, int action, int mods) {
                boolean down = action == GLFW_PRESS;

                root.dispatchEvent(down
                                ? InputEvent.MouseDown.create(
                                Mouse.getX(), Mouse.getY(), mouseButton,
                                mouseButton == GLFW_MOUSE_BUTTON_LEFT,
                                mouseButton == GLFW_MOUSE_BUTTON_RIGHT,
                                mouseButton == GLFW_MOUSE_BUTTON_MIDDLE,
                                (mods & GLFW_MOD_SHIFT) != 0,
                                (mods & GLFW_MOD_CONTROL) != 0,
                                (mods & GLFW_MOD_ALT) != 0
                        )
                                : InputEvent.MouseUp.create(
                                Mouse.getX(), Mouse.getY(), mouseButton,
                                mouseButton == GLFW_MOUSE_BUTTON_LEFT,
                                mouseButton == GLFW_MOUSE_BUTTON_RIGHT,
                                mouseButton == GLFW_MOUSE_BUTTON_MIDDLE,
                                false,
                                (mods & GLFW_MOD_SHIFT) != 0,
                                (mods & GLFW_MOD_CONTROL) != 0,
                                (mods & GLFW_MOD_ALT) != 0
                        )
                );

                InteractiveManager.getInstance().screenTouch(
                        mouseX,
                        mouseY,
                        0,
                        mouseButton,
                        down,
                        (mods & GLFW_MOD_SHIFT) != 0,
                        (mods & GLFW_MOD_CONTROL) != 0,
                        (mods & GLFW_MOD_ALT) != 0
                );
            }
        });

        glfwSetCursorPosCallback(windowId, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                mouseX = (int) (x * root.getWidth() / engine.getCanvasWidth());
                mouseY = (int) (y * root.getHeight() / engine.getCanvasHeight());

                Mouse.setXY(mouseX, mouseY);

                root.dispatchEvent(InputEvent.MouseMove.create(
                        Mouse.getX(),
                        Mouse.getY(),
                        true,
                        alt,
                        control,
                        shift
                ));

                if (isDown) {
                    root.dispatchEvent(InputEvent.MouseDrag.create(
                            Mouse.getX(),
                            Mouse.getY(),
                            0, //TODO: pass mouse button info
                            false,
                            false,
                            false,
                            alt,
                            control,
                            shift
                    ));
                }

                InteractiveManager.getInstance().screenMove(0, mouseX, mouseY, shift, control, alt);
            }
        });

        glfwSetCharCallback(windowId, (window, codepoint) -> {
            root.dispatchEvent(InputEvent.KeyType.create(
                    0,
                    alt,
                    control,
                    shift,
                    Character.toChars(codepoint)[0],
                    codepoint,
                    String.valueOf(Character.toChars(codepoint))
            ));
        });

        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            boolean shiftNow = (mods & GLFW_MOD_SHIFT) != 0;
            boolean ctrlNow = (mods & GLFW_MOD_CONTROL) != 0;
            boolean altNow = (mods & GLFW_MOD_ALT) != 0;

            shift = shiftNow;
            control = ctrlNow;
            alt = altNow;

            switch (action) {
                case GLFW_PRESS -> {
                    root.dispatchEvent(InputEvent.KeyDown.create(
                            key,
                            (char) key,
                            altNow,
                            ctrlNow,
                            shiftNow
                    ));
                }

                case GLFW_REPEAT -> {
                    root.dispatchEvent(InputEvent.KeyRepeat.create(
                            key,
                            altNow,
                            ctrlNow,
                            shiftNow
                    ));
                }

                case GLFW_RELEASE -> {
                    root.dispatchEvent(InputEvent.KeyUp.create(
                            key,
                            altNow,
                            ctrlNow,
                            shiftNow
                    ));
                }
            }
        });

        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        glfwSetWindowPos(
                windowId,
                (videoMode.width() - engine.getCanvasWidth()) / 2,
                (videoMode.height() - engine.getCanvasHeight()) / 2
        );

        glfwMakeContextCurrent(windowId);
        glfwSwapInterval(1); // enable vsync
        GL.createCapabilities();

        // TODO: remove loading demo texture data info from here
        D2D2.textureManager().loadTextureDataInfo(DEMO_TEXTURE_DATA_INF_FILE);
        glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
        glEnable(GL_MULTISAMPLE);

        renderer.init(windowId);
        renderer.reshape();

        engine.setSmoothMode(false);
    }


    public static void startRenderLoop(Engine engine) {
        long windowId = WindowGLFWHelper.getWindowId();

        Renderer renderer = engine.getRenderer();

        while (!glfwWindowShouldClose(windowId) && running) {
            glfwPollEvents();
            renderer.renderFrame();
            glfwSwapBuffers(windowId);
            Timer.processTimers();
        }

        glfwTerminate();
    }

    public static void setCursorXY(int x, int y) {
        GLFW.glfwSetCursorPos(WindowGLFWHelper.getWindowId(), x, y);
    }

    public static void setSmoothMode(boolean smoothMode) {
        WindowGLFWHelper.smoothMode = smoothMode;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        if (smoothMode) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        }
    }

    public static void setAlwaysOnTop(boolean alwaysOnTop) {
        WindowGLFWHelper.alwaysOnTop = alwaysOnTop;
        glfwWindowHint(GLFW_FLOATING, WindowGLFWHelper.alwaysOnTop ? GLFW_TRUE : GLFW_FALSE);
    }

}
