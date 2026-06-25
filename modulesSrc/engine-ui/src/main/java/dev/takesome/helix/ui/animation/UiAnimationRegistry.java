package dev.takesome.helix.ui.animation;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Registry of named UI animation effects available to JSON UI documents. */
public final class UiAnimationRegistry {
    public static final String MANUAL_NAMESPACE = "manual";

    private final Map<String, UiTextAnimationEffect> effects = new LinkedHashMap<>();
    private final Map<String, String> namespaces = new LinkedHashMap<>();

    public UiAnimationRegistry register(UiTextAnimationEffect effect) {
        return register(MANUAL_NAMESPACE, effect);
    }

    public UiAnimationRegistry register(String namespace, UiTextAnimationEffect effect) {
        if (effect == null || effect.id() == null || effect.id().isBlank()) return this;
        put(normalizeNamespace(namespace), normalize(effect.id()), effect, effects, namespaces);
        return this;
    }

    public UiAnimationRegistry register(UiAnimationDescriptor descriptor) {
        return register(MANUAL_NAMESPACE, descriptor);
    }

    public UiAnimationRegistry register(String namespace, UiAnimationDescriptor descriptor) {
        if (descriptor == null) return this;
        String ns = normalizeNamespace(namespace);
        register(ns, descriptor.effect());
        for (String alias : descriptor.aliases()) {
            alias(ns, alias, descriptor.effect().id());
        }
        return this;
    }

    public UiAnimationRegistry registerAll(Collection<UiAnimationDescriptor> descriptors) {
        return registerAll(MANUAL_NAMESPACE, descriptors);
    }

    public UiAnimationRegistry registerAll(String namespace, Collection<UiAnimationDescriptor> descriptors) {
        if (descriptors == null) return this;
        for (UiAnimationDescriptor descriptor : descriptors) {
            register(namespace, descriptor);
        }
        return this;
    }

    public UiAnimationRegistry alias(String alias, String targetId) {
        if (alias == null || alias.isBlank() || targetId == null || targetId.isBlank()) return this;
        String targetKey = normalize(targetId);
        UiTextAnimationEffect target = effects.get(targetKey);
        if (target == null) return this;
        return alias(namespaces.getOrDefault(targetKey, MANUAL_NAMESPACE), alias, targetId);
    }

    public UiAnimationRegistry alias(String namespace, String alias, String targetId) {
        if (alias == null || alias.isBlank() || targetId == null || targetId.isBlank()) return this;
        String targetKey = normalize(targetId);
        UiTextAnimationEffect target = effects.get(targetKey);
        if (target == null) return this;
        put(normalizeNamespace(namespace), normalize(alias), target, effects, namespaces);
        return this;
    }

    public UiAnimationRegistry reloadNamespace(String namespace, Collection<UiAnimationDescriptor> descriptors) {
        String ns = normalizeNamespace(namespace);
        LinkedHashMap<String, UiTextAnimationEffect> nextEffects = new LinkedHashMap<>(effects);
        LinkedHashMap<String, String> nextNamespaces = new LinkedHashMap<>(namespaces);
        removeNamespace(ns, nextEffects, nextNamespaces);
        if (descriptors != null) {
            for (UiAnimationDescriptor descriptor : descriptors) {
                if (descriptor == null) continue;
                put(ns, normalize(descriptor.effect().id()), descriptor.effect(), nextEffects, nextNamespaces);
                for (String alias : descriptor.aliases()) {
                    put(ns, normalize(alias), descriptor.effect(), nextEffects, nextNamespaces);
                }
            }
        }
        effects.clear();
        effects.putAll(nextEffects);
        namespaces.clear();
        namespaces.putAll(nextNamespaces);
        return this;
    }

    public UiAnimationRegistry unregisterNamespace(String namespace) {
        String ns = normalizeNamespace(namespace);
        removeNamespace(ns, effects, namespaces);
        return this;
    }

    public Optional<UiTextAnimationEffect> find(String id) {
        return Optional.ofNullable(findOrNull(id));
    }

    public UiTextAnimationEffect findOrNull(String id) {
        if (id == null || id.isBlank()) return null;
        return effects.get(normalize(id));
    }

    public Collection<UiTextAnimationEffect> effects() {
        return Collections.unmodifiableCollection(new LinkedHashSet<>(effects.values()));
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(effects.keySet());
    }

    public Set<String> namespaces() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(namespaces.values()));
    }

    public Set<String> idsInNamespace(String namespace) {
        String ns = normalizeNamespace(namespace);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (ns.equals(entry.getValue())) ids.add(entry.getKey());
        }
        return Collections.unmodifiableSet(ids);
    }

    public String namespaceOf(String id) {
        if (id == null || id.isBlank()) return "";
        return namespaces.getOrDefault(normalize(id), "");
    }

    public static String normalize(String id) {
        return trimToEmpty(id).toLowerCase().replace('_', '-');
    }

    public static String normalizeNamespace(String namespace) {
        String normalized = normalize(namespace);
        return normalized.isBlank() ? MANUAL_NAMESPACE : normalized;
    }

    private static void put(
            String namespace,
            String id,
            UiTextAnimationEffect effect,
            Map<String, UiTextAnimationEffect> effects,
            Map<String, String> namespaces
    ) {
        if (id == null || id.isBlank()) return;
        String owner = namespaces.get(id);
        if (owner != null && !owner.equals(namespace)) {
            throw new IllegalArgumentException("Animation id/alias `" + id + "` is already owned by namespace `" + owner + "`");
        }
        effects.put(id, effect);
        namespaces.put(id, namespace);
    }

    private static void removeNamespace(
            String namespace,
            Map<String, UiTextAnimationEffect> effects,
            Map<String, String> namespaces
    ) {
        ArrayList<String> removed = new ArrayList<>();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (namespace.equals(entry.getValue())) removed.add(entry.getKey());
        }
        for (String key : removed) {
            namespaces.remove(key);
            effects.remove(key);
        }
    }
}
