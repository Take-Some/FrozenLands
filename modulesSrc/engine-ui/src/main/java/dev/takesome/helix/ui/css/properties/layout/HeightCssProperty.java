package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class HeightCssProperty extends UiCssLengthPropertySpec {
    public HeightCssProperty() {
        super("height", Set.of("h"), true);
    }
}
