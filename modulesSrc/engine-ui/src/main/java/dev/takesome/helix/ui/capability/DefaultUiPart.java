package dev.takesome.helix.ui.capability;

import dev.takesome.helix.capability.CapabilityRegistry;
import dev.takesome.helix.capability.DefaultFeatureProvider;
import dev.takesome.helix.capability.EngineCapability;
import dev.takesome.helix.capability.EngineCapabilityContext;
import dev.takesome.helix.capability.RuntimeCapabilityProviderModule;
import dev.takesome.helix.capability.RuntimeCapabilityProviderModuleDescriptor;
import dev.takesome.helix.capability.EngineCapabilityProvider;
import dev.takesome.helix.capability.EngineFeatureProvider;
import dev.takesome.helix.ui.markup.provider.DefaultUiMarkupProvider;
import dev.takesome.helix.ui.markup.UiMarkupProvider;

import java.util.List;

public final class DefaultUiPart implements RuntimeCapabilityProviderModule {
    @Override public String id() { return "engine.ui.default"; }
    @Override public String version() { return "1.1.0"; }
    @Override public String description() { return "UI scene graph, rendering, widgets and markup capability."; }

    @Override public RuntimeCapabilityProviderModuleDescriptor descriptor() {
        return new RuntimeCapabilityProviderModuleDescriptor(
                id(),
                version(),
                description(),
                List.of(EngineUiCapabilities.FEATURE.id(), EngineUiCapabilities.MARKUP.id()),
                List.of(),
                List.of()
        );
    }

    @Override public void install(CapabilityRegistry registry) {
        registry.provide(new EngineCapabilityProvider<EngineFeatureProvider>() {
            @Override public String id() { return DefaultUiPart.this.id(); }
            @Override public String version() { return DefaultUiPart.this.version(); }
            @Override public String description() { return DefaultUiPart.this.description(); }
            @Override public EngineCapability<EngineFeatureProvider> capability() { return EngineUiCapabilities.FEATURE; }
            @Override public EngineFeatureProvider create(EngineCapabilityContext context) { return new DefaultFeatureProvider("engine.ui", "default"); }
            @Override public int priority() { return 100; }
        });

        registry.provide(new EngineCapabilityProvider<UiMarkupProvider>() {
            @Override public String id() { return "engine.ui.markup.default"; }
            @Override public String version() { return DefaultUiPart.this.version(); }
            @Override public String description() { return "HELIX UI Markup parser and compiler."; }
            @Override public EngineCapability<UiMarkupProvider> capability() { return EngineUiCapabilities.MARKUP; }
            @Override public UiMarkupProvider create(EngineCapabilityContext context) { return new DefaultUiMarkupProvider(); }
            @Override public int priority() { return 100; }
        });
    }
}
