package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class WidthCssProperty extends UiCssLengthPropertySpec {
    public WidthCssProperty() {
        super("width", Set.of("w"), true);
    }
}
