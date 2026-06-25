package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class LinkHtmlTag extends UiHtmlBaseTagSpec {
    public LinkHtmlTag() {
        super("link", Set.of(), "panel", UiHtmlCommonAttributes.common(
                "rel", "href", "type", "media", "as", "crossorigin", "integrity", "disabled", "referrerpolicy", "sizes", "data-*"
        ));
    }
}
