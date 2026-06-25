package dev.takesome.helix.ui.binding;


import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Runtime registry of generic UI binding descriptors keyed by target path. */
public final class UiBindingRegistry {
    public static final String MANUAL_NAMESPACE = "manual";

    private final Map<String, UiBindingDescriptor> byTarget = new LinkedHashMap<>();
    private final Map<String, UiBindingDescriptor> byId = new LinkedHashMap<>();
    private final Map<String, String> namespacesByTarget = new LinkedHashMap<>();
    private final Map<String, String> targetsById = new LinkedHashMap<>();

    public UiBindingRegistry register(UiBindingDescriptor descriptor) {
        return register(MANUAL_NAMESPACE, descriptor);
    }

    public UiBindingRegistry register(String namespace, UiBindingDescriptor descriptor) {
        if (descriptor == null || !descriptor.enabled()) return this;
        put(normalizeNamespace(namespace), descriptor, byTarget, byId, namespacesByTarget, targetsById);
        return this;
    }

    public UiBindingRegistry registerAll(String namespace, Collection<UiBindingDescriptor> descriptors) {
        if (descriptors == null) return this;
        for (UiBindingDescriptor descriptor : descriptors) register(namespace, descriptor);
        return this;
    }

    public UiBindingRegistry reloadNamespace(String namespace, Collection<UiBindingDescriptor> descriptors) {
        String ns = normalizeNamespace(namespace);
        LinkedHashMap<String, UiBindingDescriptor> nextTargets = new LinkedHashMap<>(byTarget);
        LinkedHashMap<String, UiBindingDescriptor> nextIds = new LinkedHashMap<>(byId);
        LinkedHashMap<String, String> nextNamespaces = new LinkedHashMap<>(namespacesByTarget);
        LinkedHashMap<String, String> nextTargetsById = new LinkedHashMap<>(targetsById);
        removeNamespace(ns, nextTargets, nextIds, nextNamespaces, nextTargetsById);
        if (descriptors != null) {
            for (UiBindingDescriptor descriptor : descriptors) {
                if (descriptor == null || !descriptor.enabled()) continue;
                put(ns, descriptor, nextTargets, nextIds, nextNamespaces, nextTargetsById);
            }
        }
        byTarget.clear();
        byTarget.putAll(nextTargets);
        byId.clear();
        byId.putAll(nextIds);
        namespacesByTarget.clear();
        namespacesByTarget.putAll(nextNamespaces);
        targetsById.clear();
        targetsById.putAll(nextTargetsById);
        return this;
    }

    public UiBindingRegistry unregisterNamespace(String namespace) {
        String ns = normalizeNamespace(namespace);
        removeNamespace(ns, byTarget, byId, namespacesByTarget, targetsById);
        return this;
    }

    public Optional<UiBindingDescriptor> findTarget(String target) {
        if (target == null || target.isBlank()) return Optional.empty();
        return Optional.ofNullable(byTarget.get(normalize(target)));
    }

    public Optional<UiBindingDescriptor> findId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(byId.get(normalize(id)));
    }

    public Set<String> targets() {
        return Collections.unmodifiableSet(byTarget.keySet());
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    public Set<String> namespaces() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(namespacesByTarget.values()));
    }

    public Set<String> targetsInNamespace(String namespace) {
        String ns = normalizeNamespace(namespace);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : namespacesByTarget.entrySet()) {
            if (ns.equals(entry.getValue())) out.add(entry.getKey());
        }
        return Collections.unmodifiableSet(out);
    }

    public String namespaceOf(String target) {
        if (target == null || target.isBlank()) return "";
        return namespacesByTarget.getOrDefault(normalize(target), "");
    }

    public static String normalize(String value) {
        return lowerTrimToEmpty(value, java.util.Locale.ROOT).replace('_', '-');
    }

    public static String normalizeNamespace(String namespace) {
        String normalized = normalize(namespace);
        return normalized.isBlank() ? MANUAL_NAMESPACE : normalized;
    }

    private static void put(
            String namespace,
            UiBindingDescriptor descriptor,
            Map<String, UiBindingDescriptor> byTarget,
            Map<String, UiBindingDescriptor> byId,
            Map<String, String> namespacesByTarget,
            Map<String, String> targetsById
    ) {
        String target = normalize(descriptor.target());
        if (target.isBlank()) return;
        String owner = namespacesByTarget.get(target);
        if (owner != null && !owner.equals(namespace)) {
            throw new IllegalArgumentException("UI binding target `" + target + "` is already owned by namespace `" + owner + "`");
        }
        String id = normalize(descriptor.id());
        String existingTarget = targetsById.get(id);
        if (existingTarget != null && !existingTarget.equals(target)) {
            throw new IllegalArgumentException("UI binding id `" + id + "` already points to target `" + existingTarget + "`");
        }
        byTarget.put(target, descriptor);
        byId.put(id, descriptor);
        namespacesByTarget.put(target, namespace);
        targetsById.put(id, target);
    }

    private static void removeNamespace(
            String namespace,
            Map<String, UiBindingDescriptor> byTarget,
            Map<String, UiBindingDescriptor> byId,
            Map<String, String> namespacesByTarget,
            Map<String, String> targetsById
    ) {
        ArrayList<String> removedTargets = new ArrayList<>();
        for (Map.Entry<String, String> entry : namespacesByTarget.entrySet()) {
            if (namespace.equals(entry.getValue())) removedTargets.add(entry.getKey());
        }
        for (String target : removedTargets) {
            namespacesByTarget.remove(target);
            byTarget.remove(target);
        }
        ArrayList<String> removedIds = new ArrayList<>();
        for (Map.Entry<String, String> entry : targetsById.entrySet()) {
            if (removedTargets.contains(entry.getValue())) removedIds.add(entry.getKey());
        }
        for (String id : removedIds) {
            targetsById.remove(id);
            byId.remove(id);
        }
    }
}
