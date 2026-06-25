package dev.takesome.helix.ui.css;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.dom.UiDomElement;
import java.util.Set;

public abstract class UiCssStringPropertySpec extends UiCssBasePropertySpec {
    protected UiCssStringPropertySpec(String cssName, Set<String> cssAliases, boolean fallbackAttribute) {
        super(cssName, cssAliases, fallbackAttribute);
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), "");
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), trimToEmpty(rawValue));
    }

    public String read(UiDomElement element, String fallbackValue) {
        String value = raw(element);
        return value.isBlank() ? fallbackValue : value;
    }
}
