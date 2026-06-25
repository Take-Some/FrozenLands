package dev.takesome.helix.ui.animation;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates UI animation manifests before they mutate a registry namespace.
 */
public final class UiAnimationManifestValidator {
    private final ClassLoader classLoader;

    public UiAnimationManifestValidator() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public UiAnimationManifestValidator(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? UiAnimationManifestValidator.class.getClassLoader() : classLoader;
    }

    public void validate(UiAnimationManifest manifest) {
        ArrayList<String> errors = new ArrayList<>();
        if (manifest == null) {
            throw new IllegalArgumentException("Animation manifest must not be null");
        }

        String namespace = clean(manifest.namespace());
        if (namespace.isBlank()) errors.add("manifest namespace must not be blank");
        if (!manifest.enabled()) {
            throwIfErrors(errors);
            return;
        }

        List<UiAnimationManifest.Entry> entries = manifest.entries();
        if (entries.isEmpty()) errors.add("manifest `" + namespace + "` must declare at least one animation");

        Set<String> idsAndAliases = new LinkedHashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            UiAnimationManifest.Entry entry = entries.get(i);
            if (entry == null || !entry.enabled()) continue;
            validateEntry(namespace, i, entry, idsAndAliases, errors);
        }
        throwIfErrors(errors);
    }

    private void validateEntry(
            String namespace,
            int index,
            UiAnimationManifest.Entry entry,
            Set<String> idsAndAliases,
            List<String> errors
    ) {
        String prefix = "manifest `" + namespace + "` entry[" + index + "]";
        String id = clean(entry.id);
        if (id.isBlank()) {
            errors.add(prefix + " requires id");
        } else {
            addUnique(prefix + " id", id, idsAndAliases, errors);
        }

        String className = clean(entry.implementationClassName());
        if (className.isBlank()) {
            errors.add(prefix + " requires className/implementation/type");
        } else {
            validateClass(prefix, className, errors);
        }

        for (String alias : entry.aliasList()) {
            String normalized = clean(alias);
            if (normalized.isBlank()) {
                errors.add(prefix + " contains blank alias");
                continue;
            }
            addUnique(prefix + " alias", normalized, idsAndAliases, errors);
        }
    }

    private void validateClass(String prefix, String className, List<String> errors) {
        try {
            Class<?> raw = Class.forName(className, false, classLoader);
            if (!UiTextAnimationEffect.class.isAssignableFrom(raw)) {
                errors.add(prefix + " class does not implement UiTextAnimationEffect: " + className);
                return;
            }
            Constructor<?> constructor = raw.getDeclaredConstructor();
            if (!constructor.canAccess(null)) constructor.setAccessible(true);
        } catch (ClassNotFoundException ex) {
            errors.add(prefix + " class not found: " + className);
        } catch (NoSuchMethodException ex) {
            errors.add(prefix + " class must have a no-arg constructor: " + className);
        } catch (RuntimeException ex) {
            errors.add(prefix + " class is not accessible: " + className);
        }
    }

    private void addUnique(String label, String value, Set<String> idsAndAliases, List<String> errors) {
        String normalized = UiAnimationRegistry.normalize(value);
        if (!idsAndAliases.add(normalized)) {
            errors.add(label + " duplicates id/alias: " + value);
        }
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }

    private static void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid UI animation manifest: " + String.join("; ", errors));
        }
    }
}
