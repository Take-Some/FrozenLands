package dev.takesome.helix.ui.css.units.unitTypes;

import java.util.Set;

import dev.takesome.helix.ui.css.units.UiCssUnitAdapter;
import dev.takesome.helix.ui.css.units.UiCssUnitResolutionContext;

public final class VhCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "vh"; }
    public Set<String> aliases() { return Set.of("wh"); }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) {
        UiCssUnitResolutionContext safe = context == null ? UiCssUnitResolutionContext.defaults() : context;
        return safe.viewportHeight() * value / 100f;
    }
}
