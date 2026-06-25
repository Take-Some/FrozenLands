package dev.takesome.helix.ui.binding;

import com.google.gson.Gson;
import dev.takesome.helix.data.gson.GsonDataLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads UI binding manifests and installs them into a runtime registry. */
public final class UiBindingDescriptorLoader {
    private static final Gson GSON = new Gson();

    private final ClassLoader classLoader;
    private final UiBindingManifestValidator validator = new UiBindingManifestValidator();

    public UiBindingDescriptorLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public UiBindingDescriptorLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? UiBindingDescriptorLoader.class.getClassLoader() : classLoader;
    }

    public UiBindingManifest loadPath(String path) {
        UiBindingManifest manifest = GsonDataLoader.read(path, UiBindingManifest.class);
        return manifest == null ? new UiBindingManifest() : manifest;
    }

    public UiBindingManifest loadResource(String resourcePath) {
        String normalized = normalizePath(resourcePath);
        if (normalized.isBlank()) throw new IllegalArgumentException("binding manifest resource path must not be blank");
        try (InputStream input = classLoader.getResourceAsStream(normalized)) {
            if (input == null) throw new IllegalArgumentException("Binding manifest resource not found: " + normalized);
            UiBindingManifest manifest = GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), UiBindingManifest.class);
            return manifest == null ? new UiBindingManifest() : manifest;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to load binding manifest resource: " + normalized, ex);
        }
    }

    public UiBindingManifest loadJson(String json) {
        if (json == null || json.isBlank()) return new UiBindingManifest();
        UiBindingManifest manifest = GSON.fromJson(json, UiBindingManifest.class);
        return manifest == null ? new UiBindingManifest() : manifest;
    }

    public UiBindingManifest loadReference(String reference) {
        String value = normalizePath(reference);
        if (value.startsWith("resource:")) return loadResource(value.substring("resource:".length()));
        if (value.startsWith("classpath:")) return loadResource(value.substring("classpath:".length()));
        if (value.startsWith("path:")) return loadPath(value.substring("path:".length()));
        if (value.startsWith("data:")) return loadPath(value.substring("data:".length()));
        return loadPath(value);
    }

    public void validate(UiBindingManifest manifest) {
        validator.validate(manifest);
    }

    public List<UiBindingDescriptor> descriptors(UiBindingManifest manifest) {
        validator.validate(manifest);
        ArrayList<UiBindingDescriptor> out = new ArrayList<>();
        if (!manifest.enabled()) return out;
        for (UiBindingDescriptor descriptor : manifest.entries()) {
            if (descriptor != null && descriptor.enabled()) out.add(descriptor);
        }
        return List.copyOf(out);
    }

    public UiBindingRegistry install(UiBindingRegistry registry, UiBindingManifest manifest) {
        return reload(registry, manifest);
    }

    public UiBindingRegistry reload(UiBindingRegistry registry, UiBindingManifest manifest) {
        UiBindingRegistry target = registry == null ? new UiBindingRegistry() : registry;
        validator.validate(manifest);
        String namespace = manifest.namespace();
        if (!manifest.enabled()) return target.unregisterNamespace(namespace);
        return target.reloadNamespace(namespace, descriptors(manifest));
    }

    public UiBindingRegistry reloadReference(UiBindingRegistry registry, String reference) {
        return reload(registry, loadReference(reference));
    }

    public UiBindingRegistry reloadPath(UiBindingRegistry registry, String path) {
        return reload(registry, loadPath(path));
    }

    public UiBindingRegistry reloadResource(UiBindingRegistry registry, String resourcePath) {
        return reload(registry, loadResource(resourcePath));
    }

    private static String normalizePath(String value) {
        if (value == null) return "";
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }
}
