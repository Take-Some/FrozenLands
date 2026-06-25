package dev.takesome.helix.ui.pipeline;


import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import dev.takesome.helix.fonts.render.FontGlyphRenderer;
import dev.takesome.helix.fonts.render.FontScalePolicy;
import dev.takesome.helix.fonts.render.FontTextMetrics;
import dev.takesome.helix.fonts.gdx.render.GdxFontGlyphRenderer;
import dev.takesome.helix.ui.layout.UiAnchor;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.layout.UiLayoutResolver;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.layout.UiPanelLayout;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.layout.UiWidgetMeasure;
import dev.takesome.helix.ui.binding.UiBindingRuntimeSource;
import dev.takesome.helix.ui.layout.UiInsets;
import dev.takesome.helix.ui.layout.UiProgressBarLayout;

import java.util.Locale;

/** Resolves panel rectangles and measured panel sizes before widget arrangement. */
public final class UiPanelMeasurePass {
    static final String LAYOUT_VBOX = "vbox";
    static final String LAYOUT_HBOX = "hbox";
    private static final float DEFAULT_TEXT_HEIGHT = 12f;
    private static final float DEFAULT_TEXT_WIDTH_PER_CHAR = 6f;
    private static final float DEFAULT_BAR_WIDTH = 120f;

    private final FontGlyphRenderer glyphs = new GdxFontGlyphRenderer();

    public UiRect resolvePanelRect(UiPanelDefinition panel, UiBindingSource binding, BitmapFont font, float viewportW, float viewportH) {
        if (!autoLayout(panel)) return panel.resolve(viewportW, viewportH);
        UiWidgetMeasure measured = measurePanel(panel, binding, font, viewportW, viewportH);
        return anchored(panel, measured.w(), measured.h(), viewportW, viewportH);
    }

    public UiWidgetMeasure measurePanel(UiPanelDefinition panel, UiBindingSource binding, BitmapFont font, float viewportW, float viewportH) {
        UiInsets padding = panelPadding(panel);
        UiWidgetMeasure content = measurePanelContent(panel, binding, font);
        float measuredW = content.w() + padding.left() + padding.right();
        float measuredH = content.h() + padding.bottom() + padding.top();
        return constrainPanelSize(panel, measuredW, measuredH, viewportW, viewportH);
    }

    public UiWidgetMeasure measurePanelContent(UiPanelDefinition panel, UiBindingSource binding, BitmapFont font) {
        String layout = layoutMode(panel);
        float gap = panelGap(panel);
        int count = 0;
        float contentW = 0f;
        float contentH = 0f;

        if (panel != null && panel.widgets != null) {
            for (int i = 0; i < panel.widgets.size(); i++) {
                UiWidgetDefinition widget = panel.widgets.get(i);
                if (widget == null || !UiPanelLayoutPass.visible(UiPanelLayoutPass.widgetTarget(widget, i, "visible"), widget.visible, binding)) continue;
                UiWidgetMeasure child = measureWidget(widget, binding, font);
                if (LAYOUT_HBOX.equals(layout)) {
                    if (count > 0) contentW += gap;
                    contentW += child.w();
                    contentH = Math.max(contentH, child.h());
                } else {
                    if (count > 0) contentH += gap;
                    contentH += child.h();
                    contentW = Math.max(contentW, child.w());
                }
                count++;
            }
        }

        return new UiWidgetMeasure(contentW, contentH);
    }

    public UiWidgetMeasure measureWidget(UiWidgetDefinition widget) {
        return measureWidget(widget, null, null);
    }

    public UiWidgetMeasure measureWidget(UiWidgetDefinition widget, UiBindingSource binding, BitmapFont font) {
        if (widget == null) return UiWidgetMeasure.ZERO;
        String primitive = primitive(widget);
        if ("bar".equals(primitive)) return measureBar(widget, binding, font);
        if ("text".equals(primitive) || hasText(widget)) return measureText(widget, binding, font);
        return new UiWidgetMeasure(positive(widget.w), positive(widget.h));
    }

    private UiWidgetMeasure measureText(UiWidgetDefinition widget, UiBindingSource binding, BitmapFont font) {
        float w = positive(widget.w);
        float h = positive(widget.h);
        if (w <= 0f) w = measureTextWidth(widget, binding, font);
        if (h <= 0f) h = measureTextHeight(widget, font);
        return new UiWidgetMeasure(w, h);
    }

    private UiWidgetMeasure measureBar(UiWidgetDefinition widget, UiBindingSource binding, BitmapFont font) {
        float w = positive(widget.w);
        float h = positive(widget.h);
        float labelHeight = widget.labelHeight > 0f ? widget.labelHeight : UiProgressBarLayout.DEFAULT_LABEL_HEIGHT;
        float labelGap = widget.labelGap >= 0f ? widget.labelGap : UiProgressBarLayout.DEFAULT_LABEL_GAP;
        float trackHeight = widget.trackHeight > 0f ? widget.trackHeight : UiProgressBarLayout.DEFAULT_TRACK_HEIGHT;

        if (w <= 0f) w = Math.max(DEFAULT_BAR_WIDTH, measureTextWidth(widget, binding, font) + 16f);
        if (h <= 0f) h = labelHeight + labelGap + trackHeight;
        return new UiWidgetMeasure(w, h);
    }

    private float measureTextWidth(UiWidgetDefinition widget, UiBindingSource binding, BitmapFont font) {
        String value = textValue(widget, binding);
        if (value.isEmpty()) return 0f;
        float scale = textScale(widget, font);
        if (font == null) return value.length() * DEFAULT_TEXT_WIDTH_PER_CHAR * (Float.isFinite(scale) && scale > 0f ? scale : 1f);
        FontTextMetrics metrics = glyphs.measure(font, value, scale, FontScalePolicy.EXACT);
        return Math.max(0f, metrics.width());
    }

