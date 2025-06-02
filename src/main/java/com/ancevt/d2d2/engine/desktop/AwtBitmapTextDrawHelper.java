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

package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.text.BitmapCharInfo;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;


public class AwtBitmapTextDrawHelper {

    static void draw(BitmapText bitmapText,
                     float alpha,
                     float scaleX,
                     float scaleY,
                     DrawCharFunction drawCharFunction,
                     ApplyColorFunction applyColorFunction) {

        BitmapFont bitmapFont = bitmapText.getBitmapFont();
        Texture texture = bitmapFont.getTexture();

        int textureWidth = texture.getWidth();
        int textureHeight = texture.getHeight();

        float lineSpacing = bitmapText.getLineSpacing();
        float spacing = bitmapText.getSpacing();

        float boundWidth = bitmapText.getWidth() * scaleX;
        float boundHeight = bitmapText.getHeight() * scaleY;

        float drawX = 0;
        float drawY = bitmapFont.getPaddingTop() * scaleY;

        double textureBleedingFix = bitmapText.getTextureBleedingFix();
        double vertexBleedingFix = bitmapText.getVertexBleedingFix();

        boolean wordWrap = bitmapText.isWordWrap();
        boolean multicolor = bitmapText.isMulticolor();

        String text = bitmapText.getText();

        float nextWordWidth;

        BitmapText.ColorTextData colorTextData = multicolor ? bitmapText.getColorTextData() : null;

        for (int i = 0; multicolor ? i < colorTextData.length() : i < text.length(); i++) {
            BitmapText.ColorTextData.Letter letter = null;

            if (multicolor) {
                letter = colorTextData.getColoredLetter(i);

                Color letterColor = letter.getColor();

                if (applyColorFunction != null) {
                    applyColorFunction.applyColor(
                            letterColor.getR() / 255f,
                            letterColor.getG() / 255f,
                            letterColor.getB() / 255f,
                            alpha
                    );
                }
            }

            char c = multicolor ? letter.getCharacter() : text.charAt(i);

            if (wordWrap && isSpecialCharacter(c)) {
                nextWordWidth = getNextWordWidth(bitmapText, i, scaleX);
            } else {
                nextWordWidth = 0f;
            }

            BitmapCharInfo charInfo = bitmapFont.getCharInfo(c);

            if (charInfo == null) continue;

            if (charInfo.character() == ' ') {
                if(bitmapFont.isMonospaced()) {
                    drawX += bitmapFont.getZeroCharWidth();
                } else {
                    drawX += meterStringWidth(bitmapText, " ");
                }

                continue;
            }

            float charWidth = charInfo.width();
            float charHeight = charInfo.height();

            if (c == '\n' || wordWrap && (boundWidth != 0 && drawX >= boundWidth - nextWordWidth - charWidth / 1.5f * scaleX)) {
                drawX = 0;
                drawY += (charHeight + lineSpacing) * scaleY;

                if (boundHeight != 0 && drawY > boundHeight - charHeight) {
                    break;
                }

                if (nextWordWidth > 0) {
                    continue;
                }
            }

            if (!wordWrap && drawX >= boundWidth - charWidth / 1.5f) {
                continue;
            }

            drawCharFunction.drawChar(
                    texture,
                    c,
                    letter, // null if not multicolor
                    drawX,
                    (drawY + scaleY * charHeight),
                    textureWidth,
                    textureHeight,
                    charInfo,
                    scaleX,
                    scaleY,
                    textureBleedingFix,
                    vertexBleedingFix
            );

            drawX += (charWidth + (c != '\n' ? spacing : 0)) * scaleX;
        }


    }

    private static float getNextWordWidth(BitmapText bitmapText, int charIndex, float scaleX) {
        String nextWord = getNextWord(bitmapText.getPlainText(), charIndex);
        if (nextWord.length() > 0) {
            char firstChar = nextWord.charAt(0);
            if (!Character.isLetterOrDigit(firstChar) && firstChar != '_') return 0f;
        }
        return meterStringWidth(bitmapText, nextWord) * scaleX;
    }

    public static String getNextWord(String text, int charIndexFrom) {
        StringBuilder word = new StringBuilder();
        boolean inWord = false;

        // Начинаем поиск слова с указанного индекса
        for (int i = charIndexFrom; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Проверяем, является ли текущий символ допустимым для слова
            if (isWordCharacter(ch)) {
                word.append(ch);
                inWord = true;
            } else {
                // Если уже начали собирать слово и текущий символ не подходит, завершаем сбор слова
                if (inWord) {
                    break;
                }
                // Если еще не начали собирать слово, продолжаем пропускать символы
                continue;
            }
        }

        return word.toString();
    }

