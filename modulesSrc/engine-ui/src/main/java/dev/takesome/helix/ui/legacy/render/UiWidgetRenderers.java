package dev.takesome.helix.ui.legacy.render;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.layout.UiWidgetMeasure;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitivePipelineFactory;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveRegistryLoader;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveRendererRegistry;

public final class UiWidgetRenderers {
    private final UiWidgetImageRenderer images;
    private final UiTextRenderer text;
    private final UiProgressBarRenderer progress;
    private final UiWidgetPrimitiveRendererRegistry primitives;
    private final UiWidgetPrimitiveRegistryLoader primitiveLoader = new UiWidgetPrimitiveRegistryLoader();
    private String primitiveSignature = "";

    UiWidgetRenderers(UiNineSliceResolver resolver, UiWidgetImageRenderer images, UiTextRenderer text, UiProgressBarRenderer progress) {
        this.images = images;
        this.text = text;
        this.progress = progress;
        this.primitives = DefaultUiWidgetPrimitiveRenderers.registry(resolver, images, text, progress);
    }

    public void prepareFonts(UiDocument doc) {
        configurePrimitives(doc);
        text.prepareFonts(doc);
    }

    public void configurePrimitives(UiDocument doc) {
        String nextSignature = UiWidgetPrimitivePipelineFactory.signature(doc);
        if (!nextSignature.equals(primitiveSignature)) {
            UiWidgetPrimitivePipelineFactory.reloadInto(primitives, doc, primitiveLoader);
            primitiveSignature = nextSignature;
        }
    }

    public void resetEffects() {
        progress.reset();
    }

    public void setI18n(dev.takesome.helix.i18n.EngineI18n i18n) {
        text.setI18n(i18n);
    }

    public UiWidgetMeasure measure(UiBindingSource binding, BitmapFont font, UiWidgetDefinition widget) {
        if (widget == null) return UiWidgetMeasure.ZERO;
        return new UiWidgetMeasure(widget.w, widget.h);
    }

    public boolean render(UiBindingSource binding, SpriteBatch batch, BitmapFont font, UiRect panelRect, UiWidgetDefinition widget, String key, float uiTime) {
        return primitives.render(primitive(widget), new UiWidgetRenderContext(binding, batch, font, panelRect, widget, key, uiTime));
    }

    public String primitiveId(UiWidgetDefinition widget) {
        return primitive(widget);
    }

    public UiWidgetPrimitiveRendererRegistry primitiveRegistry() {
        return primitives;
    }

    public void fillRect(SpriteBatch batch, float x, float y, float w, float h, float[] rgba, float alpha) {
        images.fillRect(batch, x, y, w, h, rgba, alpha);
    }

    private String primitive(UiWidgetDefinition widget) {
        if (widget == null) return "";
        if (widget.primitive != null && !widget.primitive.isBlank()) return widget.primitive.trim();
        String type = trimToEmpty(widget.type);
        if (!type.isBlank() && primitives.find(type).isPresent()) return type;
        if (widget.progress != null || widget.base != null || widget.fill != null) return "bar";
        if (widget.icon != null) return "icon";
        if (widget.material != null) return "image";
        if (widget.fillColor != null) return "fill";
        if (widget.text != null || widget.i18nKey != null || widget.textKey != null) return "text";
        return type.isBlank() ? "text" : type;
    }
}
