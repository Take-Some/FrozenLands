package dev.takesome.helix.ui;

import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.definition.UiDefinitionLoader;
import dev.takesome.helix.ui.definition.UiDocument;

/**
 * Central public entry point for the engine-ui library.
 *
 * <p>Use this facade from the engine, game modules, editor tooling or
 * standalone LibGDX applications instead of manually wiring UI renderer
 * internals.</p>
 */
public final class EngineUi {
    private EngineUi() {}

    public static EngineUiConfig.Builder configure(AssetProvider assets) {
        return EngineUiConfig.builder(assets);
    }

    public static EngineUiRuntime create(AssetProvider assets) {
        return new EngineUiRuntime(EngineUiConfig.of(assets));
    }

    public static EngineUiRuntime create(AssetProvider assets, MaterialProvider materials) {
        return new EngineUiRuntime(EngineUiConfig.of(assets, materials));
    }

    public static EngineUiRuntime create(EngineUiConfig config) {
        return new EngineUiRuntime(config);
    }

    public static UiDocument load(String internalPath) {
        return UiDefinitionLoader.load(internalPath);
    }

    public static UiDocument loadOrEmpty(String internalPath) {
        return UiDefinitionLoader.loadOrDefault(internalPath, new UiDocument());
    }

    public static boolean hasPanels(UiDocument document) {
        return document != null && document.panels != null && !document.panels.isEmpty();
    }
}
