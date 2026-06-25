package dev.takesome.helix.ui.node;

import dev.takesome.helix.ui.model.UiRect;

final class NodeGeometry {
    private float x;
    private float y;
    private float width;
    private float height;

    void setBounds(Node owner, float nextX, float nextY, float nextWidth, float nextHeight) {
        float resolvedWidth = Math.max(0f, nextWidth);
        float resolvedHeight = Math.max(0f, nextHeight);
        if (x == nextX && y == nextY && width == resolvedWidth && height == resolvedHeight) return;

        x = nextX;
        y = nextY;
        width = resolvedWidth;
        height = resolvedHeight;
        owner.markDirty();
        owner.onBoundsChanged();
    }

    UiRect bounds() {
        return new UiRect(x, y, width, height);
    }

    UiRect absoluteBounds(Node owner) {
        return new UiRect(absoluteX(owner), absoluteY(owner), width, height);
    }

    boolean containsAbsolute(Node owner, float px, float py) {
        float ax = absoluteX(owner);
        float ay = absoluteY(owner);
        return px >= ax && py >= ay && px <= ax + width && py <= ay + height;
    }

    float absoluteX(Node owner) {
        float value = x;
        Node cursor = owner.parentInternal();
        while (cursor != null) {
            value += cursor.localXInternal();
            cursor = cursor.parentInternal();
        }
        return value;
    }

    float absoluteY(Node owner) {
        float value = y;
        Node cursor = owner.parentInternal();
        while (cursor != null) {
            value += cursor.localYInternal();
            cursor = cursor.parentInternal();
        }
        return value;
    }

    float x() { return x; }
    float y() { return y; }
    float width() { return width; }
    float height() { return height; }
}
