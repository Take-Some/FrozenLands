package dev.takesome.helix.ui.uiComponents.image;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Retained image node backed by a backend-neutral resource reference. */
public final class UiImageNode extends UiComponent {
    private static final UiColor MISSING_IMAGE = new UiColor(0.18f, 0.10f, 0.13f, 0.50f);

    private String source;

    public UiImageNode(String source) {
        this.source = trimToEmpty(source);
    }

    public String source() {
        return source;
    }

    public void setSource(String source) {
        String next = trimToEmpty(source);
        if (this.source.equals(next)) return;
        this.source = next;
        markDirty();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        UiRect bounds = absoluteBounds();
        if (source.isBlank() || !ctx.image(source, bounds, 1f)) {
            ctx.fill(bounds, MISSING_IMAGE);
        }
    }
}
