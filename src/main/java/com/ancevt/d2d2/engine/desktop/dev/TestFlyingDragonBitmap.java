package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.lifecycle.Application;
import com.ancevt.d2d2.scene.Bitmap;

public class TestFlyingDragonBitmap implements Application {

    public static void main(String[] args) {
        D2D2.init(new TestFlyingDragonBitmap());
    }

    @Override
    public void start(ApplicationContext applicationContext) {
        final int width = 300, height = 150;
        final int dragonWidth = 48, dragonHeight = 24;

        Bitmap bitmap = Bitmap.create(width, height);
        bitmap.setScale(3f);

        applicationContext.stage().addChild(bitmap);
        applicationContext.stage().addChild(new FpsMeter());

        applicationContext.stage().onTick(e -> {
            bitmap.clear();

            float t = (System.currentTimeMillis() % 100000) / 1000f;

            // Движение по синусоиде
            float dragonX = (float) ((t * 30) % (width + dragonWidth)) - dragonWidth;
            float dragonY = (float) (height / 2 + Math.sin(t * 2.0) * 20);

            drawDragon(bitmap, (int) dragonX, (int) dragonY);
        });

        applicationContext.stage().onKeyDown(e -> {
            // e.getKeyCode(): int
        });

        applicationContext.stage().onKeyUp(e -> {
            // e.getKeyCode(): int
        });
    }

    private void drawDragon(Bitmap bitmap, int x0, int y0) {
        // Примитивное пиксельное изображение "дракона" (да, это больно)
        final String[] dragonPixels = new String[]{
                "........RRRRRR........",
                ".......RRRRRRRR.......",
                "......RRRYYYYRRR......",
                "......RYYYYYYYYR......",
                ".....RYYGGGGYYYR......",
                ".....RYYGGGGYYYR......",
                "......RYYYYYYRR.......",
                ".......RRRRRRR........",
                "........R..R..........",
                ".......R....R.........",
                "......R......R........",
        };

        for (int y = 0; y < dragonPixels.length; y++) {
            String row = dragonPixels[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == '.') continue;

                int color = switch (c) {
                    case 'R' -> 0xFF0000;
                    case 'Y' -> 0xFFFF00;
                    case 'G' -> 0x00FF00;
                    default -> 0x000000;
                };

                bitmap.setPixel(x0 + x, y0 + y, (color << 8) | 0xFF);
            }
        }
    }
}
