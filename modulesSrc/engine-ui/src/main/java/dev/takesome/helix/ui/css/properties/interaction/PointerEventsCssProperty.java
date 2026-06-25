package dev.takesome.helix.ui.css.properties.interaction;

import dev.takesome.helix.ui.css.UiCssStringPropertySpec;
import java.util.Set;

public final class PointerEventsCssProperty extends UiCssStringPropertySpec {
    public PointerEventsCssProperty() {
        super("pointer-events", Set.of(), true);
    }
}
