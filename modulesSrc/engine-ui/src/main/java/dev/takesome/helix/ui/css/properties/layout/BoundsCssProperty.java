package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssBasePropertySpec;
import dev.takesome.helix.ui.css.UiCssBounds;
import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssValue;
import dev.takesome.helix.ui.dom.UiDomElement;
import java.util.Optional;
import java.util.Set;

public final class BoundsCssProperty extends UiCssBasePropertySpec {
    public BoundsCssProperty() {
        super("bounds", Set.of(), true);
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), null);
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), UiCssBounds.parse(rawValue));
    }

    public Optional<UiCssBounds> read(UiDomElement element) {
        String raw = raw(element);
        return raw.isBlank() ? Optional.empty() : Optional.ofNullable(UiCssBounds.parse(raw));
    }
}
