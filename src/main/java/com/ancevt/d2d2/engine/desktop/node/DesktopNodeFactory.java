package com.ancevt.d2d2.engine.desktop.node;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.engine.NodeFactory;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.shape.*;
import com.ancevt.d2d2.scene.texture.Texture;
import com.ancevt.d2d2.scene.texture.TextureRegion;

public class DesktopNodeFactory implements NodeFactory {
    @Override
    public Group createGroup() {
        return new BasicGroup();
    }

    @Override
    public Sprite createSprite() {
        return new BasicSprite();
    }

    @Override
    public Sprite createSprite(TextureRegion textureRegion) {
        Sprite sprite = new BasicSprite();
        sprite.setTextureRegion(textureRegion);
        return sprite;
    }

    @Override
    public Sprite createSprite(Texture texture) {
        Sprite sprite = new BasicSprite();
        sprite.setTextureRegion(texture.createTextureRegion());
        return sprite;
    }

    @Override
    public Sprite createSprite(String assetFilename) {
        return createSprite(
                D2D2.getTextureManager()
                        .loadTexture(assetFilename)
                        .createTextureRegion()
        );
    }

    @Override
    public Sprite createSprite(String asset, int regionX, int regionY, int regionWidth, int regionHeight) {
        return createSprite(
                D2D2.getTextureManager()
                        .loadTexture(asset)
                        .createTextureRegion(regionX, regionY, regionWidth, regionHeight)
        );
    }

    @Override
    public BitmapCanvas createBitmapCanvas(int width, int height) {
        return new BitmapCanvasGpu(width, height);
    }

    @Override
    public AnimatedSprite createAnimatedSprite() {
        return new AnimatedSprite();
    }

    @Override
    public AnimatedSprite createAnimatedSprite(TextureRegion[] frames) {
        return new AnimatedSprite(frames);
    }

    @Override
    public AnimatedGroup createAnimatedGroup() {
        return new AnimatedGroup();
    }

    @Override
    public AnimatedGroup createAnimatedGroup(Sprite[] frames) {
        return new AnimatedGroup(frames);
    }

    @Override
    public RectangleShape createRectangle(float width, float height) {
        return new RectangleShape(width, height);
    }

    @Override
    public RectangleShape createRectangle(float width, float height, Color color) {
        return new RectangleShape(width, height, color);
    }

    @Override
    public BorderedRectangleShape createBorderedRectangle(float width, float height) {
        return new BorderedRectangleShape(width, height);
    }

    @Override
    public BorderedRectangleShape createBorderedRectangle(float width, float height, Color fillColor) {
        return new BorderedRectangleShape(width, height, fillColor);
    }

    @Override
    public BorderedRectangleShape createBorderedRectangle(float width, float height, Color fillColor, Color borderColor) {
        return new BorderedRectangleShape(width, height, fillColor, borderColor);
    }

    @Override
    public LineBatch createLineBatch() {
        return new LineBatch();
    }

    @Override
    public LineBatch createLineBatch(Color color) {
        LineBatch lineBatch = new LineBatch();
        lineBatch.setColor(color);
        return lineBatch;
    }

    @Override
    public FreeShape createFreeShape() {
        return new FreeShape();
    }

    @Override
    public FreeShape createFreeShape(Color color) {
        FreeShape freeShape = new FreeShape();
        freeShape.setColor(color);
        return freeShape;
    }

    @Override
    public CircleShape createCircleShape(float radius, int numVertices) {
        return new CircleShape(radius, numVertices);
    }

    @Override
    public CircleShape createCircleShape(float radius, int numVertices, Color color) {
        CircleShape circleShape = new CircleShape(radius, numVertices);
        circleShape.setColor(color);
        return circleShape;
    }

    @Override
    public RoundedCornerShape createRoundCornerShape(float width, float height, float radius, int segments) {
        return new RoundedCornerShape(width, height, radius, segments);
    }
}
