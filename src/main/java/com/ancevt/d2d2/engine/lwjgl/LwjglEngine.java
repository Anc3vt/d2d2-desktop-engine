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

package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.InputEvent;
import com.ancevt.d2d2.event.core.EventDispatcherImpl;
import com.ancevt.d2d2.input.Mouse;
import com.ancevt.d2d2.lifecycle.D2D2PropertyConstants;
import com.ancevt.d2d2.log.Log;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Root;
import com.ancevt.d2d2.scene.interactive.InteractiveManager;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.FractionalMetrics;
import com.ancevt.d2d2.scene.text.TrueTypeFontBuilder;
import com.ancevt.d2d2.time.Timer;
import lombok.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.ancevt.d2d2.D2D2.log;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryUtil.NULL;

// TODO: rewrite with VBO and refactor
public class LwjglEngine extends EventDispatcherImpl implements Engine {

    private static final String DEMO_TEXTURE_DATA_INF_FILE = "d2d2-core-demo-texture-data.inf";
    private LwjglRenderer renderer;
    private final int initialWidth;
    private final int initialHeight;
    private final String initialTitle;
    private int mouseX;
    private int mouseY;
    private boolean isDown;
    private Root root;
    private boolean running;
    private int frameRate = 60;
    private boolean alwaysOnTop;
    private boolean control;
    private boolean shift;
    private boolean alt;

    private long windowId;

    @Getter
    private int canvasWidth;

    @Getter
    private int canvasHeight;

    private final LwjglDisplayManager displayManager = new LwjglDisplayManager();

    private SoundManager soundManager;

    @Getter
    @Setter
    private int timerCheckFrameFrequency = 1;

