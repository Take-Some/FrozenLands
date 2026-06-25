package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class HtmlHtmlTag extends UiHtmlBaseTagSpec {
    public HtmlHtmlTag() {
        super("html", Set.of(), "panel", UiHtmlCommonAttributes.root("lang", "dir"));
    }
}
