package dev.takesome.helix.ui.css.units.unitTypes;

import dev.takesome.helix.ui.css.units.UiCssUnitAdapter;
import dev.takesome.helix.ui.css.units.UiCssUnitResolutionContext;

public final class AutoCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "auto"; }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) { return fallback; }
    public String cssText(float value) { return "auto"; }
}
