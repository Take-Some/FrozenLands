package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class TitleHtmlTag extends UiHtmlBaseTagSpec {
    public TitleHtmlTag() {
        super("title", Set.of(), "panel", UiHtmlCommonAttributes.common());
    }
}
