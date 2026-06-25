package dev.takesome.helix.ui.skin;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.skin.UiSkinDescriptor;
import dev.takesome.helix.ui.skin.UiSkinType;

import java.util.Locale;

/**
 * Declarative retained-UI skin descriptor produced from CSS, Lua or template data.
 *
 * <p>The descriptor deliberately contains only semantic type + source reference.
 * Backend-specific texture loading, alpha-grid parsing and draw batching stay in
 * the active {@link dev.takesome.helix.ui.render.UiRenderContext}.</p>
 */
public final class UiElementSkin {
    public enum Kind {
        IMAGE,
        NINE_SLICE,
        BUTTON,
        PANEL,
        RIBBON,
        THREE_SLICE
    }

    private final Kind kind;
    private final String source;
    private final int frame;
    private final UiSkinDescriptor descriptor;

    private UiElementSkin(Kind kind, String source, int frame, UiSkinDescriptor descriptor) {
        this.kind = kind == null ? Kind.IMAGE : kind;
        this.source = trimToEmpty(source);
        this.frame = Math.max(0, frame);
        this.descriptor = descriptor;
    }

    public static UiElementSkin of(String type, String source) {
        return of(type, source, 0);
    }

    public static UiElementSkin of(String type, String source, int frame) {
        return new UiElementSkin(kind(type), source, frame, null);
    }

    public static UiElementSkin of(UiSkinDescriptor descriptor) {
        if (descriptor == null) return null;
        return new UiElementSkin(kind(descriptor.type()), descriptor.source(), descriptor.frame(), descriptor);
    }

    public Kind kind() {
        return kind;
    }

    public String source() {
        return source;
    }

    public int frame() {
        return frame;
    }

    public UiSkinDescriptor descriptor() {
        return descriptor;
    }

    public boolean hasSource() {
        return !source.isBlank();
    }

    public boolean usesNineSlice() {
        return kind == Kind.NINE_SLICE || kind == Kind.BUTTON || kind == Kind.PANEL;
    }

    public boolean usesRibbonSheet() {
        return kind == Kind.RIBBON;
    }

    public boolean usesThreeSlice() {
        return kind == Kind.THREE_SLICE && descriptor != null && descriptor.usesThreeSlice();
    }

    public String cacheKey() {
        if (descriptor != null) return kind.name() + ":" + descriptor.id() + ":" + source + "#" + frame;
        return kind.name() + ":" + source + "#" + frame;
    }

    private static Kind kind(UiSkinType type) {
        if (type == null) return Kind.IMAGE;
        return switch (type) {
            case IMAGE -> Kind.IMAGE;
            case NINE_SLICE -> Kind.NINE_SLICE;
            case RIBBON -> Kind.RIBBON;
            case THREE_SLICE -> Kind.THREE_SLICE;
        };
    }

    private static Kind kind(String type) {
        if (type == null || type.isBlank()) return Kind.IMAGE;
        String normalized = type.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "button", "btn" -> Kind.BUTTON;
            case "panel", "window", "popup" -> Kind.PANEL;
            case "ribbon", "banner" -> Kind.RIBBON;
            case "three_slice", "threeslice", "3slice", "horizontal_slice", "horizontal" -> Kind.THREE_SLICE;
            case "nine_slice", "nineslice", "9slice", "slice" -> Kind.NINE_SLICE;
            case "image", "sprite", "texture" -> Kind.IMAGE;
            default -> Kind.IMAGE;
        };
    }
}
