package dev.takesome.helix.ui.css.properties.paint;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.css.UiCssBasePropertySpec;
import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssValue;
import java.util.Set;

/** Engine UI skin reference. Concrete ids are resolved by the UI skin registry. */
public final class UiSkinCssProperty extends UiCssBasePropertySpec {
    public UiSkinCssProperty() {
        super("ui-skin", Set.of("skin"), true);
    }

    @Override
    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), "none");
    }

    @Override
    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), cssUrlReference(rawValue));
    }

    private String cssUrlReference(String raw) {
        if (raw == null || raw.isBlank()) return "none";
        String value = unquote(raw.trim());
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open > 0 && close > open && "url".equalsIgnoreCase(value.substring(0, open).trim())) {
            return unquote(value.substring(open + 1, close).trim());
        }
        return value;
    }

    private String unquote(String value) {
        String out = trimToEmpty(value);
        if (out.length() >= 2 && ((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'")))) {
            return out.substring(1, out.length() - 1).trim();
        }
        return out;
    }
}
