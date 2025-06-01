package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.event.CommonEvent;
import com.ancevt.d2d2.event.StageEvent;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.shape.LineBatch;
import com.ancevt.d2d2.scene.shape.RectangleShape;
import com.ancevt.d2d2.scene.text.BitmapCharInfo;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;
import com.ancevt.d2d2.time.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class DesktopRenderer implements Renderer {

    private final DesktopEngine engine;

    boolean running = true;

    public static int batchSize = 20000;

    private static final int FLOATS_PER_VERTEX = 8; // x, y, u, v, r, g, b, a
    private static final int VERTICES_PER_SPRITE = 4;
    private static final int INDICES_PER_SPRITE = 6;

    private int vaoId;
    private int vboId;
    private int eboId;
    private int shaderProgram;

    private int uProjectionLocation;
    private int uTextureLocation;

    private Texture whiteTexture;

    private int lineVaoId;
    private int lineVboId;
    private static final int FLOATS_PER_LINE_VERTEX = 8;
    private static final int MAX_LINES = 8192;
    private final FloatBuffer lineBuffer = BufferUtils.createFloatBuffer(MAX_LINES * 2 * FLOATS_PER_LINE_VERTEX);

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
    private final float[] projectionMatrix = new float[16];

    @Getter
    private int actualFps;

    @Getter
    @Setter
    private int frameRate = 60;

    @Override
    public void init(long windowId) {
        String vertexShaderSrc =
                "#version 330 core\n" +
                        "layout(location = 0) in vec2 aPos;\n" +
                        "layout(location = 1) in vec2 aTexCoord;\n" +
                        "layout(location = 2) in vec4 aColor;\n" +
                        "uniform mat4 uProjection;\n" +
                        "out vec2 vTexCoord;\n" +
                        "out vec4 vColor;\n" +
                        "void main() {\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "    vColor = aColor;\n" +
                        "    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);\n" +
                        "}\n";

        String fragmentShaderSrc =
                "#version 330 core\n" +
                        "in vec2 vTexCoord;\n" +
                        "in vec4 vColor;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main() {\n" +
                        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
                        "    FragColor = texColor * vColor;\n" +
                        "}\n";

        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexShaderSrc);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSrc);

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("Ошибка линковки шейдера: " + GL20.glGetProgramInfoLog(shaderProgram));
        }
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        uProjectionLocation = GL20.glGetUniformLocation(shaderProgram, "uProjection");
        uTextureLocation = GL20.glGetUniformLocation(shaderProgram, "uTexture");

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                batchSize * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX * Float.BYTES,
                GL15.GL_DYNAMIC_DRAW);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        int[] indices = new int[batchSize * INDICES_PER_SPRITE];
        for (int i = 0; i < batchSize; i++) {
            int offset = i * VERTICES_PER_SPRITE;
            int indexOffset = i * INDICES_PER_SPRITE;
            indices[indexOffset] = offset;
            indices[indexOffset + 1] = offset + 1;
            indices[indexOffset + 2] = offset + 2;
            indices[indexOffset + 3] = offset + 2;
            indices[indexOffset + 4] = offset + 3;
            indices[indexOffset + 5] = offset;
        }
        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.length);
        indicesBuffer.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        // Атрибуты VAO
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 4 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1i(uTextureLocation, 0);
        GL20.glUseProgram(0);

        whiteTexture = createWhiteTexture();


        lineVaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(lineVaoId);

        lineVboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                MAX_LINES * 2 * FLOATS_PER_LINE_VERTEX * Float.BYTES,
                GL15.GL_DYNAMIC_DRAW);

