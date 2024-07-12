package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.engine.SoundManager;
import com.ancevt.d2d2.sound.Sound;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class LwjglSoundManager implements SoundManager {


    private long device;
    private long context;

    public LwjglSoundManager() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }

        // Create context
        try (MemoryStack stack = stackPush()) {
            IntBuffer contextAttribList = stack.mallocInt(1);
            contextAttribList.put(0, 0);
            context = alcCreateContext(device, contextAttribList);
        }

        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context.");
        }

        // Make the context current
        alcMakeContextCurrent(context);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            throw new IllegalStateException("OpenAL 1.0 not supported.");
        }
    }

    @Override
    public void cleanup() {
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    @Override
    public Sound createSound(InputStream inputStream) {
        return new LwjglSound(inputStream);
    }

    @Override
    public Sound createSoundFromAsset(String assetFileName) {
        return new LwjglSound(assetFileName);
    }
}
