package dev.takesome.helix.ui.css;


import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import java.util.Locale;

public final class UiCssParseContext {
    public String keyword(String raw) {
        return lowerTrimToEmpty(raw, Locale.ROOT);
    }
}
