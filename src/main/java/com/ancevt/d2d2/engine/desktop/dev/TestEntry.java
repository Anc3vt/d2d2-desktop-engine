package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.D2D2Config;
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.lifecycle.D2D2Application;
import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.Sprite;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.time.Timer;

public class TestEntry implements D2D2Application {

    public static void main(String[] args) {
        D2D2.init(
                new TestEntry(),
                new D2D2Config()
                        .fromArgs(args)
                        .noScaleMode(true)
                        .engine(DesktopEngine.class)
        );
    }

    @Override
    public void start(Stage stage) {
        Sprite sprite = Assets.loadTexture("test2.png").createSprite();
        stage.addChild(sprite, 100, 100);

        sprite.setAlpha(0.5f);

        System.out.println(sprite.getAlpha());
        sprite.setColor(Color.YELLOW);

        sprite.onPostFrame(e -> sprite.moveX(1));

        Timer timer = Timer.setInterval(100, t -> {
            sprite.moveY(1);
        });

//        new Thread(() -> {
//            while(true) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//
//                sprite.moveX(1f);
//            }
//        }).start();


        Sprite sprite2 = Assets.loadTexture("test2.png").createTextureRegion(0, 0, 128, 128).createSprite();


        stage.addChild(sprite2);

        System.out.println("done");
    }
}
