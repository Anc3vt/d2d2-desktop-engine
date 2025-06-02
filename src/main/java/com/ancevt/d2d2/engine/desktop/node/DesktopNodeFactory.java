package com.ancevt.d2d2.engine.desktop.node;

import com.ancevt.d2d2.engine.NodeFactory;
import com.ancevt.d2d2.scene.BitmapCanvas;

public class DesktopNodeFactory implements NodeFactory {
    @Override
    public BitmapCanvas createBitmapCanvas(int width, int height) {
        return new BitmapCanvasGpu(width, height);
    }
}
