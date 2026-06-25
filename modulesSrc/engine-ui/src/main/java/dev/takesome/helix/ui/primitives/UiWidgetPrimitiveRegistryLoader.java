package dev.takesome.helix.ui.primitives;

import com.google.gson.Gson;
import dev.takesome.helix.data.gson.GsonDataLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads widget primitive renderer manifests and applies namespace reload semantics. */
public final class UiWidgetPrimitiveRegistryLoader {
    private static final Gson GSON = new Gson();

    private final ClassLoader classLoader;
    private final UiWidgetPrimitiveManifestValidator validator = new UiWidgetPrimitiveManifestValidator();

    public UiWidgetPrimitiveRegistryLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public UiWidgetPrimitiveRegistryLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? UiWidgetPrimitiveRegistryLoader.class.getClassLoader() : classLoader;
    }

    public UiWidgetPrimitiveManifest loadPath(String path) {
        UiWidgetPrimitiveManifest manifest = GsonDataLoader.read(path, UiWidgetPrimitiveManifest.class);
        return manifest == null ? new UiWidgetPrimitiveManifest() : manifest;
    }

    public UiWidgetPrimitiveManifest loadResource(String resourcePath) {
        String normalized = normalizePath(resourcePath);
        if (normalized.isBlank()) throw new IllegalArgumentException("widget primitive manifest resource path must not be blank");
        try (InputStream input = classLoader.getResourceAsStream(normalized)) {
            if (input == null) throw new IllegalArgumentException("Widget primitive manifest resource not found: " + normalized);
            UiWidgetPrimitiveManifest manifest = GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), UiWidgetPrimitiveManifest.class);
            return manifest == null ? new UiWidgetPrimitiveManifest() : manifest;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to load widget primitive manifest resource: " + normalized, ex);
        }
    }

    public UiWidgetPrimitiveManifest loadJson(String json) {
        if (json == null || json.isBlank()) return new UiWidgetPrimitiveManifest();
        UiWidgetPrimitiveManifest manifest = GSON.fromJson(json, UiWidgetPrimitiveManifest.class);
        return manifest == null ? new UiWidgetPrimitiveManifest() : manifest;
    }

    public UiWidgetPrimitiveManifest loadReference(String reference) {
        String value = normalizePath(reference);
        if (value.startsWith("resource:")) return loadResource(value.substring("resource:".length()));
        if (value.startsWith("classpath:")) return loadResource(value.substring("classpath:".length()));
        if (value.startsWith("path:")) return loadPath(value.substring("path:".length()));
        if (value.startsWith("data:")) return loadPath(value.substring("data:".length()));
        return loadPath(value);
    }

    public List<UiWidgetPrimitiveDescriptor> descriptors(UiWidgetPrimitiveRendererRegistry registry, UiWidgetPrimitiveManifest manifest) {
        validator.validate(manifest, registry);
        ArrayList<UiWidgetPrimitiveDescriptor> descriptors = new ArrayList<>();
        if (!manifest.enabled()) return descriptors;
        for (UiWidgetPrimitiveManifest.Entry entry : manifest.entries()) {
            if (entry == null || !entry.enabled()) continue;
            UiWidgetPrimitiveRenderer renderer = registry.find(entry.rendererId()).orElseThrow();
            descriptors.add(new UiWidgetPrimitiveDescriptor(entry.id, renderer, entry.aliasList()));
        }
        return List.copyOf(descriptors);
    }

    public UiWidgetPrimitiveRendererRegistry reload(UiWidgetPrimitiveRendererRegistry registry, UiWidgetPrimitiveManifest manifest) {
        UiWidgetPrimitiveRendererRegistry target = registry == null ? new UiWidgetPrimitiveRendererRegistry() : registry;
        validator.validate(manifest, target);
        String namespace = manifest.namespace();
        if (!manifest.enabled()) return target.unregisterNamespace(namespace);
        return target.reloadNamespace(namespace, descriptors(target, manifest));
    }

    public UiWidgetPrimitiveRendererRegistry reloadPath(UiWidgetPrimitiveRendererRegistry registry, String path) {
        return reload(registry, loadPath(path));
    }

    public UiWidgetPrimitiveRendererRegistry reloadResource(UiWidgetPrimitiveRendererRegistry registry, String resourcePath) {
        return reload(registry, loadResource(resourcePath));
    }

    public UiWidgetPrimitiveRendererRegistry reloadReference(UiWidgetPrimitiveRendererRegistry registry, String reference) {
        return reload(registry, loadReference(reference));
    }

    private static String normalizePath(String value) {
        if (value == null) return "";
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }
}
