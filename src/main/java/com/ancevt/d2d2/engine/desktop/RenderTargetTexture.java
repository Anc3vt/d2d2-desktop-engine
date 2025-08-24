package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.D2D2_legacy;
import com.ancevt.d2d2.engine.desktop.render.DesktopRenderer;
import com.ancevt.d2d2.scene.Group;
import com.ancevt.d2d2.scene.texture.Texture;
import lombok.Getter;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderTargetTexture {

    private final int fboId;
    @Getter
    private final int textureId;
    private final int width;
    private final int height;

    public RenderTargetTexture(int width, int height) {
        this.width = width;
        this.height = height;

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer incomplete: " + status);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void dispose() {
        glDeleteFramebuffers(fboId);
        glDeleteTextures(textureId);
    }

    public static Texture renderGroupToTexture(Group group, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("Group has invalid size: " + width + "x" + height);
        }

        if (group.isOnScreen()) {
            throw new IllegalStateException("group can't be on screen when rendering to texture");
        }

        int fbo = glGenFramebuffers();
        int texId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texId, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete");
        }

        // 👇 рендер в текстуру
        glViewport(0, 0, width, height);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        DesktopRenderer renderer = (DesktopRenderer) D2D2_legacy.getEngine().getRenderer();

        renderer.renderGroupToCurrentFrameBuffer(group, width, height); // ⬅️ тебе надо этот метод добавить

        // восстановим дефолтный FBO
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);

        Texture result = new Texture(texId, width, height);

        DesktopTextureManager desktopTextureManager = (DesktopTextureManager) D2D2_legacy.getTextureManager();

        // do not use getter getLoadedTexture, it will return the copy of map
        desktopTextureManager.loadedTextures.put(texId, result);

        return result;
    }
}


