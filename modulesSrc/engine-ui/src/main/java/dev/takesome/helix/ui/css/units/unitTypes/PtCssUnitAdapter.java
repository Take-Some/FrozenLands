package dev.takesome.helix.ui.css.units.unitTypes;

import dev.takesome.helix.ui.css.units.UiCssUnitAdapter;
import dev.takesome.helix.ui.css.units.UiCssUnitResolutionContext;

public final class PtCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "pt"; }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) { return value * 96f / 72f; }
}
