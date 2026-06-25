package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class MetaHtmlTag extends UiHtmlBaseTagSpec {
    public MetaHtmlTag() {
        super("meta", Set.of(), "panel", UiHtmlCommonAttributes.common(
                "charset", "name", "content", "http-equiv", "property", "scheme", "data-*"
        ));
    }
}
