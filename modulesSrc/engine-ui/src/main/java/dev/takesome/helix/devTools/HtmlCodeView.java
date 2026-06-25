package dev.takesome.helix.devTools;

import java.io.Serializable;

public record HtmlCodeView(String path, String text) implements Serializable {
    public HtmlCodeView {
        path = path == null ? "" : path.trim();
        text = text == null ? "" : text;
    }

    public static HtmlCodeView empty() { return new HtmlCodeView("", ""); }
}
