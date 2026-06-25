package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class HeadHtmlTag extends UiHtmlBaseTagSpec {
    public HeadHtmlTag() {
        super("head", Set.of(), "panel", UiHtmlCommonAttributes.common("profile", "data-*"));
    }
}
