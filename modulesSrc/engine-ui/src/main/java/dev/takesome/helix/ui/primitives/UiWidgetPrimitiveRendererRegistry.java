package dev.takesome.helix.ui.primitives;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import dev.takesome.helix.ui.legacy.render.UiWidgetRenderContext;

/** Registry for data-selected widget primitive renderers. */
public final class UiWidgetPrimitiveRendererRegistry {
    public static final String MANUAL_NAMESPACE = "manual";

    private final Map<String, UiWidgetPrimitiveRenderer> renderers = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();
    private final Map<String, String> namespaces = new LinkedHashMap<>();

    public UiWidgetPrimitiveRendererRegistry register(String id, UiWidgetPrimitiveRenderer renderer) {
        return register(MANUAL_NAMESPACE, id, renderer);
    }

    public UiWidgetPrimitiveRendererRegistry register(String namespace, String id, UiWidgetPrimitiveRenderer renderer) {
        String key = normalize(id);
        if (key.isBlank() || renderer == null) return this;
        put(normalizeNamespace(namespace), key, renderer, renderers, aliases, namespaces, null);
        return this;
    }

    public UiWidgetPrimitiveRendererRegistry register(String namespace, UiWidgetPrimitiveDescriptor descriptor) {
        if (descriptor == null) return this;
        String ns = normalizeNamespace(namespace);
        put(ns, descriptor.id(), descriptor.renderer(), renderers, aliases, namespaces, null);
        for (String alias : descriptor.aliases()) {
            put(ns, normalize(alias), descriptor.renderer(), renderers, aliases, namespaces, descriptor.id());
        }
        return this;
    }

    public UiWidgetPrimitiveRendererRegistry registerAll(String namespace, Collection<UiWidgetPrimitiveDescriptor> descriptors) {
        if (descriptors == null) return this;
        for (UiWidgetPrimitiveDescriptor descriptor : descriptors) register(namespace, descriptor);
        return this;
    }

    public UiWidgetPrimitiveRendererRegistry alias(String alias, String target) {
        String targetKey = normalize(target);
        UiWidgetPrimitiveRenderer renderer = renderers.get(targetKey);
        if (renderer == null) return this;
        return alias(namespaces.getOrDefault(targetKey, MANUAL_NAMESPACE), alias, target);
    }

    public UiWidgetPrimitiveRendererRegistry alias(String namespace, String alias, String target) {
        String targetKey = normalize(target);
        UiWidgetPrimitiveRenderer renderer = renderers.get(targetKey);
        if (renderer == null) return this;
        put(normalizeNamespace(namespace), normalize(alias), renderer, renderers, aliases, namespaces, targetKey);
        return this;
    }

    public UiWidgetPrimitiveRendererRegistry reloadNamespace(String namespace, Collection<UiWidgetPrimitiveDescriptor> descriptors) {
        String ns = normalizeNamespace(namespace);
        LinkedHashMap<String, UiWidgetPrimitiveRenderer> nextRenderers = new LinkedHashMap<>(renderers);
        LinkedHashMap<String, String> nextAliases = new LinkedHashMap<>(aliases);
        LinkedHashMap<String, String> nextNamespaces = new LinkedHashMap<>(namespaces);
        removeNamespace(ns, nextRenderers, nextAliases, nextNamespaces);
        if (descriptors != null) {
            for (UiWidgetPrimitiveDescriptor descriptor : descriptors) {
                if (descriptor == null) continue;
                put(ns, descriptor.id(), descriptor.renderer(), nextRenderers, nextAliases, nextNamespaces, null);
                for (String alias : descriptor.aliases()) {
                    put(ns, normalize(alias), descriptor.renderer(), nextRenderers, nextAliases, nextNamespaces, descriptor.id());
                }
            }
        }
        renderers.clear();
        renderers.putAll(nextRenderers);
        aliases.clear();
        aliases.putAll(nextAliases);
        namespaces.clear();
        namespaces.putAll(nextNamespaces);
        return this;
    }

    public UiWidgetPrimitiveRendererRegistry unregisterNamespace(String namespace) {
        String ns = normalizeNamespace(namespace);
        removeNamespace(ns, renderers, aliases, namespaces);
        return this;
    }

    public Optional<UiWidgetPrimitiveRenderer> find(String id) {
        String key = normalize(id);
        if (key.isBlank()) return Optional.empty();
        return Optional.ofNullable(renderers.get(key));
    }

    public boolean render(String id, UiWidgetRenderContext context) {
        Optional<UiWidgetPrimitiveRenderer> renderer = find(id);
        renderer.ifPresent(value -> value.render(context));
        return renderer.isPresent();
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(renderers.keySet());
    }

    public Map<String, String> aliases() {
        return Collections.unmodifiableMap(aliases);
    }

    public Set<String> namespaces() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(namespaces.values()));
    }

    public Set<String> idsInNamespace(String namespace) {
        String ns = normalizeNamespace(namespace);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (ns.equals(entry.getValue())) out.add(entry.getKey());
        }
        return Collections.unmodifiableSet(out);
    }

    public String namespaceOf(String id) {
        return namespaces.getOrDefault(normalize(id), "");
    }

    public static String normalize(String id) {
        return trimToEmpty(id).replace('_', '-').toLowerCase(java.util.Locale.ROOT);
    }

    public static String normalizeNamespace(String namespace) {
        String normalized = normalize(namespace);
        return normalized.isBlank() ? MANUAL_NAMESPACE : normalized;
    }

    private static void put(
            String namespace,
            String id,
            UiWidgetPrimitiveRenderer renderer,
            Map<String, UiWidgetPrimitiveRenderer> renderers,
            Map<String, String> aliases,
            Map<String, String> namespaces,
            String aliasTarget
    ) {
        if (id == null || id.isBlank() || renderer == null) return;
        String owner = namespaces.get(id);
        if (owner != null && !owner.equals(namespace)) {
            throw new IllegalArgumentException("Widget primitive id/alias `" + id + "` is already owned by namespace `" + owner + "`");
        }
        renderers.put(id, renderer);
        namespaces.put(id, namespace);
        if (aliasTarget != null && !aliasTarget.isBlank()) aliases.put(id, aliasTarget);
        else aliases.remove(id);
    }

    private static void removeNamespace(
            String namespace,
            Map<String, UiWidgetPrimitiveRenderer> renderers,
            Map<String, String> aliases,
            Map<String, String> namespaces
    ) {
        ArrayList<String> removed = new ArrayList<>();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (namespace.equals(entry.getValue())) removed.add(entry.getKey());
        }
        for (String id : removed) {
            renderers.remove(id);
            aliases.remove(id);
            namespaces.remove(id);
        }
    }
}
