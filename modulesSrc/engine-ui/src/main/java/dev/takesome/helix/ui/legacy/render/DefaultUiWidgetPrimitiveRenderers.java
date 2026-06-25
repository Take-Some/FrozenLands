package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveRegistryLoader;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveRendererRegistry;

/** Built-in primitive renderer pack for JSON-driven widgets. */
final class DefaultUiWidgetPrimitiveRenderers {
    static final String NAMESPACE = "helix.ui.widget.primitives.builtin";
    static final String MANIFEST_RESOURCE = "dev/takesome/helix/ui/primitives/builtin-widget-primitives.json";
    private static final float[] CARD_SHADOW = { 0.0f, 0.0f, 0.0f, 0.32f };
    private static final float[] CARD_HIGHLIGHT = { 1.0f, 0.86f, 0.56f, 0.18f };
    private static final float[] CARD_EDGE = { 0.0f, 0.0f, 0.0f, 0.24f };

    private DefaultUiWidgetPrimitiveRenderers() {
    }

    static UiWidgetPrimitiveRendererRegistry registry(
            UiNineSliceResolver resolver,
            UiWidgetImageRenderer images,
            UiTextRenderer text,
            UiProgressBarRenderer progress
    ) {
        UiWidgetPrimitiveRendererRegistry registry = new UiWidgetPrimitiveRendererRegistry()
                .register(NAMESPACE, "text", context -> text.text(
                        context.binding(), context.batch(), context.font(), context.panelRect(), context.widget(), context.key() + ":text"))
                .register(NAMESPACE, "icon", context -> icon(images, resolver, text, context))
                .register(NAMESPACE, "bar", context -> progress.render(
                        context.binding(), context.batch(), context.font(), context.panelRect(), context.widget(), context.key() + ":bar-label", context.uiTime()))
                .register(NAMESPACE, "stretch", context -> images.stretchImage(context.batch(), context.panelRect(), context.widget()))
                .register(NAMESPACE, "image", context -> images.image(context.batch(), context.panelRect(), context.widget()))
                .register(NAMESPACE, "fill", context -> images.fillWidget(context.batch(), context.panelRect(), context.widget()));
        return new UiWidgetPrimitiveRegistryLoader(DefaultUiWidgetPrimitiveRenderers.class.getClassLoader())
                .reloadResource(registry, MANIFEST_RESOURCE);
    }

    private static void icon(UiWidgetImageRenderer images, UiNineSliceResolver resolver, UiTextRenderer text, UiWidgetRenderContext context) {
        UiWidgetDefinition widget = context.widget();
        if (widget == null) return;
        float size = widget.iconSize > 0f ? widget.iconSize : 18f;
        TextureRegion icon = resolver.region(widget.icon, widget.frame);
        float widgetX = context.panelRect().x + UiWidgetBounds.x(widget);
        float widgetY = context.panelRect().y + UiWidgetBounds.y(widget);
        float widgetW = UiWidgetBounds.w(widget);
        float widgetH = UiWidgetBounds.h(widget);

        boolean card = widget.fillColor != null;
        if (card) {
            float radius = Math.max(6f, Math.min(10f, widgetH * 0.34f));
            images.fillRoundedRect(context.batch(), widgetX + 1f, widgetY - 2f, widgetW, widgetH, radius, CARD_SHADOW, 1f);
            images.fillRoundedRect(context.batch(), widgetX, widgetY, widgetW, widgetH, radius, widget.fillColor, 1f);
            images.fillRoundedRect(context.batch(), widgetX + 5f, widgetY + widgetH - 8f, Math.max(0f, widgetW - 10f), 3f, 1.5f, CARD_HIGHLIGHT, 1f);
            images.fillRoundedRect(context.batch(), widgetX + 5f, widgetY + 3f, Math.max(0f, widgetW - 10f), 2f, 1f, CARD_EDGE, 1f);
        }

        if (!card) {
            if (icon != null && context.batch() != null) context.batch().draw(icon, widgetX, widgetY, size, size);
            text.label(context.binding(), context.batch(), context.font(), widget, context.key() + ":icon-label", widgetX + size + widget.gap, widgetY + size - 2f);
            return;
        }

        float padX = Math.max(8f, Math.min(12f, widgetH * 0.30f));
        float iconX = widgetX + padX;
        float iconY = widgetY + Math.max(0f, (widgetH - size) * 0.5f);
        if (icon != null && context.batch() != null) context.batch().draw(icon, iconX, iconY, size, size);
        float labelX = iconX + size + Math.max(4f, widget.gap);
        float labelW = Math.max(0f, widgetX + widgetW - labelX - padX);
        text.label(context.binding(), context.batch(), context.font(), widget, context.key() + ":icon-label", new UiRect(labelX, widgetY, labelW, widgetH));
    }
}
