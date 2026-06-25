package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class FlexWrapCssProperty extends UiCssKeywordPropertySpec<String> {
    public FlexWrapCssProperty() {
        super("flex-wrap", Set.of(), true, Map.of(
                "nowrap", "nowrap",
                "wrap", "wrap"
        ), "nowrap");
    }
}
