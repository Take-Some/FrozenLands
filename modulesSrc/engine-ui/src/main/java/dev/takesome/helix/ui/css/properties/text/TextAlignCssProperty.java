package dev.takesome.helix.ui.css.properties.text;

import dev.takesome.helix.ui.css.UiCssStringPropertySpec;
import java.util.Set;

public final class TextAlignCssProperty extends UiCssStringPropertySpec {
    public TextAlignCssProperty() {
        super("text-align", Set.of("align"), true);
    }
}
