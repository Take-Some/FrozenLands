package dev.takesome.helix.ui.primitives;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Validates widget primitive manifests before registry mutation. */
public final class UiWidgetPrimitiveManifestValidator {
    public void validate(UiWidgetPrimitiveManifest manifest, UiWidgetPrimitiveRendererRegistry registry) {
        ArrayList<String> errors = new ArrayList<>();
        if (manifest == null) throw new IllegalArgumentException("Widget primitive manifest must not be null");
        String namespace = clean(manifest.namespace());
        if (namespace.isBlank()) errors.add("manifest namespace must not be blank");
        if (!manifest.enabled()) {
            throwIfErrors(errors);
            return;
        }
        List<UiWidgetPrimitiveManifest.Entry> entries = manifest.entries();
        if (entries.isEmpty()) errors.add("manifest `" + namespace + "` must declare at least one primitive");

        Set<String> idsAndAliases = new LinkedHashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            UiWidgetPrimitiveManifest.Entry entry = entries.get(i);
            if (entry == null || !entry.enabled()) continue;
            validateEntry(namespace, i, entry, registry, idsAndAliases, errors);
        }
        throwIfErrors(errors);
    }

    private void validateEntry(
            String namespace,
            int index,
            UiWidgetPrimitiveManifest.Entry entry,
            UiWidgetPrimitiveRendererRegistry registry,
            Set<String> idsAndAliases,
            List<String> errors
    ) {
        String prefix = "manifest `" + namespace + "` primitive[" + index + "]";
        String id = UiWidgetPrimitiveRendererRegistry.normalize(entry.id);
        if (id.isBlank()) errors.add(prefix + " requires id");
        else addUnique(prefix + " id", id, idsAndAliases, errors);

        String rendererId = UiWidgetPrimitiveRendererRegistry.normalize(entry.rendererId());
        if (rendererId.isBlank()) errors.add(prefix + " requires renderer/target");
        else if (registry == null || registry.find(rendererId).isEmpty()) {
            errors.add(prefix + " references unknown renderer: " + rendererId);
        }

        for (String alias : entry.aliasList()) {
            String normalized = UiWidgetPrimitiveRendererRegistry.normalize(alias);
            if (normalized.isBlank()) {
                errors.add(prefix + " contains blank alias");
                continue;
            }
            addUnique(prefix + " alias", normalized, idsAndAliases, errors);
        }
    }

    private static void addUnique(String label, String value, Set<String> set, List<String> errors) {
        if (!set.add(value)) errors.add(label + " duplicates id/alias: " + value);
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }

    private static void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) throw new IllegalArgumentException("Invalid UI widget primitive manifest: " + String.join("; ", errors));
    }
}
