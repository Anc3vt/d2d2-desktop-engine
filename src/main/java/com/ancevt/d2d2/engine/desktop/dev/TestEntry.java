package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.D2D2Config;
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.lifecycle.D2D2Application;
import com.ancevt.d2d2.scene.Sprite;
import com.ancevt.d2d2.scene.Stage;

public class TestEntry implements D2D2Application {

    public static void main(String[] args) {
        D2D2.init(
                new TestEntry(),
                new D2D2Config()
                        .fromArgs(args)
                        .engine(DesktopEngine.class)
        );
    }

    @Override
    public void start(Stage stage) {
        Sprite sprite = Assets.loadTexture("test.png").createSprite();

        stage.addChild(sprite, 100, 100);

        System.out.println("done");
    }
}
