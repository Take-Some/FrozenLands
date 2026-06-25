package dev.takesome.helix.ui.css.properties.text;

import dev.takesome.helix.ui.css.UiCssStringPropertySpec;
import dev.takesome.helix.ui.css.UiCssFontFamilyResolver;
import dev.takesome.helix.ui.css.UiCssValue;
import java.util.Set;

public final class FontFamilyCssProperty extends UiCssStringPropertySpec {
    public FontFamilyCssProperty() {
        super("font-family", Set.of(), true);
    }

    @Override
    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), UiCssFontFamilyResolver.DEFAULT_STACK);
    }
}
