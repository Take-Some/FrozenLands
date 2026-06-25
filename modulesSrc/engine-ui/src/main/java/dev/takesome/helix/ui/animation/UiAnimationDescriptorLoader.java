package dev.takesome.helix.ui.animation;

import com.google.gson.Gson;
import dev.takesome.helix.data.gson.GsonDataLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads UI animation manifests and converts them into registry descriptors.
 */
public final class UiAnimationDescriptorLoader {
    private static final Gson GSON = new Gson();

    private final ClassLoader classLoader;
    private final UiAnimationManifestValidator validator;

    public UiAnimationDescriptorLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public UiAnimationDescriptorLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? UiAnimationDescriptorLoader.class.getClassLoader() : classLoader;
        this.validator = new UiAnimationManifestValidator(this.classLoader);
    }

    public UiAnimationManifest loadPath(String path) {
        UiAnimationManifest manifest = GsonDataLoader.read(path, UiAnimationManifest.class);
        return manifest == null ? new UiAnimationManifest() : manifest;
    }

    public UiAnimationManifest loadResource(String resourcePath) {
        String normalized = normalizePath(resourcePath);
        if (normalized.isBlank()) throw new IllegalArgumentException("animation manifest resource path must not be blank");
        try (InputStream input = classLoader.getResourceAsStream(normalized)) {
            if (input == null) throw new IllegalArgumentException("Animation manifest resource not found: " + normalized);
            return loadJson(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to load animation manifest resource: " + normalized, ex);
        }
    }

    public UiAnimationManifest loadJson(String json) {
        if (json == null || json.isBlank()) return new UiAnimationManifest();
        UiAnimationManifest manifest = GSON.fromJson(json, UiAnimationManifest.class);
        return manifest == null ? new UiAnimationManifest() : manifest;
    }

    public UiAnimationManifest loadReference(String reference) {
        String value = normalizePath(reference);
        if (value.startsWith("resource:")) return loadResource(value.substring("resource:".length()));
        if (value.startsWith("classpath:")) return loadResource(value.substring("classpath:".length()));
        if (value.startsWith("path:")) return loadPath(value.substring("path:".length()));
        if (value.startsWith("data:")) return loadPath(value.substring("data:".length()));
        return loadPath(value);
    }

    private UiAnimationManifest loadJson(InputStreamReader reader) {
        UiAnimationManifest manifest = GSON.fromJson(reader, UiAnimationManifest.class);
        return manifest == null ? new UiAnimationManifest() : manifest;
    }

    public void validate(UiAnimationManifest manifest) {
        validator.validate(manifest);
    }

    public List<UiAnimationDescriptor> descriptors(UiAnimationManifest manifest) {
        validator.validate(manifest);
        ArrayList<UiAnimationDescriptor> descriptors = new ArrayList<>();
        if (!manifest.enabled()) return descriptors;
        for (UiAnimationManifest.Entry entry : manifest.entries()) {
            if (entry == null || !entry.enabled()) continue;
            descriptors.add(descriptor(entry));
        }
        return List.copyOf(descriptors);
    }

    public UiAnimationRegistry install(UiAnimationRegistry registry, UiAnimationManifest manifest) {
        return reload(registry, manifest);
    }

    public UiAnimationRegistry reload(UiAnimationRegistry registry, UiAnimationManifest manifest) {
        UiAnimationRegistry target = registry == null ? new UiAnimationRegistry() : registry;
        validator.validate(manifest);
        String namespace = manifest.namespace();
        if (!manifest.enabled()) return target.unregisterNamespace(namespace);
        return target.reloadNamespace(namespace, descriptors(manifest));
    }

    public UiAnimationRegistry uninstall(UiAnimationRegistry registry, UiAnimationManifest manifest) {
        UiAnimationRegistry target = registry == null ? new UiAnimationRegistry() : registry;
        return manifest == null ? target : target.unregisterNamespace(manifest.namespace());
    }

    public UiAnimationRegistry installPath(UiAnimationRegistry registry, String path) {
        return install(registry, loadPath(path));
    }

    public UiAnimationRegistry installResource(UiAnimationRegistry registry, String resourcePath) {
        return install(registry, loadResource(resourcePath));
    }

    public UiAnimationRegistry reloadPath(UiAnimationRegistry registry, String path) {
        return reload(registry, loadPath(path));
    }

    public UiAnimationRegistry reloadResource(UiAnimationRegistry registry, String resourcePath) {
        return reload(registry, loadResource(resourcePath));
    }

    public UiAnimationRegistry reloadReference(UiAnimationRegistry registry, String reference) {
        return reload(registry, loadReference(reference));
    }

    private UiAnimationDescriptor descriptor(UiAnimationManifest.Entry entry) {
        UiTextAnimationEffect effect = instantiate(entry.implementationClassName());
        if (entry.id != null && !entry.id.isBlank() && !UiAnimationRegistry.normalize(entry.id).equals(UiAnimationRegistry.normalize(effect.id()))) {
            throw new IllegalArgumentException("Animation manifest id `" + entry.id + "` does not match effect id `" + effect.id() + "`");
        }
        return new UiAnimationDescriptor(effect, entry.aliasList());
    }

    private UiTextAnimationEffect instantiate(String className) {
        if (className == null || className.isBlank()) throw new IllegalArgumentException("animation effect className is required");
        try {
            Class<?> raw = Class.forName(className.trim(), true, classLoader);
            if (!UiTextAnimationEffect.class.isAssignableFrom(raw)) {
                throw new IllegalArgumentException("Animation class does not implement UiTextAnimationEffect: " + className);
            }
            return (UiTextAnimationEffect) raw.getDeclaredConstructor().newInstance();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Failed to instantiate animation effect: " + className, ex);
        }
    }

    private static String normalizePath(String value) {
        if (value == null) return "";
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }
}
