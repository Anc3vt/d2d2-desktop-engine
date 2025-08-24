package com.ancevt.d2d2.engine.desktop.dev;

import com.ancevt.d2d2.ApplicationConfig;
import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.lifecycle.Application;

public class ConsoleTest implements Application {

    public static void main(String[] args) {
        D2D2.init(new ConsoleTest(), new ApplicationConfig().args(args));
    }

    @Override
    public void start(ApplicationContext applicationContext) {

    }
}
