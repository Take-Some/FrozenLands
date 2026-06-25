package dev.takesome.helix.ui.binding;

import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiThemeDefinition;

import java.util.List;

/** Builds/reloads binding registries from document/theme declarations. */
public final class UiBindingPipelineFactory {
    private UiBindingPipelineFactory() {
    }

    public static UiBindingRegistry fromDocument(UiDocument document) {
        UiBindingRegistry registry = new UiBindingRegistry();
        reloadInto(registry, document, new UiBindingDescriptorLoader());
        return registry;
    }

    public static UiBindingRuntimeSource runtimeSource(UiDocument document, UiBindingSource source) {
        return new UiBindingRuntimeSource(source, fromDocument(document));
    }

    public static UiBindingRuntimeSource runtimeSource(UiDocument document, UiBindingSource source, EngineI18n i18n) {
        return new UiBindingRuntimeSource(source, fromDocument(document), i18n);
    }

    public static UiBindingRuntimeSource runtimeSource(UiDocument document, UiBindingSource source, UiBindingRegistry registry, UiBindingDescriptorLoader loader) {
        return runtimeSource(document, source, registry, loader, null);
    }

    public static UiBindingRuntimeSource runtimeSource(UiDocument document, UiBindingSource source, UiBindingRegistry registry, UiBindingDescriptorLoader loader, EngineI18n i18n) {
        UiBindingRegistry target = registry == null ? new UiBindingRegistry() : registry;
        reloadInto(target, document, loader == null ? new UiBindingDescriptorLoader() : loader);
        return new UiBindingRuntimeSource(source, target, i18n);
    }

    public static UiBindingRegistry reloadInto(UiBindingRegistry registry, UiDocument document, UiBindingDescriptorLoader loader) {
        UiBindingRegistry target = registry == null ? new UiBindingRegistry() : registry;
        UiBindingDescriptorLoader actualLoader = loader == null ? new UiBindingDescriptorLoader() : loader;
        if (document == null) return target;
        reloadInto(target, document.theme, actualLoader);
        reloadReferences(target, document.bindingManifests, actualLoader);
        reloadPacks(target, document.bindingPacks, actualLoader);
        return target;
    }

    public static UiBindingRegistry reloadInto(UiBindingRegistry registry, UiThemeDefinition theme, UiBindingDescriptorLoader loader) {
        UiBindingRegistry target = registry == null ? new UiBindingRegistry() : registry;
        UiBindingDescriptorLoader actualLoader = loader == null ? new UiBindingDescriptorLoader() : loader;
        if (theme == null) return target;
        reloadReferences(target, theme.bindingManifests, actualLoader);
        reloadPacks(target, theme.bindingPacks, actualLoader);
        return target;
    }

    public static String signature(UiDocument document) {
        if (document == null) return "document:null";
        StringBuilder out = new StringBuilder("document:");
        appendTheme(out, document.theme);
        appendList(out, "doc.refs", document.bindingManifests);
        appendManifests(out, "doc.packs", document.bindingPacks);
        return out.toString();
    }

    private static void reloadReferences(UiBindingRegistry registry, List<String> references, UiBindingDescriptorLoader loader) {
        if (references == null) return;
        for (String reference : references) {
            if (reference != null && !reference.isBlank()) loader.reloadReference(registry, reference);
        }
    }

    private static void reloadPacks(UiBindingRegistry registry, List<UiBindingManifest> packs, UiBindingDescriptorLoader loader) {
        if (packs == null) return;
        for (UiBindingManifest pack : packs) {
            if (pack != null) loader.reload(registry, pack);
        }
    }

    private static void appendTheme(StringBuilder out, UiThemeDefinition theme) {
        if (theme == null) {
            out.append("theme:null;");
            return;
        }
        appendList(out, "theme.refs", theme.bindingManifests);
        appendManifests(out, "theme.packs", theme.bindingPacks);
    }

    private static void appendList(StringBuilder out, String label, List<String> values) {
        out.append(label).append('[');
        if (values != null) {
            for (String value : values) if (value != null) out.append(value.trim()).append('|');
        }
        out.append(']');
    }

    private static void appendManifests(StringBuilder out, String label, List<UiBindingManifest> manifests) {
        out.append(label).append('[');
        if (manifests != null) {
            for (UiBindingManifest manifest : manifests) appendManifest(out, manifest);
        }
        out.append(']');
    }

    private static void appendManifest(StringBuilder out, UiBindingManifest manifest) {
        if (manifest == null) {
            out.append("null;");
            return;
        }
        out.append(manifest.namespace()).append(':').append(manifest.enabled()).append('{');
        for (UiBindingDescriptor descriptor : manifest.entries()) {
            if (descriptor == null) continue;
            out.append(descriptor.id()).append('@').append(descriptor.target()).append(':').append(descriptor.source()).append(':').append(descriptor.expression()).append(':').append(descriptor.type()).append(':').append(descriptor.enabled()).append(';');
        }
        out.append('}');
    }
}
