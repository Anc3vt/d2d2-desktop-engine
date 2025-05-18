package com.ancevt.d2d2.engine.desktop.lwjgl;

import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.event.InputEvent;
import com.ancevt.d2d2.input.Mouse;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Root;
import com.ancevt.d2d2.scene.interactive.InteractiveManager;
import lombok.RequiredArgsConstructor;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

import static org.lwjgl.glfw.GLFW.*;

@RequiredArgsConstructor
public class GLFWHelper {

    private final DesktopEngine engine;

    private int mouseX;
    private int mouseY;
    private boolean isDown;
    private boolean control;
    private boolean shift;
    private boolean alt;


    public void init() {
        long windowId = engine.getWindowId();
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
                        true // or false — ты сам решаешь, но сейчас логика “onArea” не применима
                        , alt,
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
    }


}
