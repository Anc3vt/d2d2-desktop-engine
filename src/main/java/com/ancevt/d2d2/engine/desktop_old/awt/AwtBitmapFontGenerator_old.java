package com.ancevt.d2d2.engine.desktop_old.awt;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.lifecycle.D2D2PropertyConstants;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.FontBuilder;
import com.ancevt.d2d2.scene.text.FractionalMetrics;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.ancevt.d2d2.D2D2.log;

public class AwtBitmapFontGenerator_old {

    @SneakyThrows
    public static BitmapFont generate(FontBuilder builder) {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        InputStream inputStream = builder.getInputStream() != null ?
                builder.getInputStream() : new FileInputStream(builder.getFilePath().toFile());

        Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
        String fontName = font.getName();
        ge.registerFont(font);

        boolean bold = builder.isBold();
        boolean italic = builder.isItalic();
        int fontSize = builder.getFontSize();
        int fontStyle = Font.PLAIN | (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);

        font = new Font(fontName, fontStyle, fontSize);

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
            log.info(AwtBitmapFontGenerator_old.class, "BMF written %s/%s".formatted(destinationPath, fileName));
        }

        return D2D2.bitmapFontManager().loadBitmapFont(
                new ByteArrayInputStream(charsDataBytes),
                new ByteArrayInputStream(pngDataBytes),
                builder.getName()
        );
    }

    private static Size computeSize(Font font, String string, FontBuilder builder) {
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

    @RequiredArgsConstructor
    @Getter
    @ToString
    private static class Size {
        private final int w;
        private final int h;
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
