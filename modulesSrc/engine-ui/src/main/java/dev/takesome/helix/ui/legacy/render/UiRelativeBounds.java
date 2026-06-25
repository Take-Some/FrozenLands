package dev.takesome.helix.ui.legacy.render;

import dev.takesome.helix.ui.model.UiRect;

final class UiRelativeBounds {
    static final UiRelativeBounds FULL = new UiRelativeBounds(0f, 0f, 0f, 0f);

    private final float leftInset;
    private final float rightInset;
    private final float topInset;
    private final float bottomInset;

    private UiRelativeBounds(float leftInset, float rightInset, float topInset, float bottomInset) {
        this.leftInset = clamp01(leftInset);
        this.rightInset = clamp01(rightInset);
        this.topInset = clamp01(topInset);
        this.bottomInset = clamp01(bottomInset);
    }

    static UiRelativeBounds of(float leftInset, float rightInset, float topInset, float bottomInset) {
        UiRelativeBounds bounds = new UiRelativeBounds(leftInset, rightInset, topInset, bottomInset);
        return bounds.isFull() ? FULL : bounds;
    }

    UiRect apply(UiRect target) {
        if (target == null || target.w <= 0f || target.h <= 0f || isFull()) return target;

        float x = target.x + target.w * leftInset;
        float y = target.y + target.h * bottomInset;
        float w = target.w * Math.max(0f, 1f - leftInset - rightInset);
        float h = target.h * Math.max(0f, 1f - topInset - bottomInset);
        if (w <= 0f || h <= 0f) return target;
        return new UiRect(x, y, w, h);
    }

    private boolean isFull() {
        return leftInset <= 0.001f
                && rightInset <= 0.001f
                && topInset <= 0.001f
                && bottomInset <= 0.001f;
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }
}
