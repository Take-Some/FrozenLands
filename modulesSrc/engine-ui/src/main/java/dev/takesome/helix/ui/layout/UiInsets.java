package dev.takesome.helix.ui.layout;

/** Pixel-space UI insets: left, bottom, right, top. */
public record UiInsets(float left, float bottom, float right, float top) {
    public static final UiInsets ZERO = new UiInsets(0f, 0f, 0f, 0f);

    public UiInsets {
        left = Math.max(0f, left);
        bottom = Math.max(0f, bottom);
        right = Math.max(0f, right);
        top = Math.max(0f, top);
    }

    public static UiInsets of(float left, float bottom, float right, float top) {
        return new UiInsets(left, bottom, right, top);
    }

    public static UiInsets symmetric(float horizontal, float vertical) {
        return new UiInsets(horizontal, vertical, horizontal, vertical);
    }
}
