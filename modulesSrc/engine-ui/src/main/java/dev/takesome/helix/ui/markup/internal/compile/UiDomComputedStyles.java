package dev.takesome.helix.ui.markup.internal.compile;

import dev.takesome.helix.ui.css.UiCssKeyframesRule;
import dev.takesome.helix.ui.dom.UiDomElement;

import java.util.Map;

record UiDomComputedStyles(
        Map<UiDomElement, Map<String, String>> base,
        Map<UiDomElement, Map<String, Map<String, String>>> states,
        Map<UiDomElement, Map<String, Map<String, String>>> elements,
        Map<String, UiCssKeyframesRule> keyframes
) {
    UiDomComputedStyles(
            Map<UiDomElement, Map<String, String>> base,
            Map<UiDomElement, Map<String, Map<String, String>>> states,
            Map<UiDomElement, Map<String, Map<String, String>>> elements
    ) {
        this(base, states, elements, Map.of());
    }
}
