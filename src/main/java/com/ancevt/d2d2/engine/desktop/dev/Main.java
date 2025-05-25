package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.scene.BasicSprite;
import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.Group;
import com.ancevt.d2d2.scene.Sprite;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.util.Args;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "D2D2 OpenGL Renderer";

    public static void main(String[] args) {

        Args a = Args.of(args);
        int count = a.get(int.class, "-count", 100);

        SpriteRenderer.batchSize = a.get(int.class, "-batch-size", 5000);

        // === GLFW инициализация ===
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW не инициализирован");
        }

        // Настройка окна: core profile, OpenGL 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        // Создание окна
        long window = GLFW.glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, 0, 0);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Не удалось создать окно");
        }

        // Установка контекста
        GLFW.glfwMakeContextCurrent(window);

        // Включаем VSync (1 = включить, 0 = выключить)
        GLFW.glfwSwapInterval(1);

        // Показываем окно
        GLFW.glfwShowWindow(window);

        // === Инициализация OpenGL (через GL.createCapabilities) ===
        GL.createCapabilities();

        // === Инициализация рендерера ===
        SpriteRenderer renderer = new SpriteRenderer();
        renderer.init();
        renderer.setProjection(WINDOW_WIDTH, WINDOW_HEIGHT);

        // === Здесь ты создаешь сцену (группы/спрайты) ===
        Group stage = Group.create();

        String filePath = "src/main/resources/assets/test.png";// путь к файлу с текстурой
        File file = new File(filePath);
        System.out.println(filePath + " exists: " + file.exists());

        Texture texture = SpriteRenderer.loadTexture(filePath);// загрузка текстуры

        Group g1 = Group.create();
        g1.setPosition(0, 0);
        g1.rotate(50f);
        g1.setScaleX(2f);
        stage.addChild(g1);

        Random r = new Random();

        List<Sprite> sprites = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Sprite s = new BasicSprite();
            s.setColor(Color.createVisibleRandomColor());
            s.setTextureRegion(texture.createTextureRegion());
            s.setPosition(r.nextFloat() * 500, r.nextFloat() * 500);
            g1.addChild(s);
            sprites.add(s);
        }

        long lastTime = System.nanoTime();
        int frameCount = 0;

        // === Главный цикл ===
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Очистка экрана
            GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            // Рендер сцены
            renderer.render(stage);

            g1.rotate(0.05f);
            if(g1.getRotation() > 20.0) g1.setRotation(0);

            // Свап буферов и обработка событий
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            // FPS
            frameCount++;

            long now = System.nanoTime();
            if (now - lastTime >= 5_000_000_000L) { // 5 секунд = 5e9 наносекунд
                double elapsedSeconds = (now - lastTime) / 1_000_000_000.0;
                double fps = frameCount / elapsedSeconds;
                System.out.printf("count: %d, batch-size: %d, FPS: %.2f%n", count, SpriteRenderer.batchSize, fps);

                frameCount = 0;
                lastTime = now;
            }

        }

        // === Освобождение ресурсов ===
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
