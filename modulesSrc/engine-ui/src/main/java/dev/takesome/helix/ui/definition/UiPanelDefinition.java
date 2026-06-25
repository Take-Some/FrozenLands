package dev.takesome.helix.ui.definition;

import java.util.ArrayList;
import java.util.List;
import dev.takesome.helix.ui.layout.UiAnchor;
import dev.takesome.helix.ui.layout.UiLayoutResolver;
import dev.takesome.helix.ui.layout.UiPanelLayout;
import dev.takesome.helix.ui.model.UiRect;

/** Panel described by JSON: position, nine-slice background and child widgets. */
public final class UiPanelDefinition {
    public String id;
    public String visible;
    public String anchor = "topLeft";
    public float x;
    public float y;
    public float w;
    public float h;

    /**
     * Optional layout mode for child widgets.
     * Supported values: absolute/default, vbox/vertical/column, hbox/horizontal/row.
     */
    public String layout;
    public String mode;

    /** Auto-layout gap between visible widgets. Negative means engine default. */
    public float gap = -1f;

    /** Auto-layout panel padding in UI pixels. Negative components default to zero. */
    public float paddingLeft = -1f;
    public float paddingRight = -1f;
    public float paddingTop = -1f;
    public float paddingBottom = -1f;

    /**
     * Child overflow policy for panel content.
     * Supported values: hidden/default, scroll and error clip children; visible disables child clipping.
     */
    public String overflow = "hidden";
    public boolean clipChildren = true;

    /** Auto-layout sizing constraints. Existing w/h still act as fixed size for absolute panels. */
    public float minW;
    public float minH;
    public float maxW;
    public float maxH;
    public float maxWidthRatio;
    public float maxHeightRatio;

    /**
     * Optional group name. Panels in the same group are stacked by document order,
     * using the first panel as the anchor origin.
     */
    public String stackGroup;
    public float stackGap = -1f;
    public boolean equalStackWidth;

    public String background;
    /** Draw order layer. Higher layers are rendered after lower layers. */
    public int layer;
    /** Optional solid/tinted fill drawn before the panel background. RGBA, 0..1. */
    public float[] fillColor;
    /** When true, fillColor is resolved against the full UI viewport before this panel draws. */
    public boolean overlay;
    public float alpha = 1f;
    public List<UiWidgetDefinition> widgets = new ArrayList<>();

    public UiRect resolve(float viewportW, float viewportH) {
        UiAnchor a = UiAnchor.parse(anchor);
        return UiLayoutResolver.resolve(new UiPanelLayout(a, x, y, w, h, mode), viewportW, viewportH);
    }
}
