package dev.takesome.helix.devTools.css;

import java.io.Serializable;

import dev.takesome.helix.ui.css.UiCssBox;

public record HtmlCssBoxSnapshot(float x, float y, float width, float height) implements Serializable {
    public static HtmlCssBoxSnapshot empty() { return new HtmlCssBoxSnapshot(0f, 0f, 0f, 0f); }
    public static HtmlCssBoxSnapshot from(UiCssBox box) {
        return box == null ? empty() : new HtmlCssBoxSnapshot(box.x(), box.y(), box.width(), box.height());
    }
}
