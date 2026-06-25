package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class MinWidthCssProperty extends UiCssLengthPropertySpec {
    public MinWidthCssProperty() {
        super("min-width", Set.of(), true);
    }
}
