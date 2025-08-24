package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.ApplicationConfig;
import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.engine.DisplayManager;
import com.ancevt.d2d2.engine.Engine;
import com.ancevt.d2d2.engine.NodeFactory;
import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.scene.Renderer;
import com.ancevt.d2d2.scene.Stage;
import com.ancevt.d2d2.scene.text.BitmapFontManager;
import com.ancevt.d2d2.scene.texture.TextureManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DesktopApplicationContext implements ApplicationContext {

    private final Engine engine;
    private final ApplicationConfig applicationConfig;
    private final TextureManager textureManager;
    private final DisplayManager displayManager;
    private final SoundManager soundManager;
    private final BitmapFontManager bitmapFontManager;
    private final NodeFactory nodeFactory;
    private final Stage stage;
    private final Renderer renderer;

    @Override
    public TextureManager textureManager() {
        return textureManager;
    }

    @Override
    public DisplayManager displayManager() {
        return displayManager;
    }

    @Override
    public SoundManager soundManager() {
        return soundManager;
    }

    @Override
    public BitmapFontManager bitmapFontManager() {
        return bitmapFontManager;
    }

    @Override
    public ApplicationConfig config() {
        return applicationConfig;
    }

    @Override
    public NodeFactory nodeFactory() {
        return nodeFactory;
    }

    @Override
    public Stage stage() {
        return stage;
    }

    @Override
    public Renderer renderer() {
        return renderer;
    }

    @Override
    public Engine engine() {
        return engine;
    }
}
