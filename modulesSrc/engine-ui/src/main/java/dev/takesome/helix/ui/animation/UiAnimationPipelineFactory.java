package dev.takesome.helix.ui.animation;

import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiThemeDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and reloads UI animation pipelines from document/theme declarations.
 */
public final class UiAnimationPipelineFactory {
    private UiAnimationPipelineFactory() {
    }

    public static UiAnimationPipeline fromDocument(UiDocument document) {
        UiAnimationRegistry registry = DefaultTextAnimations.registry();
        reloadInto(registry, document, new UiAnimationDescriptorLoader());
        return new UiAnimationPipeline(registry);
    }

    public static UiAnimationPipeline fromTheme(UiThemeDefinition theme) {
        UiAnimationRegistry registry = DefaultTextAnimations.registry();
        reloadInto(registry, theme, new UiAnimationDescriptorLoader());
        return new UiAnimationPipeline(registry);
    }

    public static UiAnimationRegistry reloadInto(UiAnimationRegistry registry, UiDocument document, UiAnimationDescriptorLoader loader) {
        UiAnimationRegistry target = registry == null ? new UiAnimationRegistry() : registry;
        UiAnimationDescriptorLoader actualLoader = loader == null ? new UiAnimationDescriptorLoader() : loader;
        if (document == null) return target;
        reloadInto(target, document.theme, actualLoader);
        reloadReferences(target, document.animationManifests, actualLoader);
        reloadPacks(target, document.animationPacks, actualLoader);
        return target;
    }

    public static UiAnimationRegistry reloadInto(UiAnimationRegistry registry, UiThemeDefinition theme, UiAnimationDescriptorLoader loader) {
        UiAnimationRegistry target = registry == null ? new UiAnimationRegistry() : registry;
        UiAnimationDescriptorLoader actualLoader = loader == null ? new UiAnimationDescriptorLoader() : loader;
        if (theme == null) return target;
        reloadReferences(target, theme.animationManifests, actualLoader);
        reloadPacks(target, theme.animationPacks, actualLoader);
        return target;
    }

    public static String signature(UiDocument document) {
        if (document == null) return "document:null";
        StringBuilder out = new StringBuilder("document:");
        appendTheme(out, document.theme);
        appendList(out, "doc.refs", document.animationManifests);
        appendManifests(out, "doc.packs", document.animationPacks);
        return out.toString();
    }

    public static String signature(UiThemeDefinition theme) {
        StringBuilder out = new StringBuilder("theme:");
        appendTheme(out, theme);
        return out.toString();
    }

    private static void reloadReferences(UiAnimationRegistry registry, List<String> references, UiAnimationDescriptorLoader loader) {
        if (references == null) return;
        for (String reference : references) {
            if (reference == null || reference.isBlank()) continue;
            loader.reloadReference(registry, reference);
        }
    }

    private static void reloadPacks(UiAnimationRegistry registry, List<UiAnimationManifest> packs, UiAnimationDescriptorLoader loader) {
        if (packs == null) return;
        for (UiAnimationManifest pack : packs) {
            if (pack == null) continue;
            loader.reload(registry, pack);
        }
    }

    private static void appendTheme(StringBuilder out, UiThemeDefinition theme) {
        if (theme == null) {
            out.append("theme:null;");
            return;
        }
        appendList(out, "theme.refs", theme.animationManifests);
        appendManifests(out, "theme.packs", theme.animationPacks);
    }

    private static void appendList(StringBuilder out, String label, List<String> values) {
        out.append(label).append('[');
        if (values != null) {
            for (String value : values) {
                if (value != null) out.append(value.trim()).append('|');
            }
        }
        out.append(']');
    }

    private static void appendManifests(StringBuilder out, String label, List<UiAnimationManifest> manifests) {
        out.append(label).append('[');
        if (manifests != null) {
            for (UiAnimationManifest manifest : manifests) {
                appendManifest(out, manifest);
            }
        }
        out.append(']');
    }

    private static void appendManifest(StringBuilder out, UiAnimationManifest manifest) {
        if (manifest == null) {
            out.append("null;");
            return;
        }
        out.append(manifest.namespace()).append(':').append(manifest.enabled()).append('{');
        for (UiAnimationManifest.Entry entry : manifest.entries()) {
            if (entry == null) continue;
            out.append(entry.id).append('@').append(entry.implementationClassName()).append(':').append(entry.enabled()).append(':');
            for (String alias : safeAliases(entry)) {
                out.append(alias).append(',');
            }
            out.append(';');
        }
        out.append('}');
    }

    private static List<String> safeAliases(UiAnimationManifest.Entry entry) {
        return entry == null ? List.of() : new ArrayList<>(entry.aliasList());
    }
}
