package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.scene.Group;
import com.ancevt.d2d2.scene.Node;
import com.ancevt.d2d2.scene.Sprite;
import com.ancevt.d2d2.scene.texture.Texture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Основной класс высокопроизводительного рендерера на LWJGL3.
 * Рендерер умеет отображать объекты, реализующие интерфейсы Node, Group и Sprite,
 * используя VAO/VBO и батчинг спрайтов для высокой производительности.
 */
public class SpriteRenderer {

    // Максимальное количество спрайтов в одном батче (например, 1000).
    // Это ограничение можно увеличить, изменив BATCH_SIZE и пересоздав буферы.
    public static int batchSize = 20000;

    // Константы для размеров атрибутов вершины:
    // позиция (x, y) и текстурные координаты (u, v).
    private static final int FLOATS_PER_VERTEX = 4;       // 2 для позиции + 2 для UV
    private static final int VERTICES_PER_SPRITE = 4;     // 4 вершины на каждый спрайт (четырёхугольник)
    private static final int INDICES_PER_SPRITE = 6;      // 6 индексов (2 треугольника) на спрайт

    // OpenGL идентификаторы для VAO, VBO, EBO (Index Buffer) и шейдерной программы.
    private int vaoId;
    private int vboId;
    private int eboId;
    private int shaderProgram;

    // Локации uniform-переменных в шейдере.
    private int uProjectionLocation;
    private int uTextureLocation;

    // Буфер для вершинных данных (динамический), рассчитанный на BATCH_SIZE спрайтов.
    private final FloatBuffer vertexBuffer;

    // Матрица проекции (4x4) для перевода координат в пространство клипа OpenGL.
    // Используется ортографическая проекция для 2D.
    private final float[] projectionMatrix = new float[16];

    public SpriteRenderer() {
        // Выделяем буфер под максимальное число вершин (BATCH_SIZE спрайтов * 4 вершины * 4 компонента).
        this.vertexBuffer = BufferUtils.createFloatBuffer(batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
    }

    /**
     * Инициализация рендерера: создание шейдеров, VAO, VBO, EBO и настроек OpenGL.
     * Это нужно вызвать один раз после создания OpenGL-контекста.
     */
    public void init() {
        // Компиляция вершинного шейдера
        String vertexShaderSrc =
                "#version 330 core\n" +
                        "layout(location = 0) in vec2 aPos;\n" +
                        "layout(location = 1) in vec2 aTexCoord;\n" +
                        "uniform mat4 uProjection;\n" +
                        "out vec2 vTexCoord;\n" +
                        "void main() {\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);\n" +
                        "}\n";
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexShaderSrc);

        // Компиляция фрагментного шейдера
        String fragmentShaderSrc =
                "#version 330 core\n" +
                        "in vec2 vTexCoord;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main() {\n" +
                        "    // Семплируем цвет из текстуры с учётом прозрачности (альфа)\n" +
                        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
                        "    FragColor = texColor;\n" +
                        "}\n";
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSrc);