    public LwjglEngine(int initialWidth, int initialHeight, String initialTitle) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.initialTitle = initialTitle;
        this.canvasWidth = initialWidth;
        this.canvasHeight = initialHeight;
        D2D2.textureManager().setTextureEngine(new LwjglTextureEngine());
    }

    @Override
    public SoundManager soundManager() {
        if (soundManager == null) {
            soundManager = new LwjglSoundManager();
        }
        return soundManager;
    }

    @Override
    public void setCanvasSize(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
    }

    @Override
    public Log log() {
        //TODO: refactor
        return new Log() {
            private static final String EMPTY_STRING = "";
            private int level = INFO;
            private boolean colorized = true;

            @Override
            public void setLevel(int level) {
                this.level = level;
            }

            @Override
            public int getLevel() {
                return level;
            }

            @Override
            public void setColorized(boolean colorized) {
                this.colorized = colorized;
            }

            @Override
            public boolean isColorized() {
                return colorized;
            }

            private void logMessage(Object tag, Object msg, PrintStream stream, Throwable throwable) {
                String tagStr = (tag instanceof Class<?> clazz) ? clazz.getSimpleName() : String.valueOf(tag);
                String formattedMsg = UnixTextColorFilter.filterText(String.valueOf(msg), colorized);
                stream.printf("%s: %s%n", tagStr, formattedMsg);
                if (throwable != null) {
                    throwable.printStackTrace(stream);
                }
            }

            @Override
            public void error(Object tag, Object msg) {
                if (level < ERROR) return;
                logMessage(tag, msg, System.err, null);
            }

            @Override
            public void error(Object tag, Object msg, Throwable throwable) {
                if (level < ERROR) return;
                logMessage(tag, msg, System.err, throwable);
            }

            @Override
            public void info(Object tag, Object msg) {
                if (level < INFO) return;
                logMessage(tag, msg, System.out, null);
            }

            @Override
            public void debug(Object tag, Object msg) {
                if (level < DEBUG) return;
                logMessage(tag, msg, System.out, null);
            }
        };

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
        if (!running) return;
        running = false;
    }


    @Override
    public void create() {
        root = new Root();
        renderer = new LwjglRenderer(root, this);
        renderer.setLWJGLTextureEngine((LwjglTextureEngine) D2D2.textureManager().getTextureEngine());
        displayManager.windowId = createWindow();
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
        return ((LwjglRenderer) renderer).smoothMode;
    }

    @Override
    public void start() {
        running = true;
        root.dispatchEvent(CommonEvent.Start.create());
        startRenderLoop();
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

    private long createWindow() {
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

        glfwSetWindowSizeCallback(windowId, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long l, int width, int height) {
                canvasWidth = width;
                canvasHeight = height;
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
                mouseX = (int) (x * root.getWidth() / canvasWidth);
                mouseY = (int) (y * root.getHeight() / canvasHeight);

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


        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        glfwSetWindowPos(
                windowId,
                (videoMode.width() - initialWidth) / 2,
                (videoMode.height() - initialHeight) / 2
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

        setSmoothMode(false);
        return windowId;
    }

    @Override
    public void setCursorXY(int x, int y) {
        GLFW.glfwSetCursorPos(windowId, x, y);
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
            //e.printStackTrace(); // ignore exception
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

    private void startRenderLoop() {

        long windowId = displayManager.getWindowId();

        while (!glfwWindowShouldClose(windowId) && running) {
            glfwPollEvents();
            renderer.renderFrame();
            glfwSwapBuffers(windowId);
            Timer.processTimers();
        }

        String prop = System.getProperty("d2d2.glfw.no-terminate");
        if (prop != null && prop.equals("true")) {
            log.error(getClass(), "d2d2.glfw.no-terminate is set");
            return;
        }

        glfwTerminate();
    }


    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    private static class Size {
        private final int w;
        private final int h;
    }

    /*
    private static Size computeAtlasSize(Font font, String string, TrueTypeBitmapFontBuilder builder) {
        FontMetrics metrics = new Canvas().getFontMetrics(font);
        int width = 0;
        int currentHeight = 0;
        int maxWidth = 0;

        for (int i = 0; i < string.length(); i++) {
            char currentChar = string.charAt(i);
            int charWidth = metrics.charWidth(currentChar);
            width += charWidth;

            if (width > 4096) {
                maxWidth = Math.max(maxWidth, width - charWidth);
                width = charWidth;
                currentHeight += builder.getSpacingY();
            }
        }

        maxWidth = Math.max(maxWidth, width);
        currentHeight += metrics.getHeight();

        return new Size(maxWidth, currentHeight);
    }
     */

    private static Size computeSize(java.awt.Font font, String string, TrueTypeFontBuilder builder) {
        int x = 0;
        int y = 0;
        FontMetrics fontMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().getFontMetrics(font);

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            int w = fontMetrics.charWidth(c);
            int h = fontMetrics.getHeight();

            x += w + builder.getSpacingX();

            if (x >= 2048) {
                y += h + builder.getSpacingY();
                x = 0;
            }
        }

        return new Size(2048, y + font.getSize() * 2 + 128);
    }

    @SneakyThrows
    @Override
    public BitmapFont generateBitmapFont(TrueTypeFontBuilder builder) {

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        InputStream inputStream = builder.getInputStream() != null ?
                builder.getInputStream() : new FileInputStream(builder.getFilePath().toFile());

        java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, inputStream);
        String fontName = font.getName();
        ge.registerFont(font);

        boolean bold = builder.isBold();
        boolean italic = builder.isItalic();
        int fontSize = builder.getFontSize();
        int fontStyle = java.awt.Font.PLAIN | (bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN) | (italic ? java.awt.Font.ITALIC : java.awt.Font.PLAIN);

        font = new java.awt.Font(fontName, fontStyle, fontSize);

        String string = builder.getCharSourceString();

        Size size = computeSize(font, string, builder);

        int textureWidth = size.w;
        int textureHeight = size.h;
        BufferedImage bufferedImage = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();

        if (builder.fractionalMetrics() != null)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, FractionalMetrics.nativeValue(builder.fractionalMetrics()));

        if (builder.isTextAntialiasOn())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (builder.isTextAntialiasGasp())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

        if (builder.isTextAntialiasLcdHrgb())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        if (builder.isTextAntialiasLcdHbgr())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);

        if (builder.isTextAntialiasLcdVrgb())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);

        if (builder.isTextAntialiasLcdVbgr())
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);

        g.setColor(Color.WHITE);

        List<CharInfo> charInfos = new ArrayList<>();

        int x = 0;
        int y = font.getSize();
        FontMetrics fontMetrics = g.getFontMetrics(font);

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            int w = fontMetrics.charWidth(c);
            int h = fontMetrics.getHeight();
            int toY = fontMetrics.getDescent();

            g.setFont(font);
            g.drawString(String.valueOf(c), x, y);

            CharInfo charInfo = new CharInfo();
            charInfo.character = c;
            charInfo.x = x + builder.getOffsetX();
            charInfo.y = y - h + toY + builder.getOffsetY();

            charInfo.width = w + builder.getOffsetX();
            charInfo.height = h + builder.getOffsetY();

            charInfos.add(charInfo);

            x += w + builder.getSpacingX();

            if (x >= bufferedImage.getWidth() - font.getSize()) {
                y += h + builder.getSpacingY();
                x = 0;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        // meta
        stringBuilder.append("#meta ");
        stringBuilder.append("spacingX ").append(builder.getSpacingX()).append(" ");
        stringBuilder.append("spacingY ").append(builder.getSpacingY()).append(" ");
        stringBuilder.append("\n");

        // char infos
        charInfos.forEach(charInfo ->
                stringBuilder
                        .append(charInfo.character)
                        .append(' ')
                        .append(charInfo.x)
                        .append(' ')
                        .append(charInfo.y)
                        .append(' ')
                        .append(charInfo.width)
                        .append(' ')
                        .append(charInfo.height)
                        .append('\n')
        );

        byte[] charsDataBytes = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", pngOutputStream);
        byte[] pngDataBytes = pngOutputStream.toByteArray();

        if (System.getProperty(D2D2PropertyConstants.D2D2_BITMAPFONT_SAVEBMF) != null) {
            String assetPath = builder.getAssetPath();
            Path ttfPath = builder.getFilePath();

            String fileName = assetPath != null ?
                    Path.of(assetPath).getFileName().toString() : ttfPath.getFileName().toString();

            String saveToPathString = System.getProperty(D2D2PropertyConstants.D2D2_BITMAPFONT_SAVEBMF);

            Path destinationPath = Files.createDirectories(Path.of(saveToPathString));

            fileName = fileName.substring(0, fileName.length() - 4) + "-" + fontSize;

            Files.write(destinationPath.resolve(fileName + ".png"), pngDataBytes);
            Files.writeString(destinationPath.resolve(fileName + ".bmf"), stringBuilder.toString());
            log.info(getClass(), "BMF written %s/%s".formatted(destinationPath, fileName));
        }

        return D2D2.bitmapFontManager().loadBitmapFont(
                new ByteArrayInputStream(charsDataBytes),
                new ByteArrayInputStream(pngDataBytes),
                builder.getName()
        );
    }

    private static class CharInfo {
        public char character;
        public int x;
        public int y;
        public int width;
        public int height;

        @Override
        public String toString() {
            return "CharInfo{" +
                    "character=" + character +
                    ", x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
