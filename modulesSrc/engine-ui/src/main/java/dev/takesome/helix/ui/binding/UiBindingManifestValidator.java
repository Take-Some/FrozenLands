package dev.takesome.helix.ui.binding;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Validates UI binding manifests before registry mutation. */
public final class UiBindingManifestValidator {
    private final UiBindingExpressionEvaluator expressionEvaluator = new UiBindingExpressionEvaluator();
    public void validate(UiBindingManifest manifest) {
        ArrayList<String> errors = new ArrayList<>();
        if (manifest == null) throw new IllegalArgumentException("UI binding manifest must not be null");
        String namespace = clean(manifest.namespace());
        if (namespace.isBlank()) errors.add("manifest namespace must not be blank");
        if (!manifest.enabled()) {
            throwIfErrors(errors);
            return;
        }
        List<UiBindingDescriptor> entries = manifest.entries();
        if (entries.isEmpty()) errors.add("manifest `" + namespace + "` must declare at least one binding");

        Set<String> ids = new LinkedHashSet<>();
        Set<String> targets = new LinkedHashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            UiBindingDescriptor descriptor = entries.get(i);
            if (descriptor == null || !descriptor.enabled()) continue;
            validateDescriptor(namespace, i, descriptor, ids, targets, errors);
        }
        throwIfErrors(errors);
    }

    private void validateDescriptor(
            String namespace,
            int index,
            UiBindingDescriptor descriptor,
            Set<String> ids,
            Set<String> targets,
            List<String> errors
    ) {
        String prefix = "manifest `" + namespace + "` binding[" + index + "]";
        String id = clean(descriptor.id());
        if (id.isBlank()) errors.add(prefix + " requires id or target");
        else addUnique(prefix + " id", id, ids, errors);

        String target = clean(descriptor.target());
        if (target.isBlank()) errors.add(prefix + " requires target");
        else {
            if (!target.contains(".")) errors.add(prefix + " target must have node.property form: " + target);
            addUnique(prefix + " target", target, targets, errors);
        }

        String type = clean(descriptor.type());
        if (type.isBlank()) errors.add(prefix + " requires type");
        boolean hasSource = !clean(descriptor.source()).isBlank();
        boolean hasExpression = !clean(descriptor.expression()).isBlank();
        if (!hasSource && !hasExpression && clean(descriptor.defaultValue()).isBlank()) {
            errors.add(prefix + " requires source, expr/expression or defaultValue");
        }
        if (hasSource && hasExpression) {
            errors.add(prefix + " cannot declare both source and expr/expression");
        }
        if (hasExpression) {
            try {
                expressionEvaluator.validate(descriptor.expression());
            } catch (RuntimeException ex) {
                errors.add(prefix + " has invalid expression: " + ex.getMessage());
            }
        }
    }

    private static void addUnique(String label, String value, Set<String> set, List<String> errors) {
        String normalized = UiBindingRegistry.normalize(value);
        if (!set.add(normalized)) errors.add(label + " duplicates: " + value);
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }

    private static void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) throw new IllegalArgumentException("Invalid UI binding manifest: " + String.join("; ", errors));
    }
}
