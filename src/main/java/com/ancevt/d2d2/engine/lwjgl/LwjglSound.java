/**
 * Copyright (C) 2025 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.sound.Sound;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.openal.AL10.*;

public class LwjglSound implements Sound {

    private final int bufferId;
    private final int sourceId;
    private boolean disposed = false;

    public LwjglSound(String assetPath) {
        this(Assets.getAsset(assetPath).getInputStream());
    }

    @SneakyThrows
    public LwjglSound(InputStream inputStream) {
        Bitstream bitstream = new Bitstream(inputStream);
        Decoder decoder = new Decoder();

        List<Short> pcmList = new ArrayList<>();
        int channels = 0;
        int sampleRate = 0;

        try {
            while (true) {
                javazoom.jl.decoder.Header header = bitstream.readFrame();
                if (header == null) {
                    break;
                }

                SampleBuffer sampleBuffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                if (channels == 0) {
                    channels = header.mode() == 3 ? 1 : 2; // 1 = Mono, 2 = Stereo
                    sampleRate = header.frequency();
                }

                for (short sample : sampleBuffer.getBuffer()) {
                    pcmList.add(sample);
                }

                bitstream.closeFrame();
            }
        } catch (JavaLayerException e) {
            e.printStackTrace();
        }

        short[] pcm = new short[pcmList.size()];
        for (int i = 0; i < pcmList.size(); i++) {
            pcm[i] = pcmList.get(i);
        }

        int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;

        bufferId = alGenBuffers();
        ByteBuffer buffer = ByteBuffer.allocateDirect(pcm.length * 2).order(ByteOrder.nativeOrder());
        buffer.asShortBuffer().put(pcm);
        alBufferData(bufferId, format, buffer, sampleRate);

        sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcef(sourceId, AL_GAIN, 1f); // Установите начальную громкость на приемлемый уровень
    }

    @Override
    public void play() {
        if (!disposed) {
            alSourcePlay(sourceId);
        }
    }

    @Override
    public void asyncPlay() {
        new Thread(this::play).start();
    }

    @Override
    public void stop() {
        if (!disposed) {
            alSourceStop(sourceId);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (!disposed) {
            alSourcef(sourceId, AL_GAIN, volume);
        }
    }

    @Override
    public float getVolume() {
        return disposed ? 0.0f : alGetSourcef(sourceId, AL_GAIN);
    }

    @Override
    public void setPan(float pan) {
        if (!disposed) {
            alSource3f(sourceId, AL_POSITION, pan, 0, 0);
        }
    }

    @Override
    public float getPan() {
        if (disposed) {
            return 0.0f;
        }
        float[] position = new float[3];
        alGetSource3f(sourceId, AL_POSITION, position, new float[1], new float[1]);
        return position[0];
    }

    @Override
    public void dispose() {
        if (!disposed) {
            alDeleteSources(sourceId);
            alDeleteBuffers(bufferId);
            disposed = true;
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public int getBufferId() {
        return bufferId;
    }

    public int getSourceId() {
        return sourceId;
    }
}
