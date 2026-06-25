package dev.takesome.helix.ui.uiComponents.panel;

import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.List;

/** Simple filled rectangle node with optional border and CSS-like shadow styling. */
public class UiPanelNode extends UiComponent {
    private UiColor color;
    private UiElementSkin backgroundSkin;
    private UiColor borderColor;
    private float borderWidth;
    private List<UiBoxShadow> boxShadows = List.of();

    public UiPanelNode(UiColor color) {
        this.color = color;
    }

    public UiColor color() {
        return color;
    }

    public void setColor(UiColor color) {
        this.color = color;
        markDirty();
    }

    public UiElementSkin backgroundSkin() {
        return backgroundSkin;
    }

    public void setBackgroundSkin(UiElementSkin backgroundSkin) {
        this.backgroundSkin = backgroundSkin;
        markDirty();
    }

    public void setBorder(UiColor borderColor, float borderWidth) {
        this.borderColor = borderColor;
        this.borderWidth = Float.isFinite(borderWidth) ? Math.max(0f, borderWidth) : 0f;
        markDirty();
    }

    public void setShadow(UiColor shadowColor, float offsetX, float offsetY) {
        if (shadowColor == null || shadowColor.a <= 0f) this.boxShadows = List.of();
        else this.boxShadows = List.of(new UiBoxShadow(offsetX, offsetY, 0f, 0f, shadowColor, false));
        markDirty();
    }

    public void setShadow(UiColor shadowColor, float offsetX, float offsetY, float blurRadius, float spreadRadius) {
        if (shadowColor == null || shadowColor.a <= 0f) this.boxShadows = List.of();
        else this.boxShadows = List.of(new UiBoxShadow(offsetX, offsetY, blurRadius, spreadRadius, shadowColor, false));
        markDirty();
    }

    public void setBoxShadows(List<UiBoxShadow> boxShadows) {
        this.boxShadows = boxShadows == null ? List.of() : List.copyOf(boxShadows);
        markDirty();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        UiRect bounds = absoluteBounds();
        for (UiBoxShadow shadow : boxShadows) if (shadow != null && shadow.visible() && !shadow.inset()) ctx.boxShadow(bounds, shadow);
        if (backgroundSkin != null) ctx.drawElement(backgroundSkin, bounds);
        if (color != null && color.a > 0f) ctx.fill(bounds, color);
        for (UiBoxShadow shadow : boxShadows) if (shadow != null && shadow.visible() && shadow.inset()) ctx.boxShadow(bounds, shadow);
        if (borderColor != null && borderWidth > 0f && borderColor.a > 0f) ctx.stroke(bounds, borderColor, borderWidth);
    }
}
