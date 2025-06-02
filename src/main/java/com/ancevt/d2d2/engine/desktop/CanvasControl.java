package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.engine.desktop.render.DesktopRenderer;
import com.ancevt.d2d2.event.InputEvent;
import com.ancevt.d2d2.input.Mouse;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.scene.interactive.InteractiveManager;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class CanvasControl {

    @Getter
    @Setter
    private static int width;

    @Getter
    @Setter
    private static int height;

    @Getter
    private static String title;

    private static boolean isDown;
    private static boolean control;
    private static boolean shift;
    private static boolean alt;
    private static int mouseX;
    private static int mouseY;

    @Getter
    private static long windowId;

    public static void init(int width, int height, String title) {
        CanvasControl.width = width;
        CanvasControl.height = height;
        CanvasControl.title = title;
    }

    public static void createAndSetupGlfwWindow(DesktopEngine engine) {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW is not initialized");
        }

        GLFWErrorCallback.createPrint(System.err).set();

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        GLFW.glfwDefaultWindowHints();

        windowId = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (windowId == MemoryUtil.NULL) {
            throw new RuntimeException("Unable to create window");
        }

        GLFW.glfwMakeContextCurrent(windowId);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(windowId);

        GL.createCapabilities();

        Stage stage = engine.getStage();
        DesktopRenderer renderer = (DesktopRenderer) engine.getRenderer();

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
                stage.dispatchEvent(InputEvent.MouseWheel.create(
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

                stage.dispatchEvent(down
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
                mouseX = (int) (x * stage.getWidth() / engine.getCanvasWidth());
                mouseY = (int) (y * stage.getHeight() / engine.getCanvasHeight());

                Mouse.setXY(mouseX, mouseY);

                stage.dispatchEvent(InputEvent.MouseMove.create(
                        Mouse.getX(),
                        Mouse.getY(),
                        true,
                        alt,
                        control,
                        shift
                ));

                if (isDown) {
                    stage.dispatchEvent(InputEvent.MouseDrag.create(
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
            stage.dispatchEvent(InputEvent.KeyType.create(
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
                    stage.dispatchEvent(InputEvent.KeyDown.create(
                            key,
                            (char) key,
                            altNow,
                            ctrlNow,
                            shiftNow
                    ));
                }

                case GLFW_REPEAT -> {
                    stage.dispatchEvent(InputEvent.KeyRepeat.create(
                            key,
                            altNow,
                            ctrlNow,
                            shiftNow
                    ));
                }

                case GLFW_RELEASE -> {
                    stage.dispatchEvent(InputEvent.KeyUp.create(
                            key,
                            altNow,
                            ctrlNow,
                            shiftNow
                    ));
                }
            }
        });

        renderer.init(windowId);

    }

    public static void setSize(int width, int height) {
        CanvasControl.width = width;
        CanvasControl.height = height;
    }
}
