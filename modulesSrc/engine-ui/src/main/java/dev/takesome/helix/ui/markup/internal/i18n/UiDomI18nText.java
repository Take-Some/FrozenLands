package dev.takesome.helix.ui.markup.internal.i18n;


import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.i18n.I18nArgs;
import dev.takesome.helix.i18n.I18nKey;
import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.model.UiDomAttributes;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;

public final class UiDomI18nText {
    private static final Logger LOG = EngineLog.logger(UiDomI18nText.class);
    private static final Set<String> WARNED_KEYS = new LinkedHashSet<>();
    private UiDomI18nText() {
    }

    public static String textOrLocalized(UiDomElement element, String fallbackAttribute, EngineI18n i18n) {
        String key = UiDomAttributes.firstValue(element, "data-i18n-key", "i18n-key", "data-text-key", "text-key", "data-label-key", "label-key");
        if (!key.isBlank()) {
            if (i18n == null) {
                warnOnce("no-i18n|" + key, "UI i18n unavailable for key='{}' tag='{}'; using fallback", key, textOrEmpty(element, item -> item.tagName()));
            } else {
                String resolved = i18n.resolve(I18nKey.of(key), args(element));
                if (resolved != null && !resolved.isBlank() && !missingMarker(resolved, key)) return resolved;
                warnOnce("missing|" + key, "UI i18n missing key='{}' tag='{}' resolved='{}'; using fallback", key, textOrEmpty(element, item -> item.tagName()), resolved);
            }
        }
        String fallback = UiDomAttributes.textOrValue(element, fallbackAttribute);
        return fallback == null || fallback.isBlank() ? humanFallback(key) : fallback;
    }

    private static void warnOnce(String key, String message, Object... args) {
        synchronized (WARNED_KEYS) {
            if (WARNED_KEYS.add(key)) LOG.warn(message, args);
        }
    }

    private static boolean missingMarker(String resolved, String key) {
        if (resolved == null || key == null) return true;
        String value = resolved.trim();
        return value.equals(key) || value.equals("??" + key + "??") || (value.startsWith("??") && value.endsWith("??"));
    }

    private static String humanFallback(String key) {
        if (key == null || key.isBlank()) return "";
        String value = key;
        int dot = value.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < value.length()) value = value.substring(dot + 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i == 0) out.append(Character.toUpperCase(c));
            else if (Character.isUpperCase(c)) out.append(' ').append(c);
            else out.append(c);
        }
        return out.toString();
    }

    public static I18nArgs args(UiDomElement element) {
        String raw = UiDomAttributes.value(element, "i18n-args");
        if (raw.isBlank()) return I18nArgs.empty();
        I18nArgs.Builder builder = I18nArgs.builder();
        for (String token : raw.split("[;,]")) {
            int split = token.indexOf('=');
            if (split <= 0) continue;
            String key = token.substring(0, split).trim();
            String value = token.substring(split + 1).trim();
            if (!key.isBlank()) builder.put(key, value);
        }
        return builder.build();
    }
}
