package dev.takesome.helix.ui.frame;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;

/** One typed operation in a resolved UI frame. */
public final class UiFrameCommand {
    private final UiFrameCommandKind kind;
    private final UiRect rect;
    private final UiColor color;
    private final UiBoxShadow boxShadow;
    private final float strokeWidth;
    private final String text;
    private final float scale;
    private final TextAlign align;
    private final String fontId;
    private final UiIcon icon;
    private final String imageSource;
    private final float opacity;
    private final UiElementSkin element;
    private final UiColor tint;
    private final int depth;
    private final String label;

    private UiFrameCommand(UiFrameCommandKind kind, UiRect rect, UiColor color, UiBoxShadow boxShadow, float strokeWidth, String text, float scale, TextAlign align, String fontId, UiIcon icon, String imageSource, float opacity, UiElementSkin element, UiColor tint, int depth, String label) {
        this.kind = kind;
        this.rect = rect;
        this.color = color;
        this.boxShadow = boxShadow;
        this.strokeWidth = strokeWidth;
        this.text = emptyIfNull(text);
        this.scale = scale;
        this.align = align == null ? TextAlign.LEFT : align;
        this.fontId = emptyIfNull(fontId);
        this.icon = icon;
        this.imageSource = emptyIfNull(imageSource);
        this.opacity = Float.isFinite(opacity) ? Math.max(0f, Math.min(1f, opacity)) : 1f;
        this.element = element;
        this.tint = tint;
        this.depth = Math.max(0, depth);
        this.label = emptyIfNull(label);
    }

    public static UiFrameCommand fill(UiRect rect, UiColor color) { return new UiFrameCommand(UiFrameCommandKind.FILL, rect, color, null, 0f, "", 1f, TextAlign.LEFT, "", null, "", 1f, null, null, 0, ""); }
    public static UiFrameCommand stroke(UiRect rect, UiColor color, float width) { return new UiFrameCommand(UiFrameCommandKind.STROKE, rect, color, null, width, "", 1f, TextAlign.LEFT, "", null, "", 1f, null, null, 0, ""); }
    public static UiFrameCommand boxShadow(UiRect rect, UiBoxShadow shadow) { return new UiFrameCommand(UiFrameCommandKind.BOX_SHADOW, rect, null, shadow, 0f, "", 1f, TextAlign.LEFT, "", null, "", 1f, null, null, 0, ""); }
    public static UiFrameCommand text(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) { return new UiFrameCommand(UiFrameCommandKind.TEXT, rect, color, null, 0f, text, scale, align, fontId, null, "", 1f, null, null, 0, ""); }
    public static UiFrameCommand buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align) { return buttonText(text, rect, scale, color, align, ""); }
    public static UiFrameCommand buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) { return new UiFrameCommand(UiFrameCommandKind.BUTTON_TEXT, rect, color, null, 0f, text, scale, align, fontId, null, "", 1f, null, null, 0, ""); }
    public static UiFrameCommand icon(UiIcon icon, UiRect rect, float scale, UiColor color, TextAlign align) { return new UiFrameCommand(UiFrameCommandKind.ICON, rect, color, null, 0f, "", scale, align, "", icon, "", 1f, null, null, 0, ""); }
    public static UiFrameCommand image(String source, UiRect rect, float opacity) { return new UiFrameCommand(UiFrameCommandKind.IMAGE, rect, null, null, 0f, "", 1f, TextAlign.LEFT, "", null, source, opacity, null, null, 0, ""); }
    public static UiFrameCommand element(UiElementSkin element, UiRect rect, UiColor tint) { return new UiFrameCommand(UiFrameCommandKind.ELEMENT, rect, null, null, 0f, "", 1f, TextAlign.LEFT, "", null, "", 1f, element, tint, 0, ""); }
    public static UiFrameCommand traceNode(UiRect bounds, UiRect contentBounds, int depth, String label) { return new UiFrameCommand(UiFrameCommandKind.TRACE_NODE, contentBounds == null ? bounds : contentBounds, null, null, 0f, "", 1f, TextAlign.LEFT, "", null, "", 1f, null, null, depth, label); }

    public UiFrameCommandKind kind() { return kind; }
    public UiRect rect() { return rect; }
    public UiColor color() { return color; }
    public UiBoxShadow boxShadow() { return boxShadow; }
    public float strokeWidth() { return strokeWidth; }
    public String text() { return text; }
    public float scale() { return scale; }
    public TextAlign align() { return align; }
    public String fontId() { return fontId; }
    public UiIcon icon() { return icon; }
    public String imageSource() { return imageSource; }
    public float opacity() { return opacity; }
    public UiElementSkin element() { return element; }
    public UiColor tint() { return tint; }
    public int depth() { return depth; }
    public String label() { return label; }
}