// Атрибуты: x, y, u, v, r, g, b, a
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, FLOATS_PER_LINE_VERTEX * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, FLOATS_PER_LINE_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, FLOATS_PER_LINE_VERTEX * Float.BYTES, 4 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);


    }

    public void setProjection(int width, int height) {
        float l = 0;
        float r = width;
        float t = 0;
        float b = height;
        float n = -1;
        float f = 1;
        for (int i = 0; i < 16; i++) projectionMatrix[i] = 0.0f;
        projectionMatrix[0] = 2.0f / (r - l);
        projectionMatrix[5] = 2.0f / (t - b);
        projectionMatrix[10] = -2.0f / (f - n);
        projectionMatrix[12] = -(r + l) / (r - l);
        projectionMatrix[13] = -(t + b) / (t - b);
        projectionMatrix[14] = -(f + n) / (f - n);
        projectionMatrix[15] = 1.0f;
    }

    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Shader compilation error: " + GL20.glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    @Override
    public void reshape() {
        GL11.glViewport(0, 0, engine.getCanvasWidth(), engine.getCanvasHeight());
        setProjection(engine.getCanvasWidth(), engine.getCanvasHeight());

        var s = engine.getStage();
        s.dispatchEvent(CommonEvent.Resize.create(engine.getCanvasWidth(), engine.getCanvasHeight()));
        s.setSize(engine.getCanvasWidth(), engine.getCanvasHeight());

    }

    public static void setNearestFilter(int textureId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    @Override
    public void renderFrame() {
        List<ShapeDrawInfo> shapesToDraw = new ArrayList<>();
        List<LineDrawInfo> linesToDraw = new ArrayList<>();

        Stage stage = engine.getStage();

        Color backgroundColor = stage.getBackgroundColor();
        GL11.glClearColor(
                backgroundColor.getR() / 255f,
                backgroundColor.getG() / 255f,
                backgroundColor.getB() / 255f,
                1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        List<SpriteDrawInfo> spritesToDraw = new ArrayList<>();
        List<BitmapTextDrawInfo> bitmapTextsToDraw = new ArrayList<>();

        collectNodes(stage, 1f, 0f, 0f, 0f, 1f, 0f, 1f, spritesToDraw, bitmapTextsToDraw, shapesToDraw, linesToDraw);

        spritesToDraw.sort(Comparator.comparingInt(info -> info.zOrder));

        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(vaoId);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        vertexBuffer.clear();

        int spritesInBatch = 0;
        int currentTextureId = -1;

        // ==== SPRITES ====
        for (SpriteDrawInfo info : spritesToDraw) {
            int texId = info.textureId;
            if (currentTextureId == -1 || texId != currentTextureId || spritesInBatch >= batchSize) {
                if (spritesInBatch > 0) flushBatch(spritesInBatch);
                spritesInBatch = 0;
                currentTextureId = texId;
                setNearestFilter(currentTextureId);
            }

            float r = 1f, g = 1f, b = 1f, alpha = info.alpha;
            if (info.node instanceof Colored colored) {
                Color c = colored.getColor();
                r = c.getR() / 255f;
                g = c.getG() / 255f;
                b = c.getB() / 255f;
            }

            float x = info.x, y = info.y;
            float a = info.a, bb = info.b, d = info.d, e = info.e;
            float w = info.width, h = info.height;

            float x0 = x, y0 = y;
            float x1 = a * w + x, y1 = d * w + y;
            float x2 = a * w + bb * h + x, y2 = d * w + e * h + y;
            float x3 = bb * h + x, y3 = e * h + y;

            float u0 = 0f, v0 = 1f;
            float u1 = 1f, v1 = 1f;
            float u2 = 1f, v2 = 0f;
            float u3 = 0f, v3 = 0f;

            if (info.node instanceof Sprite sprite && sprite.getTextureRegion() != null) {
                TextureRegion region = sprite.getTextureRegion();
                Texture texture = region.getTexture();

                float texW = texture.getWidth();
                float texH = texture.getHeight();

                float regionX = region.getX();
                float regionY = region.getY();
                float regionW = region.getWidth();
                float regionH = region.getHeight();

                float uLeft = regionX / texW;
                float uRight = (regionX + regionW) / texW;

                float vTop = (texH - regionY - regionH) / texH;
                float vBottom = (texH - regionY) / texH;

                u0 = uLeft;
                v0 = vBottom;
                u1 = uRight;
                v1 = vBottom;
                u2 = uRight;
                v2 = vTop;
                u3 = uLeft;
                v3 = vTop;
            }

            int baseIndex = spritesInBatch * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX;

            float[] data = {
                    x0, y0, u0, v0, r, g, b, alpha,
                    x1, y1, u1, v1, r, g, b, alpha,
                    x2, y2, u2, v2, r, g, b, alpha,
                    x3, y3, u3, v3, r, g, b, alpha
            };
            vertexBuffer.position(baseIndex);
            vertexBuffer.put(data);

            spritesInBatch++;
        }

        // ==== BITMAP TEXT ====
        for (BitmapTextDrawInfo info : bitmapTextsToDraw) {
            BitmapText text = info.bitmapText;
            if (text.isEmpty()) continue;

            BitmapFont font = text.getBitmapFont();
            Texture texture = font.getTexture();
            int texId = texture.getId();

            if (currentTextureId != texId) {
                if (spritesInBatch > 0) flushBatch(spritesInBatch);
                spritesInBatch = 0;
                currentTextureId = texId;
                setNearestFilter(currentTextureId);
            }

            float texW = texture.getWidth();
            float texH = texture.getHeight();
            double tf = text.getTextureBleedingFix();
            double vf = text.getVertexBleedingFix();

            String content = text.getPlainText();
            float cursorX = 0f;
            float cursorY = 0f;

            float spacing = text.getSpacing();
            float lineSpacing = text.getLineSpacing();
            float scaleX = text.getScaleX();
            float scaleY = text.getScaleY();

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);

                if (c == '\n') {
                    cursorX = 0;
                    cursorY += font.getZeroCharHeight() + lineSpacing;
                    continue;
                }

                BitmapCharInfo charInfo = font.getCharInfo(c);
                if (charInfo == null) continue;

                float charW = charInfo.width();
                float charH = charInfo.height();

                float u0 = charInfo.x() / texW;
                float v0 = (texH - charInfo.y()) / texH;
                float u1 = (charInfo.x() + charW) / texW;
                float v1 = (texH - (charInfo.y() + charH)) / texH;

                float vx = info.x + cursorX * scaleX;
                float vy = info.y + cursorY * scaleY;
                float vw = charW * scaleX;
                float vh = charH * scaleY;

                float r = 1f, g = 1f, b = 1f, a = info.alpha;
                if (text.isMulticolor()) {
                    Color color = text.getColorTextData().getColoredLetter(i).getColor();
                    r = color.getR() / 255f;
                    g = color.getG() / 255f;
                    b = color.getB() / 255f;
                } else {
                    Color color = text.getColor();
                    r = color.getR() / 255f;
                    g = color.getG() / 255f;
                    b = color.getB() / 255f;
                }

                if (spritesInBatch >= batchSize) {
                    flushBatch(spritesInBatch);
                    spritesInBatch = 0;
                }

                int base = spritesInBatch * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX;
                vertexBuffer.position(base);
                vertexBuffer.put(new float[]{
                        vx - (float) vf, vy - (float) vf, u0 + (float) tf, v0 - (float) tf, r, g, b, a,
                        vx + vw + (float) vf, vy - (float) vf, u1 - (float) tf, v0 - (float) tf, r, g, b, a,
                        vx + vw + (float) vf, vy + vh + (float) vf, u1 - (float) tf, v1 + (float) tf, r, g, b, a,
                        vx - (float) vf, vy + vh + (float) vf, u0 + (float) tf, v1 + (float) tf, r, g, b, a
                });

                spritesInBatch++;
                cursorX += charW + spacing;
            }
        }

        // ==== RECTANGLES ====
        for (ShapeDrawInfo info : shapesToDraw) {
            RectangleShape shape = info.shape;

            float r = 1f, g = 1f, b = 1f, a = info.alpha;
            Color color = shape.getColor();
            if (color != null) {
                r = color.getR() / 255f;
                g = color.getG() / 255f;
                b = color.getB() / 255f;
            }

            if (currentTextureId != whiteTexture.getId()) {
                if (spritesInBatch > 0) flushBatch(spritesInBatch);
                spritesInBatch = 0;
                currentTextureId = whiteTexture.getId();
                setNearestFilter(currentTextureId);
            }

            float w = shape.getWidth();
            float h = shape.getHeight();
            float x = info.x;
            float y = info.y;
            float a_ = info.a;
            float b_ = info.b;
            float d_ = info.d;
            float e_ = info.e;

            float x0 = x, y0 = y;
            float x1 = a_ * w + x, y1 = d_ * w + y;
            float x2 = a_ * w + b_ * h + x, y2 = d_ * w + e_ * h + y;
            float x3 = b_ * h + x, y3 = e_ * h + y;

            int baseIndex = spritesInBatch * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX;
            float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;

            vertexBuffer.position(baseIndex);
            vertexBuffer.put(new float[]{
                    x0, y0, u0, v0, r, g, b, a,
                    x1, y1, u1, v0, r, g, b, a,
                    x2, y2, u1, v1, r, g, b, a,
                    x3, y3, u0, v1, r, g, b, a
            });

            spritesInBatch++;
        }

        // === LINE BATCH ===
        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(uProjectionLocation, false, projectionMatrix);
        GL30.glBindVertexArray(lineVaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVboId);
        lineBuffer.clear();

        int totalLineVertices = 0;

        for (LineDrawInfo info : linesToDraw) {
            LineBatch lineBatch = info.lineBatch;
            if (lineBatch.getLines().isEmpty()) continue;

            float r = 1f, g = 1f, b = 1f, a = info.alpha;
            Color color = lineBatch.getColor();
            if (color != null) {
                r = color.getR() / 255f;
                g = color.getG() / 255f;
                b = color.getB() / 255f;
            }

            for (LineBatch.Line line : lineBatch.getLines()) {
                float x1 = line.vertexA.x;
                float y1 = line.vertexA.y;
                float x2 = line.vertexB.x;
                float y2 = line.vertexB.y;

                float a_ = info.a, b_ = info.b, c_ = info.c;
                float d_ = info.d, e_ = info.e, f_ = info.f;

                float x1t = a_ * x1 + b_ * y1 + c_;
                float y1t = d_ * x1 + e_ * y1 + f_;
                float x2t = a_ * x2 + b_ * y2 + c_;
                float y2t = d_ * x2 + e_ * y2 + f_;

                lineBuffer.put(new float[]{x1t, y1t, 0f, 0f, r, g, b, a});
                lineBuffer.put(new float[]{x2t, y2t, 0f, 0f, r, g, b, a});
                totalLineVertices += 2;
            }
        }

        if (totalLineVertices > 0) {
            lineBuffer.flip();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, whiteTexture.getId());
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, lineBuffer);
            GL11.glDrawArrays(GL11.GL_LINES, 0, totalLineVertices);
        }


        ////-----

        if (spritesInBatch > 0) flushBatch(spritesInBatch);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);


    }

    private static void collectNodes(Node node,
                                     float parentA, float parentB, float parentC,
                                     float parentD, float parentE, float parentF,
                                     float parentAlpha,
                                     List<SpriteDrawInfo> outSprites,
                                     List<BitmapTextDrawInfo> outTexts,
                                     List<ShapeDrawInfo> outShapes,
                                     List<LineDrawInfo> outLines) {

        float x = node.getX(), y = node.getY();
        float scaleX = node.getScaleX(), scaleY = node.getScaleY();
        float rad = (float) Math.toRadians(node.getRotation());
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);

        float a2 = cos * scaleX, b2 = -sin * scaleY, d2 = sin * scaleX, e2 = cos * scaleY;
        float c2 = x, f2 = y;

        float a = parentA * a2 + parentB * d2;
        float b = parentA * b2 + parentB * e2;
        float c = parentA * c2 + parentB * f2 + parentC;
        float d = parentD * a2 + parentE * d2;
        float e = parentD * b2 + parentE * e2;
        float f = parentD * c2 + parentE * f2 + parentF;

        float effectiveAlpha = parentAlpha * node.getAlpha();

        if (node instanceof Sprite sprite) {
            SpriteDrawInfo info = new SpriteDrawInfo();
            info.a = a;
            info.b = b;
            info.c = c;
            info.d = d;
            info.e = e;
            info.f = f;
            info.x = c;
            info.y = f;
            info.width = sprite.getWidth();
            info.height = sprite.getHeight();
            info.textureId = getSpriteTextureId(sprite);
            info.zOrder = sprite.getGlobalZOrderIndex();
            info.node = sprite;
            info.alpha = effectiveAlpha;
            outSprites.add(info);

        } else if (node instanceof BitmapText btx) {
            if (btx.isCacheAsSprite()) {
                collectNodes(btx.cachedSprite(), a, b, c, d, e, f, effectiveAlpha, outSprites, outTexts, outShapes, outLines);
            } else {
                BitmapTextDrawInfo info = new BitmapTextDrawInfo();
                info.bitmapText = btx;
                info.a = a;
                info.b = b;
                info.c = c;
                info.d = d;
                info.e = e;
                info.f = f;
                info.x = c;
                info.y = f;
                info.alpha = effectiveAlpha;
                outTexts.add(info);
            }
        } else if (node instanceof RectangleShape rect) {
            ShapeDrawInfo info = new ShapeDrawInfo();
            info.shape = rect;
            info.a = a;
            info.b = b;
            info.c = c;
            info.d = d;
            info.e = e;
            info.f = f;
            info.x = c;
            info.y = f;
            info.alpha = effectiveAlpha;
            outShapes.add(info);
        } else if (node instanceof LineBatch lineBatch) {
            LineDrawInfo info = new LineDrawInfo();
            info.lineBatch = lineBatch;
            info.a = a;
            info.b = b;
            info.c = c;
            info.d = d;
            info.e = e;
            info.f = f;
            info.x = c;
            info.y = f;
            info.alpha = effectiveAlpha;
            outLines.add(info);
        }


        if (node instanceof Group group) {
            for (Node child : group.children().toList()) {
                collectNodes(child, a, b, c, d, e, f, effectiveAlpha, outSprites, outTexts, outShapes, outLines);
            }
        }
    }


    private void flushBatch(int spriteCount) {
        if (spriteCount <= 0) return;

        // 💡 Убедимся, что привязаны все буферы
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        // 🧠 Установка лимита и позиции перед заливкой данных
        vertexBuffer.limit(spriteCount * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
        vertexBuffer.position(0);

        // 🚀 Отправка данных в VBO
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // ✅ Отрисовка индексов
        GL11.glDrawElements(GL11.GL_TRIANGLES, spriteCount * INDICES_PER_SPRITE, GL11.GL_UNSIGNED_INT, 0);

        // 🧹 Очистка буфера для следующего кадра
        vertexBuffer.clear();
    }

    private static int getSpriteTextureId(Sprite sprite) {
        if (sprite != null && sprite.getTextureRegion() != null) {
            return sprite.getTextureRegion().getTexture().getId();
        }
        return -1;
    }

    public void startRenderLoop() {
        long windowId = CanvasControl.getWindowId();

        Stage stage = engine.getStage();

        long lastTime = System.nanoTime();
        long accumulator = 0L;
        long lastRenderTime = System.nanoTime();

        int frames = 0;
        long fpsTimer = System.currentTimeMillis();

        while (!GLFW.glfwWindowShouldClose(windowId) && running) {
            final long tickInterval = 1_000_000_000L / (frameRate + 10);
            final long frameInterval = 1_000_000_000L / (frameRate + 10);

            long now = System.nanoTime();
            long delta = now - lastTime;
            lastTime = now;
            accumulator += delta;

            // ✅ Tick логики
            while (accumulator >= tickInterval) {
                Timer.processTimers();
                stage.dispatchEvent(StageEvent.Tick.create());
                accumulator -= tickInterval;
            }

            // ✅ Ограничим рендер частотой frameRate
            if (now - lastRenderTime >= frameInterval) {
                stage.dispatchEvent(StageEvent.PreFrame.create());
                renderFrame();
                stage.dispatchEvent(StageEvent.PostFrame.create());
                GLFW.glfwSwapBuffers(windowId);
                lastRenderTime = now;
                frames++;
            }

            // ✅ Обрабатываем события независимо
            GLFW.glfwPollEvents();

            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                actualFps = frames;
                frames = 0;
                fpsTimer += 1000;
            }

            // 💡 Чтобы не сжигать CPU — делаем sleep на пару миллисекунд
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        GLFW.glfwTerminate();
    }

    private static Texture createWhiteTexture() {
        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        buffer.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255).flip(); // RGBA white

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        return new Texture(texId, 1, 1);
    }

    private static class SpriteDrawInfo {
        float a, b, c, d, e, f;
        float x, y;
        float width, height;
        int textureId;
        int zOrder;
        Node node;
        float alpha;
    }

    private static class BitmapTextDrawInfo {
        BitmapText bitmapText;
        float x, y;
        float a, b, c, d, e, f;
        float alpha;
    }

    private static class ShapeDrawInfo {
        RectangleShape shape;
        float a, b, c, d, e, f;
        float x, y;
        float alpha;
    }

    private static class LineDrawInfo {
        LineBatch lineBatch;
        float a, b, c, d, e, f;
        float x, y;
        float alpha;
    }


}
