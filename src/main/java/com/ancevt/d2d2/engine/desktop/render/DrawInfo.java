package com.ancevt.d2d2.engine.desktop.render;

import java.nio.FloatBuffer;

interface DrawInfo {
    int render(FloatBuffer vertexBuffer, DesktopRenderer renderer);

    int getTextureId();
}
