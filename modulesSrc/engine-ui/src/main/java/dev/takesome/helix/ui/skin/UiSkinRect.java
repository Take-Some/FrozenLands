package dev.takesome.helix.ui.skin;

/** Integer source rectangle in texture-region local coordinates. */
public record UiSkinRect(int x, int y, int w, int h) {
    public static final UiSkinRect EMPTY = new UiSkinRect(0, 0, 0, 0);

    public UiSkinRect {
        x = Math.max(0, x);
        y = Math.max(0, y);
        w = Math.max(0, w);
        h = Math.max(0, h);
    }

    public boolean valid() {
        return w > 0 && h > 0;
    }
}
