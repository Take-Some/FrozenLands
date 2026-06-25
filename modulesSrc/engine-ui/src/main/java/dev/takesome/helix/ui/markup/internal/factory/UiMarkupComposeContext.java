package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.html.UiHtmlTagSpec;

import java.util.Map;

public record UiMarkupComposeContext(
        UiDomElement element,
        UiHtmlTagSpec tag,
        Map<String, String> style,
        float parentW,
        float parentH,
        UiDomRetainedNodeFactory nodes
) {
}
