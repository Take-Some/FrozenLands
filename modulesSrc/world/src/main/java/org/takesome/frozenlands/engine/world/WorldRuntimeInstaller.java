package org.takesome.frozenlands.engine.world;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;
import org.takesome.frozenlands.engine.world.spawn.SpawnManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;

public final class WorldRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 600;
    }

    @Override
    public String id() {
        return WorldModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        TerrainManager terrainManager = context.requireService(TerrainManager.class);
        SpawnManager spawnManager = new SpawnManager(context, terrainManager);
        context.appStateManager().attach(spawnManager);
        context.registerService(SpawnManager.class, spawnManager);
        context.getModuleRegistry().register(new WorldModule(terrainManager, spawnManager), context);
    }
}
