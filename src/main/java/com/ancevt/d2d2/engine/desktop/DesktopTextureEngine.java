package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureEngine;
import com.ancevt.d2d2.scene.texture.TextureRegionCombinerCell;

import java.io.InputStream;

public class DesktopTextureEngine implements TextureEngine {
    @Override
    public boolean bind(Texture texture) {
        return false;
    }

    @Override
    public void enable(Texture texture) {

    }

    @Override
    public void disable(Texture texture) {

    }

    @Override
    public Texture createTexture(InputStream pngInputStream) {
        return null;
    }

    @Override
    public Texture createTexture(String assetPath) {
        return null;
    }

    @Override
    public Texture createTexture(int width, int height, TextureRegionCombinerCell[] cells) {
        return null;
    }

    @Override
    public void unloadTexture(Texture texture) {

    }

    @Override
    public Texture bitmapTextToTexture(BitmapText bitmapText) {
        return null;
    }
}
