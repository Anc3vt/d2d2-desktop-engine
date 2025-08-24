package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.lifecycle.Application;
import com.ancevt.d2d2.scene.Bitmap;
import com.ancevt.d2d2.scene.Color;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestBitmapGalaxyV2 implements Application {

    public static void main(String[] args) {
        D2D2.init(new TestBitmapGalaxyV2());
    }

    @Override
    public void start(ApplicationContext applicationContext) {
        final int width = 400, height = 400;
        final int cx = width / 2, cy = height / 2;

        Bitmap bitmap = Bitmap.create(width, height);
        Random rnd = new Random();

        bitmap.setScale(2f);

        List<Particle> particles = new ArrayList<>();

        // генерим частицы с базовым цветом из твоего Color
        for (int i = 0; i < 10_000; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double radius = 20 + rnd.nextDouble() * (Math.min(width, height) * 0.45);
            int baseRgb = Color.createVisibleRandomColor().getValue(); // 0xRRGGBB
            float[] hsv = rgbToHsv(baseRgb);

            // лёгкая разница скоростей и оттенков
            double angVel = (rnd.nextDouble() - 0.5) * 0.003;          // медленное вращение
            float hueShift = (float) (rnd.nextDouble() * 360.0);       // смещение тона
            float pulse = 0.3f + rnd.nextFloat() * 0.7f;               // глубина пульса яркости

            particles.add(new Particle(angle, radius, angVel, hsv[0], hsv[1], hsv[2], hueShift, pulse));
        }

        applicationContext.stage().onTick(e -> {
            bitmap.clear();

            float t = (System.currentTimeMillis() % 100000) / 1000f;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgba = bitmap.getPixel(x, y);
                    int a = rgba & 0xFF;
                    a *= 0.85; // затухание альфы (0.0–1.0)
                    if (a < 5) a = 0;
                    rgba = (rgba & 0xFFFFFF00) | a;
                    bitmap.setPixel(x, y, rgba);
                }
            }

            for (Particle p : particles) {
                // обновляем угол
                p.angle += p.angVel;

                // лёгкая пульсация радиуса
                double r = p.radius + Math.sin(t * 0.9 + p.radius * 0.03) * 4.0;

                int x = (int) (cx + Math.cos(p.angle) * r);
                int y = (int) (cy + Math.sin(p.angle) * r);

                if ((x | y) >= 0 && x < width && y < height) {
                    // плавный перелив цвета: вращаем H, модулируем V
                    float h = wrapHue(p.baseH + p.hueShift + t * 22f);
                    float s = clamp01(p.baseS);
                    float v = clamp01(0.4f + p.baseV * 0.6f + (float) Math.sin(t * 2.3 + p.radius * 0.02) * 0.15f * p.pulse);

                    int rgb = hsvToRgb(h, s, v);     // 0xRRGGBB
                    int rgba = (rgb << 8) | 0xFF;   // 0xRRGGBBFF — как ты и рисовал

                    bitmap.setPixel(x, y, rgba);
                }
            }
        });

        applicationContext.stage().addChild(bitmap);
        applicationContext.stage().addChild(new FpsMeter());
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Particle {
        public double angle;
        public double radius;
        public double angVel;

        // базовый HSV, вытащенный из твоего Color.getValue()
        public float baseH, baseS, baseV;

        // параметры анимации
        public float hueShift; // градусов
        public float pulse;    // интенсивность пульса яркости
    }

    // -------- utils: чистый матан, без методов Color --------

    private static float[] rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb) & 0xFF;

        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;

        float h;
        if (d == 0) h = 0;
        else if (max == rf) h = 60f * (((gf - bf) / d) % 6f);
        else if (max == gf) h = 60f * (((bf - rf) / d) + 2f);
        else h = 60f * (((rf - gf) / d) + 4f);

        if (h < 0) h += 360f;

        float s = (max == 0f) ? 0f : (d / max);
        float v = max;

        return new float[]{h, s, v};
    }

    private static int hsvToRgb(float h, float s, float v) {
        h = wrapHue(h);
        s = clamp01(s);
        v = clamp01(v);

        float c = v * s;
        float x = c * (1 - Math.abs(((h / 60f) % 2) - 1));
        float m = v - c;

        float rf, gf, bf;
        if (h < 60) {
            rf = c;
            gf = x;
            bf = 0;
        } else if (h < 120) {
            rf = x;
            gf = c;
            bf = 0;
        } else if (h < 180) {
            rf = 0;
            gf = c;
            bf = x;
        } else if (h < 240) {
            rf = 0;
            gf = x;
            bf = c;
        } else if (h < 300) {
            rf = x;
            gf = 0;
            bf = c;
        } else {
            rf = c;
            gf = 0;
            bf = x;
        }

        int r = (int) ((rf + m) * 255 + 0.5f);
        int g = (int) ((gf + m) * 255 + 0.5f);
        int b = (int) ((bf + m) * 255 + 0.5f);

        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static float wrapHue(float h) {
        h %= 360f;
        if (h < 0) h += 360f;
        return h;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
