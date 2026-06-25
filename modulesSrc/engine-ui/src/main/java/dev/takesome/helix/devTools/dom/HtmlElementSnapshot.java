package dev.takesome.helix.devTools.dom;

import java.io.Serializable;

public record HtmlElementSnapshot(
        int id,
        int parent,
        int depth,
        String tag,
        String selector,
        String path,
        float x,
        float y,
        float width,
        float height,
        String attributeText,
        String display,
        int childCount
) implements Serializable {
    public HtmlElementSnapshot {
        id = Math.max(0, id);
        parent = Math.max(0, parent);
        depth = Math.max(0, depth);
        tag = clean(tag);
        selector = clean(selector);
        path = clean(path);
        x = finiteOrZero(x);
        y = finiteOrZero(y);
        width = nonNegativeFinite(width);
        height = nonNegativeFinite(height);
        attributeText = clean(attributeText);
        display = clean(display);
        childCount = Math.max(0, childCount);
    }

    public HtmlElementSnapshot(int id, int parent, int depth, String tag, String selector, String path, float x, float y, float width, float height) {
        this(id, parent, depth, tag, selector, path, x, y, width, height, "", "", 0);
    }

    public HtmlElementSnapshot(int id, int parent, int depth, String tag, String selector, String path) {
        this(id, parent, depth, tag, selector, path, 0f, 0f, 0f, 0f);
    }

    public HtmlElementSnapshot(int id, int parent, int depth, String tag, String selector) {
        this(id, parent, depth, tag, selector, selector);
    }

    public boolean hasChildren() {
        return childCount > 0;
    }

    public String dimensionsLabel() {
        return Math.round(width) + "×" + Math.round(height) + " @ " + Math.round(x) + "," + Math.round(y);
    }

    public String displayName() {
        return selector.isBlank() ? tag + " #" + id : selector + " #" + id;
    }

    @Override
    public String toString() {
        return displayName();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0f;
    }

    private static float nonNegativeFinite(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
