package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.D2D2Config;
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.debug.StarletSpace;
import com.ancevt.d2d2.engine.desktop.DesktopEngine;
import com.ancevt.d2d2.lifecycle.D2D2Application;
import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.Sprite;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.scene.shape.BorderedRectangle;
import com.ancevt.d2d2.scene.shape.RectangleShape;
import com.ancevt.d2d2.scene.text.BitmapText;

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
        StarletSpace.haveFun();


        Sprite sprite = Assets.loadTexture("test2.png").createSprite();
        stage.addChild(sprite, 100, 100);

        sprite.setAlpha(0.5f);

        System.out.println(sprite.getAlpha());
        sprite.setColor(Color.YELLOW);

        Sprite sprite2 = Assets.loadTexture("test2.png").createTextureRegion(0, 0, 128, 128).createSprite();

        stage.addChild(sprite2);

        BitmapText bitmapText = new BitmapText();
        bitmapText.setText("Hello world");
        bitmapText.setColor(Color.YELLOW);
        stage.addChild(bitmapText, 10, 10);

        RectangleShape rectangleShape = new RectangleShape(50, 50, Color.YELLOW);
        stage.addChild(rectangleShape, 300, 300);


        BorderedRectangle borderedRectangle = new BorderedRectangle(50, 50, Color.BLUE_VIOLET, Color.RED);

        stage.addChild(borderedRectangle, 400, 400);

        stage.onTick(e -> {
            sprite2.moveX(0.5f);
        });

        stage.addChild(new FpsMeter());

        stage.onResize(e -> {
            System.out.println("Stage resized: " + e.getWidth() + "x" + e.getHeight());
        });
    }
}
