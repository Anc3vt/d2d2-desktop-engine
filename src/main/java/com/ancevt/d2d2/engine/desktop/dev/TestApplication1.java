package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.ApplicationConfig;
import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.lifecycle.Application;

public class TestApplication1 implements Application {

    public static void main(String[] args) {
        D2D2.init(
                new TestApplication1(),
                new ApplicationConfig()
                        .args(args)
        );
    }


    @Override
    public void start(ApplicationContext applicationContext) {

        applicationContext.stage().addChild(new FpsMeter());
    }
}
