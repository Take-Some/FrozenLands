package dev.takesome.helix.ui.css.units.unitTypes;

import dev.takesome.helix.ui.css.units.UiCssUnitAdapter;
import dev.takesome.helix.ui.css.units.UiCssUnitResolutionContext;

public final class PxCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "px"; }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) { return value; }
}
