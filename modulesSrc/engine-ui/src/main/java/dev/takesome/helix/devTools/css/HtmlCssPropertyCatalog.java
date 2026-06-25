package dev.takesome.helix.devTools.css;

import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssPropertyRegistry;
import dev.takesome.helix.ui.css.UiCssPropertySpec;
import dev.takesome.helix.ui.css.UiCssValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dynamic CSS property catalog backed by the engine-ui CSS property registry. */
public final class HtmlCssPropertyCatalog {
    private final UiCssPropertyRegistry registry;
    private final UiCssParseContext parseContext = new UiCssParseContext();

    public HtmlCssPropertyCatalog(UiCssPropertyRegistry registry) {
        this.registry = registry == null ? UiCssPropertyRegistry.loadBuiltins() : registry;
    }

    public static HtmlCssPropertyCatalog builtins() {
        return new HtmlCssPropertyCatalog(UiCssPropertyRegistry.loadBuiltins());
    }

    public List<HtmlCssPropertyDescriptor> definitions() {
        ArrayList<HtmlCssPropertyDescriptor> out = new ArrayList<>();
        for (UiCssPropertySpec spec : registry.definitions()) out.add(descriptor(spec));
        out.sort(Comparator.comparing(HtmlCssPropertyDescriptor::name));
        return List.copyOf(out);
    }

    public List<HtmlCssPropertySnapshot> declared(Map<String, String> style) {
        if (style == null || style.isEmpty()) return List.of();
        LinkedHashMap<String, HtmlCssPropertySnapshot> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : style.entrySet()) {
            String name = entry.getKey() == null ? "" : entry.getKey().trim();
            if (name.isBlank()) continue;
            out.put(name, snapshot(name, entry.getValue(), "computed"));
        }
        return List.copyOf(out.values());
    }

    public List<HtmlCssPropertySnapshot> computed(Map<String, String> style) {
        LinkedHashMap<String, HtmlCssPropertySnapshot> out = new LinkedHashMap<>();
        Map<String, String> safeStyle = style == null ? Map.of() : style;

        for (UiCssPropertySpec spec : registry.definitions()) {
            String name = spec.name();
            String raw = firstValue(safeStyle, name, spec.aliases());
            out.put(name, snapshot(spec, raw, raw == null ? "initial" : "computed"));
        }

        for (Map.Entry<String, String> entry : safeStyle.entrySet()) {
            String name = entry.getKey() == null ? "" : entry.getKey().trim();
            if (name.isBlank()) continue;
            String canonical = canonical(name);
            out.putIfAbsent(canonical, snapshot(name, entry.getValue(), "unknown"));
        }
        return List.copyOf(out.values());
    }

    public HtmlCssPropertySnapshot snapshot(String propertyName, String rawValue, String origin) {
        String name = propertyName == null ? "" : propertyName.trim();
        return registry.find(name)
                .map(spec -> snapshot(spec, rawValue, origin))
                .orElseGet(() -> new HtmlCssPropertySnapshot(name, safe(rawValue), origin + " · not registered", false));
    }

    private HtmlCssPropertySnapshot snapshot(UiCssPropertySpec spec, String rawValue, String origin) {
        String raw = rawValue == null ? value(spec.initialValue()) : rawValue;
        try {
            UiCssValue parsed = spec.parse(parseContext, raw);
            return new HtmlCssPropertySnapshot(spec.name(), value(parsed), origin + " · " + spec.status().name().toLowerCase(), false);
        } catch (RuntimeException ex) {
            return new HtmlCssPropertySnapshot(spec.name(), safe(raw), "invalid · " + ex.getMessage(), false);
        }
    }

    private HtmlCssPropertyDescriptor descriptor(UiCssPropertySpec spec) {
        UiCssValue initial = spec.initialValue();
        Object value = initial == null ? null : initial.value();
        return new HtmlCssPropertyDescriptor(
                spec.name(),
                List.copyOf(spec.aliases()),
                value(initial),
                value == null ? "" : value.getClass().getSimpleName(),
                spec.status().name().toLowerCase(),
                spec.attributeFallback(),
                spec.replacement()
        );
    }

    private String firstValue(Map<String, String> style, String name, Iterable<String> aliases) {
        String direct = style.get(name);
        if (direct != null) return direct;
        if (aliases != null) {
            for (String alias : aliases) {
                String value = style.get(alias);
                if (value != null) return value;
            }
        }
        return null;
    }

    private String canonical(String name) {
        try {
            return registry.canonicalName(name);
        } catch (RuntimeException ignored) {
            return name == null ? "" : name.trim();
        }
    }

    private static String value(UiCssValue value) {
        return value == null || value.value() == null ? "" : String.valueOf(value.value());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
