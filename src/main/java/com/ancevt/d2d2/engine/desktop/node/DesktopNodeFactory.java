package com.ancevt.d2d2.engine.desktop.node;

import com.ancevt.d2d2.engine.NodeFactory;
import com.ancevt.d2d2.scene.*;
import com.ancevt.d2d2.scene.interactive.InteractiveGroup;
import com.ancevt.d2d2.scene.interactive.InteractiveSprite;
import com.ancevt.d2d2.scene.shape.*;
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
        return new BasicSprite(textureRegion);
    }

    @Override
    public InteractiveSprite createInteractiveSprite() {
        return new InteractiveSprite();
    }

    @Override
    public InteractiveSprite createInteractiveSprite(TextureRegion textureRegion) {
        InteractiveSprite interactiveSprite = new InteractiveSprite();
        interactiveSprite.setTextureRegion(textureRegion);
        return interactiveSprite;
    }

    @Override
    public InteractiveGroup createInteractiveGroup(int width, int height) {
        return new InteractiveGroup(width, height);
    }

    @Override
    public Bitmap createBitmap(int width, int height) {
        return new BitmapGpu(width, height);
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