    private float measureTextHeight(UiWidgetDefinition widget, BitmapFont font) {
        float scale = textScale(widget, font);
        if (font == null) return DEFAULT_TEXT_HEIGHT * (Float.isFinite(scale) && scale > 0f ? scale : 1f);
        return glyphs.lineHeight(font, scale, FontScalePolicy.EXACT);
    }

    private float textScale(UiWidgetDefinition widget, BitmapFont font) {
        if (widget == null) return 1f;
        if (widget.hasFontSize()) return Math.max(0.01f, widget.fontSize / basePixelSize(font));
        float scale = widget.scale;
        return Float.isFinite(scale) && scale > 0f ? scale : 1f;
    }

    private float basePixelSize(BitmapFont font) {
        if (font == null) return 16f;
        float lineHeight = font.getLineHeight();
        if (Float.isFinite(lineHeight) && lineHeight > 0f) return lineHeight;
        float capHeight = font.getCapHeight();
        return Float.isFinite(capHeight) && capHeight > 0f ? capHeight : 16f;
    }

    private String textValue(UiWidgetDefinition widget, UiBindingSource binding) {
        if (widget == null) return "";
        if (binding instanceof UiBindingRuntimeSource runtime && widget.id != null && !widget.id.isBlank()) {
            return runtime.textTarget(widget.id + ".text", widget.text);
        }
        return bind(widget.text, binding);
    }

    private String bind(String template, UiBindingSource binding) {
        if (template == null) return "";
        String out = template;
        int guard = 0;
        while (guard++ < 64) {
            int a = out.indexOf('{');
            int z = out.indexOf('}', a + 1);
            if (a < 0 || z < 0) break;
            String key = out.substring(a + 1, z).trim();
            String value = textOrEmpty(binding, item -> item.text(key));
            if (value == null || value.isEmpty()) value = "0";
            out = out.substring(0, a) + value + out.substring(z + 1);
        }
        return out;
    }

    private String primitive(UiWidgetDefinition widget) {
        String primitive = normalized(widget.primitive);
        if (!primitive.isEmpty()) return primitive;
        String type = normalized(widget.type);
        if ("text".equals(type) || "bar".equals(type) || "icon".equals(type) || "stretch".equals(type) || "image".equals(type) || "fill".equals(type)) {
            return type;
        }
        return "";
    }

    private boolean hasText(UiWidgetDefinition widget) {
        return widget != null && widget.text != null && !widget.text.isBlank();
    }

    private String normalized(String value) {
        return lowerTrimToEmpty(value, Locale.ROOT);
    }

    private float positive(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }

    public static boolean autoLayout(UiPanelDefinition panel) {
        String mode = layoutMode(panel);
        return LAYOUT_VBOX.equals(mode) || LAYOUT_HBOX.equals(mode);
    }

    public static String layoutMode(UiPanelDefinition panel) {
        if (panel == null) return "";
        String raw = panel.layout != null && !panel.layout.isBlank() ? panel.layout : panel.mode;
        if (raw == null || raw.isBlank()) return "";
        String key = raw.trim().replace("-", "").replace("_", "").toLowerCase(java.util.Locale.ROOT);
        if ("vbox".equals(key) || "vertical".equals(key) || "column".equals(key)) return LAYOUT_VBOX;
        if ("hbox".equals(key) || "horizontal".equals(key) || "row".equals(key)) return LAYOUT_HBOX;
        return "";
    }

    public static UiInsets panelPadding(UiPanelDefinition panel) {
        if (panel == null) return UiInsets.ZERO;
        return UiInsets.of(
                panel.paddingLeft >= 0f ? panel.paddingLeft : 0f,
                panel.paddingBottom >= 0f ? panel.paddingBottom : 0f,
                panel.paddingRight >= 0f ? panel.paddingRight : 0f,
                panel.paddingTop >= 0f ? panel.paddingTop : 0f
        );
    }

    public static float panelGap(UiPanelDefinition panel) {
        return panel != null && panel.gap >= 0f ? panel.gap : 0f;
    }

    private UiWidgetMeasure constrainPanelSize(UiPanelDefinition panel, float measuredW, float measuredH, float viewportW, float viewportH) {
        if (panel == null) return new UiWidgetMeasure(measuredW, measuredH);
        float minW = panel.minW > 0f ? panel.minW : Math.max(0f, panel.w);
        float minH = panel.minH > 0f ? panel.minH : Math.max(0f, panel.h);
        float maxW = panel.maxW > 0f ? panel.maxW : ratioLimit(panel.maxWidthRatio, viewportW);
        float maxH = panel.maxH > 0f ? panel.maxH : ratioLimit(panel.maxHeightRatio, viewportH);

        float w = Math.max(minW, measuredW);
        float h = Math.max(minH, measuredH);
        if (maxW > 0f) w = Math.min(w, Math.max(minW, maxW));
        if (maxH > 0f) h = Math.min(h, Math.max(minH, maxH));
        return new UiWidgetMeasure(w, h);
    }

    private UiRect anchored(UiPanelDefinition panel, float w, float h, float viewportW, float viewportH) {
        UiAnchor anchor = UiAnchor.parse(panel.anchor);
        return UiLayoutResolver.resolve(new UiPanelLayout(anchor, panel.x, panel.y, w, h, panel.mode), viewportW, viewportH);
    }

    private float ratioLimit(float ratio, float viewport) {
        if (!Float.isFinite(ratio) || ratio <= 0f || !Float.isFinite(viewport) || viewport <= 0f) return 0f;
        return viewport * MathUtils.clamp(ratio, 0f, 1f);
    }
}
