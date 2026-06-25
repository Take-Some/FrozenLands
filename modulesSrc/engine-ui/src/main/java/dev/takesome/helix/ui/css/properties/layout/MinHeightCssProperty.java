package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class MinHeightCssProperty extends UiCssLengthPropertySpec {
    public MinHeightCssProperty() {
        super("min-height", Set.of(), true);
    }
}
