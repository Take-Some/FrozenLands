package dev.takesome.helix.ui.uiComponents.label;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

/**
 * Text node for retained UI scenes.
 */
public class UiLabelNode extends UiComponent {
    private String text;
    private float scale;
    private UiColor color;
    private TextAlign align;
    private String fontId;
    private float paddingLeft;
    private float paddingRight;
    private float paddingTop;
    private float paddingBottom;
    private boolean clipText;
    private float clipBleedLeft = 8f;
    private float clipBleedRight;

    public UiLabelNode(String text) {
        this(text, 1f, UiColor.WHITE, TextAlign.LEFT);
    }

    public UiLabelNode(String text, float scale, UiColor color, TextAlign align) {
        this(text, scale, color, align, null);
    }

    public UiLabelNode(String text, float scale, UiColor color, TextAlign align, String fontId) {
        this.text = emptyIfNull(text);
        this.scale = scale;
        this.color = color == null ? UiColor.WHITE : color;
        this.align = align == null ? TextAlign.LEFT : align;
        this.fontId = normalizeFontId(fontId);
    }

    public String text() {
        return text;
    }

    public void setText(String text) {
        String next = emptyIfNull(text);

        if (this.text.equals(next)) return;

        this.text = next;
        markDirty();
    }

    public float scale() {
        return scale;
    }

    public void setScale(float scale) {
        if (this.scale == scale) return;

        this.scale = scale;
        markDirty();
    }

    public void setColor(UiColor color) {
        this.color = color == null ? UiColor.WHITE : color;
        markDirty();
    }

    public void setAlign(TextAlign align) {
        this.align = align == null ? TextAlign.LEFT : align;
        markDirty();
    }

    public String fontId() {
        return fontId;
    }

    public void setFontId(String fontId) {
        String next = normalizeFontId(fontId);
        if (this.fontId == null ? next == null : this.fontId.equals(next)) return;
        this.fontId = next;
        markDirty();
    }

    public void setPadding(float left, float right, float top, float bottom) {
        paddingLeft = safePadding(left);
        paddingRight = safePadding(right);
        paddingTop = safePadding(top);
        paddingBottom = safePadding(bottom);
        markDirty();
    }

    public void setClipText(boolean clipText) {
        if (this.clipText == clipText) return;
        this.clipText = clipText;
        markDirty();
    }

    public void setClipBleed(float left, float right) {
        clipBleedLeft = safePadding(left);
        clipBleedRight = safePadding(right);
        markDirty();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        UiRect drawBounds = paddedBounds();
        if (drawBounds.w <= 0.5f || drawBounds.h <= 0.5f) return;
        if (!clipText) {
            ctx.text(text, drawBounds, scale, color, align, fontId);
            return;
        }
        boolean clipped = ctx.pushClip(clipBounds(drawBounds));
        try {
            ctx.text(text, drawBounds, scale, color, align, fontId);
        } finally {
            if (clipped) ctx.popClip();
        }
    }

    private UiRect paddedBounds() {
        UiRect bounds = absoluteBounds();
        float x = bounds.x + paddingLeft;
        float y = bounds.y + paddingBottom;
        float w = Math.max(0f, bounds.w - paddingLeft - paddingRight);
        float h = Math.max(0f, bounds.h - paddingTop - paddingBottom);
        return new UiRect(x, y, w, h);
    }

    private UiRect clipBounds(UiRect bounds) {
        return new UiRect(
                bounds.x - clipBleedLeft,
                bounds.y,
                Math.max(0f, bounds.w + clipBleedLeft + clipBleedRight),
                bounds.h
        );
    }

    private float safePadding(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }

    private String normalizeFontId(String fontId) {
        if (fontId == null || fontId.isBlank()) return null;
        return fontId.trim();
    }
}
