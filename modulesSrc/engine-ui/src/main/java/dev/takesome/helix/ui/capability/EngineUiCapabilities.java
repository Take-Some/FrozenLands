package dev.takesome.helix.ui.capability;

import dev.takesome.helix.capability.EngineCapability;
import dev.takesome.helix.capability.EngineFeatureProvider;
import dev.takesome.helix.capability.NoOpFeatureProvider;
import dev.takesome.helix.ui.markup.provider.NoOpUiMarkupProvider;
import dev.takesome.helix.ui.markup.UiMarkupProvider;

public final class EngineUiCapabilities {
    public static final EngineCapability<EngineFeatureProvider> FEATURE = EngineCapability.degraded(
            "engine.ui",
            EngineFeatureProvider.class,
            () -> new NoOpFeatureProvider("engine.ui")
    );

    public static final EngineCapability<UiMarkupProvider> MARKUP = EngineCapability.degraded(
            "engine.ui.markup",
            UiMarkupProvider.class,
            NoOpUiMarkupProvider::new
    );

    private EngineUiCapabilities() {}
}