        // Создание и линковка шейдерной программы
        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        // Проверяем успешность линковки
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String infoLog = GL20.glGetProgramInfoLog(shaderProgram);
            System.err.println("Ошибка линковки шейдерной программы: " + infoLog);
        }
        // Шейдеры компилированы, можно удалить их исходники из GPU
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        // Получаем локации uniform-переменных для последующего использования
        uProjectionLocation = GL20.glGetUniformLocation(shaderProgram, "uProjection");
        uTextureLocation = GL20.glGetUniformLocation(shaderProgram, "uTexture");

        // Создаём VAO (Vertex Array Object) для хранения настроек атрибутов
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Создаём VBO (Vertex Buffer Object) для вершинных данных спрайтов
        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        // Выделяем память на GPU под максимальный объём данных, пока без заполнения (NULL), с динамическим использованием
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX * Float.BYTES,
                GL15.GL_DYNAMIC_DRAW);

        // Создаём и заполняем EBO (Element Buffer Object) для индексированных отрисовок квадов
        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        // Генерируем массив индексов для всех спрайтов в батче
        int[] indices = new int[batchSize * INDICES_PER_SPRITE];
        for (int i = 0; i < batchSize; i++) {
            int offset = i * VERTICES_PER_SPRITE;
            int indexOffset = i * INDICES_PER_SPRITE;
            // Треугольники: (0,1,2) и (2,3,0) для каждого спрайта (четырёхугольника)
            indices[indexOffset] = offset;
            indices[indexOffset + 1] = offset + 1;
            indices[indexOffset + 2] = offset + 2;
            indices[indexOffset + 3] = offset + 2;
            indices[indexOffset + 4] = offset + 3;
            indices[indexOffset + 5] = offset;
        }
        // Загружаем индексные данные в EBO на GPU (статические, так как не меняются)
        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.length);
        indicesBuffer.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        // Указываем OpenGL структуру вершинных атрибутов:
        // Атрибут 0 - позиция (vec2), 2 float, сдвиг 0
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        // Атрибут 1 - текстурные координаты (vec2), 2 float, сдвиг 2 * sizeof(float)
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // Отвязываем VBO (VAO сохранит ссылку на него для атрибутов)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        // Отвязывать GL_ELEMENT_ARRAY_BUFFER не нужно до отвязки VAO (VAO хранит его состояние)
        GL30.glBindVertexArray(0);
        // Можно отвязать EBO после отвязки VAO, чтобы случайно не повредить данные
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Включаем режим смешивания (blending) для поддержки прозрачности
        GL11.glEnable(GL11.GL_BLEND);
        // Настраиваем функцию смешивания для альфа-композиции:
        // srcAlpha контролирует видимость (стандартный режим прозрачности)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Отключаем глубину (Z-тест), так как будем сортировать и рисовать в нужном порядке сами
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // Активируем текстурный блок 0 и привязываем uniform uTexture к этому блоку
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1i(uTextureLocation, 0); // 0 соответствует GL_TEXTURE0
        GL20.glUseProgram(0);
    }

    /**
     * Установка ортографической проекции для 2D-координат.
     *
     * @param width  ширина окна/экранного пространства в пикселях.
     * @param height высота окна/экранного пространства в пикселях.
     *               <p>
     *               В этой проекции точка (0,0) находится в левом-верхнем углу,
     *               а ось Y увеличивается вниз (как в координатах экранов и AS3).
     */
    public void setProjection(int width, int height) {
        // Вычисляем матрицу ортографической проекции:
        // Используем диапазон x: [0, width], y: [0, height] (0 вверху) -> преобразование в NDC [-1,1].
        float l = 0;
        float r = width;
        float t = 0;
        float b = height;
        float n = -1;  // ближняя плоскость (можно -1 для 2D)
        float f = 1;   // дальняя плоскость (+1)
        // Заполняем элементы матрицы 4x4 (column-major для OpenGL)
        for (int i = 0; i < 16; i++) projectionMatrix[i] = 0.0f;
        projectionMatrix[0] = 2.0f / (r - l);
        projectionMatrix[5] = 2.0f / (t - b);
        projectionMatrix[10] = -2.0f / (f - n);
        projectionMatrix[12] = -(r + l) / (r - l);
        projectionMatrix[13] = -(t + b) / (t - b);
        projectionMatrix[14] = -(f + n) / (f - n);
        projectionMatrix[15] = 1.0f;
        // Обратите внимание: выше projectionMatrix[5] = 2/(t-b), где t<b, даст отрицательное значение,
        // что инвертирует ось Y (0 вверху).
    }

    /**
     * Выполняет отрисовку сцены, начиная с указанного узла (Node).
     *
     * @param root корневой узел (сцены или группы), содержащий группы и спрайты.
     */
    public void render(Node root) {
        // Перед отрисовкой предполагается, что кадр очищен (glClear) вне этого метода.
        // Здесь мы собираем все спрайты с их мировыми трансформациями и сортируем их по z-order.

        // Собираем все спрайты в список с вычисленными глобальными трансформациями
        List<SpriteDrawInfo> spritesToDraw = new ArrayList<>();
        // Запускаем рекурсивный обход дерева, начиная с корневого узла, без родительской трансформации (identity)
        collectNodes(root,
                // Параметры единичной матрицы 2D-трансформации:
                1.0f, 0.0f, 0.0f,   // a, b, c для матрицы 3x3 (a,b - косинус/син, c - трансляция X)
                0.0f, 1.0f, 0.0f,   // d, e, f (d,e - син/кос, f - трансляция Y)
                spritesToDraw);

        // Сортируем список спрайтов по их глобальному индексу Z-order (возрастание индекса).
        // Это определяет порядок рисования: спрайты с меньшим zIndex рисуются раньше (под другими).
        spritesToDraw.sort(Comparator.comparingInt(info -> info.zOrder));

        // Активация нашего шейдера и VAO перед рисованием
        GL20.glUseProgram(shaderProgram);
        // Загружаем матрицу проекции в шейдер (uniform uProjection)
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(vaoId);

        // Переменные для батчинга
        int spritesInBatch = 0;         // сколько спрайтов уже накоплено в текущем батче
        int currentTextureId = -1;      // текущая текстура, с которой собираем батч

        // Подготовка к обновлению VBO: связываем VBO для записи данных (VAO уже связан)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        vertexBuffer.clear(); // сбрасываем позицию в нашем буфере для новых данных

        for (SpriteDrawInfo info : spritesToDraw) {
            int texId = info.textureId;
            // Если мы начали новый батч или текстура изменилась, то "сбрасываем" (рисуем) предыдущий батч
            if (currentTextureId == -1) {
                // Стартовый случай: устанавливаем текущую текстуру
                currentTextureId = texId;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
            } else if (texId != currentTextureId) {
                // Текстура изменилась — сначала отрисовываем все накопленные спрайты с прошлой текстурой
                flushBatch(spritesInBatch);
                spritesInBatch = 0;
                // Устанавливаем новую текущую текстуру для следующего батча
                currentTextureId = texId;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
            }

            // Если достигли максимального размера батча, сбрасываем текущее содержимое
            if (spritesInBatch >= batchSize) {
                flushBatch(spritesInBatch);
                spritesInBatch = 0;
                // Повторно привязываем текущую текстуру (она остаётся той же, т.к. не менялась в этом случае)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
            }

            // Вычисляем вершинные координаты спрайта (4 вершины квадрата) с помощью его матрицы трансформации
            float x = info.x;       // глобальная трансляция X (c)
            float y = info.y;       // глобальная трансляция Y (f)
            float a = info.a, b = info.b, d = info.d, e = info.e;
            float w = info.width;
            float h = info.height;
            // Четыре угла спрайта в локальных координатах:
            // (0,0) - верхний левый угол
            // (w,0) - верхний правый
            // (w,h) - нижний правый
            // (0,h) - нижний левый
            // Применяем глобальную аффинную трансформацию к каждому углу:
            // Глобальные координаты = матрица [a b c; d e f] * [x_local; y_local; 1]
            float x0 = x;
            float y0 = y;
            float x1 = a * w + x + b * 0;      // = a*w + c
            float y1 = d * w + y + e * 0;      // = d*w + f
            float x2 = a * w + b * h + x;      // = a*w + b*h + c
            float y2 = d * w + e * h + y;      // = d*w + e*h + f
            float x3 = b * h + x;             // = b*h + c
            float y3 = e * h + y;             // = e*h + f

            // Текстурные координаты (u,v) для вершин:
            // Предполагается, что текстура покрывает весь спрайт.
            // (0,0) локальный -> (u=0, v=0) нижний левый угол текстуры
            // (w,h) локальный -> (u=1, v=1) верхний правый угол текстуры.
            // Однако так как у нас 0,0 локально - верхний левый угол спрайта,
            // мы инвертируем координату v (OpenGL ожидает (0,0) внизу).
            float u0 = 0.0f, v0 = 1.0f;   // для вершины (0,0) спрайта: левый верх -> u=0, v=1
            float u1 = 1.0f, v1 = 1.0f;   // (w,0): правый верх -> u=1, v=1
            float u2 = 1.0f, v2 = 0.0f;   // (w,h): правый низ -> u=1, v=0
            float u3 = 0.0f, v3 = 0.0f;   // (0,h): левый низ -> u=0, v=0

            // Заполняем данные по вершинам в буфер.
            // Каждая вершина: {x, y, u, v}.
            int baseIndex = spritesInBatch * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX;
            // Верхний левый угол
            vertexBuffer.put(baseIndex, x0);
            vertexBuffer.put(baseIndex + 1, y0);
            vertexBuffer.put(baseIndex + 2, u0);
            vertexBuffer.put(baseIndex + 3, v0);
            // Верхний правый угол
            vertexBuffer.put(baseIndex + 4, x1);
            vertexBuffer.put(baseIndex + 5, y1);
            vertexBuffer.put(baseIndex + 6, u1);
            vertexBuffer.put(baseIndex + 7, v1);
            // Нижний правый угол
            vertexBuffer.put(baseIndex + 8, x2);
            vertexBuffer.put(baseIndex + 9, y2);
            vertexBuffer.put(baseIndex + 10, u2);
            vertexBuffer.put(baseIndex + 11, v2);
            // Нижний левый угол
            vertexBuffer.put(baseIndex + 12, x3);
            vertexBuffer.put(baseIndex + 13, y3);
            vertexBuffer.put(baseIndex + 14, u3);
            vertexBuffer.put(baseIndex + 15, v3);

            spritesInBatch++;
        }

        // После обхода - рисуем оставшиеся неотрисованные спрайты
        if (spritesInBatch > 0) {
            flushBatch(spritesInBatch);
        }

        // Очистка состояний: отвязываем VAO, VBO и шейдер
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    /**
     * Рекурсивно обходит дерево узлов и собирает информацию о каждом спрайте (Sprite),
     * вычисляя его глобальные трансформационные коэффициенты и z-index.
     *
     * @param node    текущий узел (Sprite или Group)
     * @param parentA элемент a матрицы родителя (a = cos*scaleX родителя)
     * @param parentB элемент b матрицы родителя (b = -sin*scaleY родителя)
     * @param parentC элемент c матрицы родителя (c = трансляция X родителя)
     * @param parentD элемент d матрицы родителя (d = sin*scaleX родителя)
     * @param parentE элемент e матрицы родителя (e = cos*scaleY родителя)
     * @param parentF элемент f матрицы родителя (f = трансляция Y родителя)
     * @param outList список, в который добавляется информация о спрайтах для отрисовки.
     */
    private void collectNodes(Node node,
                              float parentA, float parentB, float parentC,
                              float parentD, float parentE, float parentF,
                              List<SpriteDrawInfo> outList) {
        // Извлекаем локальные трансформации узла
        float x = node.getX();
        float y = node.getY();
        float scaleX = node.getScaleX();
        float scaleY = node.getScaleY();
        float rotation = node.getRotation();
        // Конвертируем угол поворота из градусов в радианы (предполагаем, что rotation задан в градусах, как в AS3)
        float rad = (float) Math.toRadians(rotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        // Формируем локальную матрицу узла [a2 b2 c2; d2 e2 f2], где:
        float a2 = cos * scaleX;
        float b2 = -sin * scaleY;
        float d2 = sin * scaleX;
        float e2 = cos * scaleY;
        float c2 = x;
        float f2 = y;
        // Вычисляем глобальную (комбинированную) матрицу для этого узла: parentMatrix * localMatrix
        float a = parentA * a2 + parentB * d2;
        float b = parentA * b2 + parentB * e2;
        float c = parentA * c2 + parentB * f2 + parentC;
        float d = parentD * a2 + parentE * d2;
        float e = parentD * b2 + parentE * e2;
        float f = parentD * c2 + parentE * f2 + parentF;

        if (node instanceof Sprite) {
            // Если узел - Sprite, сохраняем информацию о нём
            Sprite sprite = (Sprite) node;
            SpriteDrawInfo info = new SpriteDrawInfo();
            info.a = a;
            info.b = b;
            info.c = c;
            info.d = d;
            info.e = e;
            info.f = f;
            info.x = c;  // глобальное смещение по X (c)
            info.y = f;  // глобальное смещение по Y (f)
            info.width = sprite.getWidth();
            info.height = sprite.getHeight();
            info.textureId = getSpriteTextureId(sprite); // идентификатор текстуры спрайта
            info.zOrder = sprite.getGlobalZOrderIndex(); // глобальный z-индекс спрайта для сортировки
            // (Предполагается, что Sprite имеет метод getTextureId() и getGlobalZOrderIndex())

            outList.add(info);
        }
        if (node instanceof Group group) {
            List<Node> children = group.children().toList();
            for (Node child : children) {
                collectNodes(child, a, b, c, d, e, f, outList);
            }
        }
    }

    private static int getSpriteTextureId(Sprite sprite) {
        if (sprite != null && sprite.getTextureRegion() != null) {
            return sprite.getTextureRegion().getTexture().getId();
        }
        return -1;
    }

    /**
     * Вспомогательный метод: рендерит текущий накопленный батч спрайтов (spritesInBatch штук).
     * Выполняет обновление VBO на GPU и вызов glDrawElements с нужным числом индексов.
     *
     * @param spriteCount количество спрайтов в текущем батче
     */
    private void flushBatch(int spriteCount) {
        if (spriteCount == 0) return;
        // Подготовка данных: скопировать накопленные вершины в GPU VBO и выполнить рисование
        // Мы уже заполнили float-буфер vertexBuffer путём вызова put(...).
        // Устанавливаем лимит буфера на используемый объём данных (spriteCount * 4 вершин * 4 значений)
        vertexBuffer.limit(spriteCount * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
        vertexBuffer.position(0);
        // Обновляем содержимое VBO на GPU новыми данными (с начала буфера до текущего лимита)
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);
        // Вызываем отрисовку всех накопленных индексов: по 6 индексов на каждый спрайт
        int indexCount = spriteCount * INDICES_PER_SPRITE;
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        // После отрисовки можно сбросить буфер для следующего батча (здесь просто оставляем position = 0 для перезаписи)
        vertexBuffer.clear();
    }

    /**
     * Компилирует шейдер OpenGL из исходного текста и возвращает его ID.
     *
     * @param type   тип шейдера (GL20.GL_VERTEX_SHADER или GL20.GL_FRAGMENT_SHADER)
     * @param source исходный код шейдера
     * @return идентификатор шейдера
     */
    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        // Проверка на ошибки компиляции
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String infoLog = GL20.glGetShaderInfoLog(shaderId);
            System.err.println("Ошибка компиляции шейдера: " + infoLog);
        }
        return shaderId;
    }

    /**
     * Вспомогательный класс для хранения информации о спрайте при сборе перед отрисовкой.
     * Содержит глобальные параметры трансформации (матрицы) и другие данные для вершин.
     */
    private static class SpriteDrawInfo {
        float a, b, c, d, e, f;    // коэффициенты аффинной матрицы [a b c; d e f] спрайта
        float x, y;                // то же, что c и f (глобальная позиция)
        float width, height;       // размеры спрайта (оригинальные, до трансформации)
        int textureId;             // OpenGL ID текстуры спрайта
        int zOrder;                // глобальный индекс Z-порядка для сортировки
    }

    // Пример функции загрузки текстуры из PNG файла с помощью STBImage.
    public static Texture loadTexture(String filePath) {
        // Используем MemoryStack для временных буферов под размеры
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Загружаем изображение в ByteBuffer (RGBA)
            STBImage.stbi_set_flip_vertically_on_load(true); // переворачиваем по вертикали для правильной ориентации
            ByteBuffer image = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (image == null) {
                throw new RuntimeException("Не удалось загрузить текстуру: " + STBImage.stbi_failure_reason());
            }

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            // Параметры фильтрации и повторения (Clamp to Edge, чтобы избежать артефактов на границах прозрачности)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);

            // Загрузка пикселей в GPU память
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w.get(0), h.get(0), 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
            // Создаём mipmaps (необязательно, но на будущее для масштабирования)
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            // Освобождаем память, занятую изображением в RAM
            STBImage.stbi_image_free(image);

            // Возвращаем объект текстуры с параметрами
            return new Texture(textureId, w.get(0), h.get(0));
        }
    }

}



