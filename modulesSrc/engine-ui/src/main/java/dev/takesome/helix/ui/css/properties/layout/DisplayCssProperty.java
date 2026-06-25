package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssKeywordPropertySpec;
import dev.takesome.helix.ui.css.UiDisplayMode;
import java.util.Map;
import java.util.Set;

public final class DisplayCssProperty extends UiCssKeywordPropertySpec<UiDisplayMode> {
    public DisplayCssProperty() {
        super("display", Set.of(), true, Map.of(
                "none", new UiDisplayMode(true, false),
                "block", new UiDisplayMode(false, false),
                "inline", new UiDisplayMode(false, false),
                "inline-block", new UiDisplayMode(false, false),
                "flex", new UiDisplayMode(false, true)
        ), new UiDisplayMode(false, false));
    }
}
