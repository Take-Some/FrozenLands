package dev.takesome.helix.ui.primitives;

import java.util.List;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiThemeDefinition;

/** Applies document/theme-declared widget primitive packs to a primitive renderer registry. */
public final class UiWidgetPrimitivePipelineFactory {
    private UiWidgetPrimitivePipelineFactory() {
    }

    public static UiWidgetPrimitiveRendererRegistry reloadInto(
            UiWidgetPrimitiveRendererRegistry registry,
            UiDocument document,
            UiWidgetPrimitiveRegistryLoader loader
    ) {
        UiWidgetPrimitiveRendererRegistry target = registry == null ? new UiWidgetPrimitiveRendererRegistry() : registry;
        UiWidgetPrimitiveRegistryLoader actualLoader = loader == null ? new UiWidgetPrimitiveRegistryLoader() : loader;
        if (document == null) return target;
        reloadInto(target, document.theme, actualLoader);
        reloadReferences(target, document.primitiveManifests, actualLoader);
        reloadPacks(target, document.primitivePacks, actualLoader);
        return target;
    }

    public static UiWidgetPrimitiveRendererRegistry reloadInto(
            UiWidgetPrimitiveRendererRegistry registry,
            UiThemeDefinition theme,
            UiWidgetPrimitiveRegistryLoader loader
    ) {
        UiWidgetPrimitiveRendererRegistry target = registry == null ? new UiWidgetPrimitiveRendererRegistry() : registry;
        UiWidgetPrimitiveRegistryLoader actualLoader = loader == null ? new UiWidgetPrimitiveRegistryLoader() : loader;
        if (theme == null) return target;
        reloadReferences(target, theme.primitiveManifests, actualLoader);
        reloadPacks(target, theme.primitivePacks, actualLoader);
        return target;
    }

    public static String signature(UiDocument document) {
        if (document == null) return "document:null";
        StringBuilder out = new StringBuilder("document:");
        appendTheme(out, document.theme);
        appendList(out, "doc.refs", document.primitiveManifests);
        appendManifests(out, "doc.packs", document.primitivePacks);
        return out.toString();
    }

    private static void reloadReferences(UiWidgetPrimitiveRendererRegistry registry, List<String> references, UiWidgetPrimitiveRegistryLoader loader) {
        if (references == null) return;
        for (String reference : references) {
            if (reference != null && !reference.isBlank()) loader.reloadReference(registry, reference);
        }
    }

    private static void reloadPacks(UiWidgetPrimitiveRendererRegistry registry, List<UiWidgetPrimitiveManifest> packs, UiWidgetPrimitiveRegistryLoader loader) {
        if (packs == null) return;
        for (UiWidgetPrimitiveManifest pack : packs) {
            if (pack != null) loader.reload(registry, pack);
        }
    }

    private static void appendTheme(StringBuilder out, UiThemeDefinition theme) {
        if (theme == null) {
            out.append("theme:null;");
            return;
        }
        appendList(out, "theme.refs", theme.primitiveManifests);
        appendManifests(out, "theme.packs", theme.primitivePacks);
    }

    private static void appendList(StringBuilder out, String label, List<String> values) {
        out.append(label).append('[');
        if (values != null) {
            for (String value : values) if (value != null) out.append(value.trim()).append('|');
        }
        out.append(']');
    }

    private static void appendManifests(StringBuilder out, String label, List<UiWidgetPrimitiveManifest> manifests) {
        out.append(label).append('[');
        if (manifests != null) {
            for (UiWidgetPrimitiveManifest manifest : manifests) appendManifest(out, manifest);
        }
        out.append(']');
    }

    private static void appendManifest(StringBuilder out, UiWidgetPrimitiveManifest manifest) {
        if (manifest == null) {
            out.append("null;");
            return;
        }
        out.append(manifest.namespace()).append(':').append(manifest.enabled()).append('{');
        for (UiWidgetPrimitiveManifest.Entry entry : manifest.entries()) {
            if (entry == null) continue;
            out.append(entry.id).append('@').append(entry.rendererId()).append(':').append(entry.enabled()).append(':');
            for (String alias : entry.aliasList()) out.append(alias).append(',');
            out.append(';');
        }
        out.append('}');
    }
}
