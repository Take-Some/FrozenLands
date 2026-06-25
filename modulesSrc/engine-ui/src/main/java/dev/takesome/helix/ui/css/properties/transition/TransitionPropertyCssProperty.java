package dev.takesome.helix.ui.css.properties.transition;

import dev.takesome.helix.ui.css.UiCssStringPropertySpec;
import java.util.Set;

public final class TransitionPropertyCssProperty extends UiCssStringPropertySpec {
    public TransitionPropertyCssProperty() {
        super("transition-property", Set.of(), true);
    }
}
