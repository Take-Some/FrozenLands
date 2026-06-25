package dev.takesome.helix.ui.markup.internal.style;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.skin.UiSkinDescriptor;
import dev.takesome.helix.ui.skin.UiSkinRegistry;
import dev.takesome.helix.ui.skin.UiSkinResolver;
import dev.takesome.helix.ui.skin.UiSkinType;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves computed CSS skin references into engine UI element skins. */
public final class UiDomSkinResolver implements UiSkinResolver {
    private static final Logger LOG = EngineLog.logger(UiDomSkinResolver.class);
    private static final Set<String> WARNED_MISSING_DESCRIPTORS = ConcurrentHashMap.newKeySet();

    private final UiDomStyleReader reader;
    private final UiSkinRegistry registry;

    public UiDomSkinResolver(UiDomStyleReader reader, UiSkinRegistry registry) {
        this.reader = reader;
        this.registry = registry;
    }

    @Override
    public UiElementSkin resolve(Map<String, String> style, String defaultKind) {
        if (style == null || style.isEmpty()) return null;
        String raw = reader.first(style, "data-skin", "ui-skin", "skin");
        if (raw.isBlank()) return null;

        String value = cssUrlReference(raw);
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return null;

        if (registry != null) {
            UiSkinDescriptor descriptor = registry.find(value).orElse(null);
            if (descriptor != null) return UiElementSkin.of(descriptor);
        }

        UiSkinType requestedType = requestedType(style, defaultKind);
        if (requestedType == UiSkinType.THREE_SLICE && WARNED_MISSING_DESCRIPTORS.add(value)) {
            LOG.warn("UI three-slice skin descriptor not found id='{}'; falling back to raw source lookup", value);
        }
        String kind = explicitKind(requestedType, defaultKind);
        int frame = frame(style);
        return UiElementSkin.of(kind, value, frame);
    }

    private UiSkinType requestedType(Map<String, String> style, String fallback) {
        String raw = reader.first(style, "ui-skin-type", "skin-type", "skin-kind", "uiSkinType", "skinType", "kind");
        return UiSkinType.parse(raw, UiSkinType.parse(fallback, UiSkinType.IMAGE));
    }

    private String explicitKind(UiSkinType parsed, String fallback) {
        return switch (parsed) {
            case IMAGE -> "image";
            case NINE_SLICE -> fallbackKind(fallback, "panel");
            case RIBBON -> "ribbon";
            case THREE_SLICE -> "three-slice";
        };
    }

    private String fallbackKind(String fallback, String defaultValue) {
        return fallback == null || fallback.isBlank() ? defaultValue : fallback;
    }

    private int frame(Map<String, String> style) {
        int fallback = reader.integer(style, "frame", 0);
        fallback = reader.integer(style, "sprite-frame", fallback);
        return reader.integer(style, "ui-skin-frame", fallback);
    }

    private String cssUrlReference(String raw) {
        String value = unquote(raw);
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open > 0 && close > open && "url".equalsIgnoreCase(value.substring(0, open).trim())) {
            return unquote(value.substring(open + 1, close));
        }
        return value;
    }

    private String unquote(String raw) {
        String out = trimToEmpty(raw);
        if (out.length() >= 2 && ((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'")))) {
            return out.substring(1, out.length() - 1).trim();
        }
        return out;
    }
}
