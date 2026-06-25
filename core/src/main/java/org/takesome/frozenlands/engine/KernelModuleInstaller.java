package org.takesome.frozenlands.engine;

import org.takesome.frozenlands.engine.core.console.ConsoleCursorPolicyState;
import org.takesome.frozenlands.engine.core.console.ConsoleInteractionPolicyState;
import org.takesome.frozenlands.engine.core.console.CoreConsoleState;
import org.takesome.frozenlands.engine.icons.IcoParserModule;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.player.PlayerModule;
import org.takesome.frozenlands.engine.save.SaveManager;
import org.takesome.frozenlands.engine.save.SaveModule;
import org.takesome.frozenlands.engine.shaders.ShaderModule;
import org.takesome.frozenlands.engine.world.WorldModule;
import org.takesome.frozenlands.engine.world.effect.ParticleModule;
import org.takesome.frozenlands.engine.world.terrain.TerrainModule;

/** Installs runtime EngineModule adapters after providers and world services exist. */
public final class KernelModuleInstaller {
    private final EngineContext context;
    private final ModuleRegistry moduleRegistry;

    public KernelModuleInstaller(EngineContext context, ModuleRegistry moduleRegistry) {
        this.context = context;
        this.moduleRegistry = moduleRegistry;
    }

    public KernelModuleRuntime install(KernelWorldBootstrap.KernelWorldRuntime worldRuntime) {
        moduleRegistry.register(new WorldModule(worldRuntime.terrainManager(), worldRuntime.spawnManager()), context);
        moduleRegistry.register(new TerrainModule(worldRuntime.terrainManager()), context);
        moduleRegistry.register(new ShaderModule(worldRuntime.shaders()), context);
        moduleRegistry.register(new ParticleModule(worldRuntime.worldUpdate().getParticleManager()), context);
        moduleRegistry.register(new PlayerModule(context), context);

        SaveManager saveManager = new SaveManager(context);
        moduleRegistry.register(new SaveModule(saveManager), context);
        moduleRegistry.register(new IcoParserModule(), context);
        context.appStateManager().attach(new ConsoleInteractionPolicyState(context));
        context.appStateManager().attach(new ConsoleCursorPolicyState(context));
        context.appStateManager().attach(new CoreConsoleState(context));
        return new KernelModuleRuntime(saveManager);
    }

    public record KernelModuleRuntime(SaveManager saveManager) {
    }
}
