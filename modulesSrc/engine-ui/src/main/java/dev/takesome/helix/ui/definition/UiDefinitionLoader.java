package dev.takesome.helix.ui.definition;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.takesome.helix.data.gson.GsonDataLoader;
import dev.takesome.helix.ui.animation.UiAnimationPipeline;
import dev.takesome.helix.ui.animation.UiAnimationPipelineFactory;
import dev.takesome.helix.ui.binding.UiBindingPipelineFactory;
import dev.takesome.helix.ui.binding.UiBindingRegistry;

/** Loads data-driven UI documents through the shared Gson loader. */
public final class UiDefinitionLoader {
    private static final Gson GSON = new Gson();

    private UiDefinitionLoader() {}

    public static UiDocument load(String internalPath) {
        JsonObject root = GsonDataLoader.readObject(internalPath);
        UiDocument document = GSON.fromJson(root, UiDocument.class);
        return document == null ? new UiDocument() : document;
    }

    public static UiDocument loadOrDefault(String internalPath, UiDocument fallback) {
        try {
            return load(internalPath);
        } catch (RuntimeException ex) {
            return fallback == null ? new UiDocument() : fallback;
        }
    }

    public static UiAnimationPipeline loadAnimationPipeline(String internalPath) {
        return animationPipeline(load(internalPath));
    }

    public static UiAnimationPipeline animationPipeline(UiDocument document) {
        return UiAnimationPipelineFactory.fromDocument(document);
    }

    public static UiBindingRegistry loadBindingRegistry(String internalPath) {
        return bindingRegistry(load(internalPath));
    }

    public static UiBindingRegistry bindingRegistry(UiDocument document) {
        return UiBindingPipelineFactory.fromDocument(document);
    }
}
