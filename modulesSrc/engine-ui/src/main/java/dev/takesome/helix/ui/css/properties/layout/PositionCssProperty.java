package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssKeywordPropertySpec;
import dev.takesome.helix.ui.css.UiPositionMode;
import java.util.Map;
import java.util.Set;

public final class PositionCssProperty extends UiCssKeywordPropertySpec<UiPositionMode> {
    public PositionCssProperty() {
        super("position", Set.of(), true, Map.of(
                "static", new UiPositionMode(false, false),
                "relative", new UiPositionMode(true, false),
                "absolute", new UiPositionMode(false, true),
                "fixed", new UiPositionMode(false, true)
        ), new UiPositionMode(false, false));
    }
}
