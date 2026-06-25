package dev.takesome.helix.ui.components;

import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

/**
 * Static retained UI element node backed by a declarative engine skin.
 */
public final class UiElementNode extends UiComponent {
    private UiElementSkin element;

    public UiElementNode(UiElementSkin element) {
        this.element = element;
    }

    public UiElementSkin element() {
        return element;
    }

    public void setElement(UiElementSkin element) {
        this.element = element;
        markDirty();
    }

    @Override
    protected UiRect debugContentBounds(UiRenderContext ctx) {
        UiRect bounds = absoluteBounds();
        if (ctx == null || element == null) return bounds;
        UiRect content = ctx.elementContentBounds(element, bounds);
        return content == null ? bounds : content;
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        if (ctx == null || element == null) return;
        ctx.drawElement(element, absoluteBounds());
    }
}
