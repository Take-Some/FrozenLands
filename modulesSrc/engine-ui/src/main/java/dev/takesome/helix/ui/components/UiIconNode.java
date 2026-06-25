package dev.takesome.helix.ui.components;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Retained Font Awesome icon node backed by a backend-neutral UiIcon descriptor. */
public final class UiIconNode extends UiComponent {
    private UiIcon icon;
    private UiColor color = UiColor.WHITE;
    private float scale = 0.82f;

    public UiIconNode(UiIcon icon) {
        this.icon = icon;
    }

    public UiIcon icon() {
        return icon;
    }

    public void setIcon(UiIcon icon) {
        if (this.icon == icon) return;
        this.icon = icon;
        markDirty();
    }

    public void setColor(UiColor color) {
        if (color == null) return;
        this.color = color;
        markDirty();
    }

    public void setScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0f) return;
        this.scale = scale;
        markDirty();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        if (ctx == null || icon == null || !ctx.supportsIcons()) return;
        UiRect bounds = absoluteBounds();
        ctx.icon(icon, bounds, scale, color, TextAlign.CENTER);
    }
}
