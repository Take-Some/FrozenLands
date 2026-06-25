package org.takesome.frozenlands.engine;

import org.takesome.frozenlands.engine.core.CoreModule;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderRegistry;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;
import org.takesome.frozenlands.engine.providers.model.ModelProvider;
import org.takesome.frozenlands.engine.providers.sound.SoundProvider;

/** Builds the provider/runtime registries that Kernel exposes through EngineContext. */
public final class KernelProviderBootstrap {
    private final EngineContext context;
    private final ProviderRegistry providerRegistry;
    private final ModuleRegistry moduleRegistry;
    private final CoreModule coreModule;

    public KernelProviderBootstrap(
            EngineContext context,
            ProviderRegistry providerRegistry,
            ModuleRegistry moduleRegistry,
            CoreModule coreModule
    ) {
        this.context = context;
        this.providerRegistry = providerRegistry;
        this.moduleRegistry = moduleRegistry;
        this.coreModule = coreModule;
    }

    public KernelProviderRuntime boot() {
        moduleRegistry.register(coreModule, context);

        EngineProviders providers = EngineProviders.bootstrap(context, providerRegistry);
        moduleRegistry.register(EngineModule.descriptor("engine.providers", "Provider registry exposed to Lua bridge"), context);

        return new KernelProviderRuntime(
                providers,
                providers.getSoundProvider(),
                providers.getMaterialProvider(),
                providers.getModelProvider()
        );
    }

    public record KernelProviderRuntime(
            EngineProviders providers,
            SoundProvider soundProvider,
            MaterialProvider materialProvider,
            ModelProvider modelProvider
    ) {
    }
}
