package dev.takesome.helix.ui.primitives;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Data-friendly widget primitive renderer pack manifest. */
public final class UiWidgetPrimitiveManifest {
    public static final String DEFAULT_NAMESPACE = "default";

    public String id;
    public String namespace;
    public Boolean enabled;
    public List<Entry> primitives;
    public List<Entry> renderers;

    public String namespace() {
        if (namespace != null && !namespace.isBlank()) return namespace.trim();
        if (id != null && !id.isBlank()) return id.trim();
        return DEFAULT_NAMESPACE;
    }

    public boolean enabled() {
        return enabled == null || enabled;
    }

    public List<Entry> entries() {
        if (primitives != null && !primitives.isEmpty()) return Collections.unmodifiableList(primitives);
        if (renderers != null && !renderers.isEmpty()) return Collections.unmodifiableList(renderers);
        return List.of();
    }

    public static UiWidgetPrimitiveManifest of(String namespace, Entry... entries) {
        UiWidgetPrimitiveManifest manifest = new UiWidgetPrimitiveManifest();
        manifest.id = namespace;
        manifest.namespace = namespace;
        manifest.enabled = true;
        manifest.primitives = entries == null ? List.of() : List.of(entries);
        return manifest;
    }

    public static Entry primitive(String id, String renderer, String... aliases) {
        Entry entry = new Entry();
        entry.id = id;
        entry.renderer = renderer;
        entry.aliases = aliases == null ? List.of() : Arrays.asList(aliases);
        entry.enabled = true;
        return entry;
    }

    /** One data declaration for a primitive id mapped to an existing renderer id. */
    public static final class Entry {
        public String id;
        public String renderer;
        public String target;
        public List<String> aliases = new ArrayList<>();
        public Boolean enabled;

        public boolean enabled() {
            return enabled == null || enabled;
        }

        public String rendererId() {
            if (renderer != null && !renderer.isBlank()) return renderer.trim();
            if (target != null && !target.isBlank()) return target.trim();
            return trimToEmpty(id);
        }

        public List<String> aliasList() {
            return aliases == null ? List.of() : List.copyOf(aliases);
        }
    }
}
