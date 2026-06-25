package dev.takesome.helix.ui.css.properties.text;

import dev.takesome.helix.ui.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class TextOverflowCssProperty extends UiCssKeywordPropertySpec<String> {
    public TextOverflowCssProperty() {
        super("text-overflow", Set.of(), true, Map.of(
                "clip", "clip",
                "ellipsis", "ellipsis"
        ), "clip");
    }
}
