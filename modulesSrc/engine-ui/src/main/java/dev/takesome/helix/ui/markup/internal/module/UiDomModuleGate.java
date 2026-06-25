package dev.takesome.helix.ui.markup.internal.module;

import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.model.UiDomAttributes;

import java.io.File;
import java.util.Locale;

/** Optional module availability gate for markup fragments and menu contributions. */
public final class UiDomModuleGate {
    public boolean closed(UiDomElement element) {
        String required = firstAttr(element, "requires-module", "requiresModule", "if-module", "ifModule");
        if (required.isBlank()) return false;
        return !moduleJarPresent(required);
    }

    public String firstAttr(UiDomElement element, String... keys) {
        if (element == null || keys == null) return "";
        for (String key : keys) {
            String value = UiDomAttributes.value(element, key).trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private boolean moduleJarPresent(String requested) {
        String normalized = normalizeModuleName(requested);
        if (normalized.isBlank()) return false;
        File dir = new File(System.getProperty("helix.modules.dir", "modules"));
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file == null || !file.isFile()) continue;
            String candidate = normalizeModuleName(file.getName());
            if (moduleNameMatches(candidate, normalized)) return true;
        }
        return false;
    }

    private boolean moduleNameMatches(String candidate, String requested) {
        if (candidate.equals(requested)) return true;
        if (candidate.startsWith(requested + "-")) return true;
        if ("editor".equals(requested)) {
            return candidate.equals("editor")
                    || candidate.startsWith("editor-")
                    || candidate.equals("helix-editor")
                    || candidate.startsWith("helix-editor-");
        }
        return false;
    }

    private String normalizeModuleName(String value) {
        if (value == null) return "";
        String result = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (result.endsWith(".jar")) result = result.substring(0, result.length() - 4);
        if (result.endsWith("-all")) result = result.substring(0, result.length() - 4);
        return result;
    }
}
