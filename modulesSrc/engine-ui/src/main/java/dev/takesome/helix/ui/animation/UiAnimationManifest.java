package dev.takesome.helix.ui.animation;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Data-friendly animation pack manifest.
 *
 * A manifest declares animation effect implementation classes and aliases. The
 * registry loader turns these entries into runtime descriptors without hardcoded
 * registration branches in the pipeline.
 */
public final class UiAnimationManifest {
    public static final String DEFAULT_NAMESPACE = "default";

    /** Optional manifest id for diagnostics. */
    public String id;

    /** Registry namespace used for unload/reload semantics. Defaults to id. */
    public String namespace;

    /** Disabled manifests uninstall their namespace when loaded through the loader. */
    public Boolean enabled;

    /** Preferred field name for animation entries. */
    public List<Entry> animations;

    /** Compatibility field name for animation entries. */
    public List<Entry> effects;

    public String namespace() {
        if (namespace != null && !namespace.isBlank()) return namespace.trim();
        if (id != null && !id.isBlank()) return id.trim();
        return DEFAULT_NAMESPACE;
    }

    public boolean enabled() {
        return enabled == null || enabled;
    }

    public List<Entry> entries() {
        if (animations != null && !animations.isEmpty()) return Collections.unmodifiableList(animations);
        if (effects != null && !effects.isEmpty()) return Collections.unmodifiableList(effects);
        return List.of();
    }

    public static UiAnimationManifest of(String namespace, Entry... entries) {
        UiAnimationManifest manifest = new UiAnimationManifest();
        manifest.id = namespace;
        manifest.namespace = namespace;
        manifest.enabled = true;
        manifest.animations = entries == null ? List.of() : List.of(entries);
        return manifest;
    }

    public static Entry animation(String id, Class<? extends UiTextAnimationEffect> type, String... aliases) {
        return animation(id, type == null ? null : type.getName(), aliases);
    }

    public static Entry animation(String id, String className, String... aliases) {
        Entry entry = new Entry();
        entry.id = id;
        entry.className = className;
        entry.aliases = aliases == null ? List.of() : Arrays.asList(aliases);
        entry.enabled = true;
        return entry;
    }

    /** One manifest entry for one text animation effect. */
    public static final class Entry {
        /** Stable animation id. Must match the effect id when supplied. */
        public String id;

        /** Fully qualified implementation class name. */
        public String className;

        /** Alternative field name accepted by the loader. */
        public String implementation;

        /** Alternative field name accepted by the loader. */
        public String type;

        /** Optional aliases for JSON/markup usage. */
        public List<String> aliases = new ArrayList<>();

        /** Disabled entries are ignored. */
        public Boolean enabled;

        public boolean enabled() {
            return enabled == null || enabled;
        }

        public String implementationClassName() {
            if (className != null && !className.isBlank()) return className.trim();
            if (implementation != null && !implementation.isBlank()) return implementation.trim();
            return trimToEmpty(type);
        }

        public List<String> aliasList() {
            return aliases == null ? List.of() : List.copyOf(aliases);
        }
    }
}
