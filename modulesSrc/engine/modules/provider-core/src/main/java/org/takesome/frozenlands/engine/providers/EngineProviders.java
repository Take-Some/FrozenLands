package org.takesome.frozenlands.engine.providers;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.modules.ProviderModuleAdapter;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;
import org.takesome.frozenlands.engine.providers.model.ModelProvider;
import org.takesome.frozenlands.engine.providers.sound.SoundProvider;

import java.util.Map;

public final class EngineProviders {
    public static final String SOUND_PROVIDER = "engine.sound";
    public static final String MATERIAL_PROVIDER = "engine.material";
    public static final String MODEL_PROVIDER = "engine.model";

    private final ProviderRegistry registry;
    private final SoundProvider soundProvider;
    private final MaterialProvider materialProvider;
    private final ModelProvider modelProvider;

    private EngineProviders(EngineContext context, ProviderRegistry registry) {
        this.registry = registry;
        this.soundProvider = (SoundProvider) registry.register(new SoundProvider(context), context);
        context.getModuleRegistry().register(new ProviderModuleAdapter(soundProvider), context);
        this.materialProvider = (MaterialProvider) registry.register(new MaterialProvider(context), context);
        context.getModuleRegistry().register(new ProviderModuleAdapter(materialProvider), context);
        this.modelProvider = (ModelProvider) registry.register(new ModelProvider(context), context);
        context.getModuleRegistry().register(new ProviderModuleAdapter(modelProvider), context);
    }

    public static EngineProviders bootstrap(EngineContext context, ProviderRegistry registry) {
        return new EngineProviders(context, registry);
    }

    public ProviderRegistry getRegistry() {
        return registry;
    }

    public SoundProvider getSoundProvider() {
        return soundProvider;
    }

    public MaterialProvider getMaterialProvider() {
        return materialProvider;
    }

    public ModelProvider getModelProvider() {
        return modelProvider;
    }

    public Map<String, Object> luaManifest() {
        return registry.luaManifest();
    }
}