    // Метод для проверки символа на принадлежность к допустимым символам слова
    private static boolean isWordCharacter(char ch) {
        return Character.isLetterOrDigit(ch) ||
                ch == '!' || ch == '_' || ch == '.' ||
                ch == ':' || ch == ';' || ch == ',';
    }

    public static float meterStringWidth(BitmapText bitmapText, String string) {
        float result = 0f;

        BitmapFont bitmapFont = bitmapText.getBitmapFont();

        for (char c : string.toCharArray()) {
            BitmapCharInfo charInfo = bitmapFont.getCharInfo(c);
            result += charInfo.width() + bitmapText.getSpacing();
        }

        return result;
    }

    private static boolean isSpecialCharacter(char ch) {
        return !Character.isLetterOrDigit(ch) && ch != '_';
    }


    public static Texture bitmapTextToTexture(BitmapText bitmapText) {
        int width = (int) bitmapText.getWidth();
        int height = (int) bitmapText.getHeight();

        if(width == 0) {
            System.err.println("possible not optimal text \"cache as sprite\" call");
            width = 10;
        }
        if(height == 0) {
            System.err.println("possible not optimal text \"cache as sprite\" call");
            height = 10;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        AwtBitmapTextDrawHelper.draw(
                bitmapText,
                bitmapText.getAlpha(),
                bitmapText.getScaleX(),
                bitmapText.getScaleY(),
                (texture, c, letter, drawX, drawY, textureAtlasWidth, textureAtlasHeight, charInfo, scX, scY, textureBleedingFix, vertexBleedingFix) -> {

                    if (c != '\n') {
                        int charX = charInfo.x();
                        int charY = charInfo.y();

                        int offsetX = 0;
                        int offsetY = 0;

                        if (charX < 0) {
                            offsetX = -charX;
                            charX = 0;
                        }
                        if (charY < 0) {
                            offsetY = -charY;
                            charY = 0;
                        }

                        BufferedImage charImage = textureRegionToImage(
                                texture, charX, charY, charInfo.width(), charInfo.height()
                        );

                        charImage = copyImage(charImage);

                        com.ancevt.d2d2.scene.Color letterColor = letter == null ? bitmapText.getColor() : letter.getColor();

                        applyColorFilter(
                                charImage,
                                letterColor.getR(),
                                letterColor.getG(),
                                letterColor.getB()
                        );

                        g.drawImage(charImage, (int) drawX + offsetX, (int) drawY - charInfo.height() + offsetY, null);
                    }

                },
                null
        );


        final Texture texture = createTextureFromBufferedImage(image);
        D2D2.textureManager().addTextureRegion("_texture_text_" + texture.getId(), texture.createTextureRegion());
        return texture;
    }

    private static void applyColorFilter(BufferedImage image, int redPercent, int greenPercent, int bluePercent) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                pixel = (alpha << 24) | (redPercent * red / 255 << 16) | (greenPercent * green / 255 << 8) | (bluePercent * blue / 255);

                image.setRGB(x, y, pixel);
            }
        }
    }

    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    public static Texture createTextureFromBufferedImage(BufferedImage image) {
        try {
            InputStream inputStream = bufferedImageToPngInputStream(image);
            return D2D2.textureManager().loadTexture(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream bufferedImageToPngInputStream(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);  // Запись в PNG
        baos.flush();
        return new ByteArrayInputStream(baos.toByteArray()); // Обратный поток
    }


    private static BufferedImage textureRegionToImage(Texture texture, int x, int y, int width, int height) {
        Map<Integer, BufferedImage> map = ((DesktopTextureEngine) D2D2.textureManager().getTextureEngine()).getBufferedImageMap();
        BufferedImage bufferedImage = map.computeIfAbsent(texture.getId(), id -> {
            throw new IllegalStateException("texture not found: " + id);
        });
        return bufferedImage.getSubimage(x, y, width, height);
    }

    @FunctionalInterface
    interface DrawCharFunction {

        void drawChar(
                Texture texture,
                char c,
                BitmapText.ColorTextData.Letter letter,
                float x,
                float y,
                int textureWidth,
                int textureHeight,
                BitmapCharInfo charInfo,
                float scX,
                float scY,
                double textureBleedingFix,
                double vertexBleedingFix);
    }

    @FunctionalInterface
    interface ApplyColorFunction {
        void applyColor(float r, float g, float b, float alpha);
    }
}
