package dev.takesome.helix.ui.layout;

/** Bottom-left UI rectangle in already resolved screen/panel coordinates. */
public record UiBox(float x, float y, float w, float h) {
    public UiBox {
        w = Math.max(0f, w);
        h = Math.max(0f, h);
    }

    public static UiBox of(float x, float y, float w, float h) {
        return new UiBox(x, y, w, h);
    }

    public float right() {
        return x + w;
    }

    public float top() {
        return y + h;
    }

    public boolean empty() {
        return w <= 0f || h <= 0f;
    }

    public UiBox inset(UiInsets insets) {
        UiInsets safe = insets == null ? UiInsets.ZERO : insets;
        float nx = x + safe.left();
        float ny = y + safe.bottom();
        float nw = Math.max(0f, w - safe.left() - safe.right());
        float nh = Math.max(0f, h - safe.bottom() - safe.top());
        return new UiBox(nx, ny, nw, nh);
    }

    public UiBox withWidth(float width) {
        return new UiBox(x, y, width, h);
    }
}
