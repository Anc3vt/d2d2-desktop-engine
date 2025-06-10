package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.asset.Asset;
import com.ancevt.d2d2.scene.Group;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureManager;
import com.ancevt.d2d2.scene.texture.TextureRegion;
import com.ancevt.d2d2.util.InputStreamFork;
import lombok.Getter;
import lombok.SneakyThrows;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class DesktopTextureManager implements TextureManager {

    @Getter
    private final Map<Integer, BufferedImage> bufferedImageMap = new HashMap<>();

    final Map<Integer, Texture> loadedTextures = new HashMap<>();
    private final Map<String, Texture> loadedTexturesByAssetPath = new HashMap<>();

    public static void bindTexture(Texture texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
    }

    public static Texture loadTextureInternal(int width, int height) {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null
        );

        return new Texture(textureId, width, height);
    }

    @Override
    public Map<Integer, Texture> getLoadedTextures() {
        return Map.copyOf(loadedTextures);
    }

    @Override
    public Texture loadTexture(InputStream pngInputStream) {
        return actualLoadTexture(pngInputStream);
    }

    @Override
    public Texture loadTexture(String assetPath) {
        return loadedTexturesByAssetPath.computeIfAbsent(assetPath, path -> {
            try (var inputStream = Asset.getAsset(path).getInputStream()) {
                return actualLoadTexture(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void unloadTexture(Texture texture) {
        glDeleteTextures(texture.getId());
        loadedTextures.remove(texture.getId());

        String key = null;
        for (var e : loadedTexturesByAssetPath.entrySet()) {
            if (e.getValue() == texture) key = e.getKey();
        }
        if (key != null) loadedTexturesByAssetPath.remove(key);
    }

    @Override
    public Texture renderBitmapTextToTexture(BitmapText bitmapText) {
        return AwtBitmapTextDrawHelper.bitmapTextToTexture(bitmapText);
    }

    @Override
    public Texture renderGroupToTexture(Group group, int width, int height) {
        return RenderTargetTexture.renderGroupToTexture(group, width, height);
    }

    @Override
    public boolean isTextureActive(Texture texture) {
        return loadedTextures.containsValue(texture);
    }

    @SneakyThrows
    private Texture actualLoadTexture(InputStream pngInputStream) {
        InputStreamFork fork = InputStreamFork.fork(pngInputStream);
        InputStream inputStream = fork.left();

        BufferedImage bufferedImage = ImageIO.read(fork.right());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            byte[] imageBytes = inputStream.readAllBytes();

            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
            }

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            //GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            //GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);

            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    w.get(0),
                    h.get(0),
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    image
            );
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            STBImage.stbi_image_free(image);
            bufferedImageMap.put(textureId, bufferedImage);

            Texture result = new Texture(textureId, w.get(0), h.get(0));
            loadedTextures.put(textureId, result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Could not load texture", e);
        }
    }

    @Override
    public void registerTextureRegion(String key, TextureRegion textureRegion) {
        TextureDataInfoReadHelper.regionMap.put(key, textureRegion);
    }

    @Override
    public void removeTextureRegion(String key) {
        TextureDataInfoReadHelper.regionMap.remove(key);
    }

    @Override
    public Map<String, TextureRegion> getTextureRegionMap() {
        return Map.copyOf(TextureDataInfoReadHelper.regionMap);
    }

    @Override
    public TextureRegion getTextureRegion(String key) {
        return TextureDataInfoReadHelper.regionMap.get(key);
    }

    @Override
    public void loadTextureDataInfo(String assetMetaFile) {
        try {
            TextureDataInfoReadHelper.readTextureDataInfoFile(assetMetaFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TextureDataInfoReadHelper {

        private TextureDataInfoReadHelper() {
        }

        private static Texture currentTexture;

        public static void readTextureDataInfoFile(String assetPath) throws IOException {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Asset.getAsset(assetPath).getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                parseLine(line);
            }
        }

        private static void parseLine(String line) {
            line = line.trim();
            line = line.replaceAll("\\s+", " ");

            if (line.length() == 0) return;

            char firstChar = line.charAt(0);
            if (firstChar == ':') {
                String tileSetName = line.substring(1);
                currentTexture = D2D2.getEngine().getTextureManager().loadTexture(tileSetName);
                return;
            }

            String[] splitted = line.split(" ");

            String textureKey = splitted[0];
            int x = Integer.parseInt(splitted[1]);
            int y = Integer.parseInt(splitted[2]);
            int w = Integer.parseInt(splitted[3]);
            int h = Integer.parseInt(splitted[4]);

            TextureRegion textureRegion = currentTexture.createTextureRegion(x, y, w, h);

            regionMap.put(textureKey, textureRegion);
        }

        static final Map<String, TextureRegion> regionMap = new HashMap<>();

    }
}
