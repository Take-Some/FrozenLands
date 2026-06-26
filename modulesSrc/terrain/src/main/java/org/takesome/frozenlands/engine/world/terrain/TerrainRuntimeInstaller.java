package org.takesome.frozenlands.engine.world.terrain;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

public final class TerrainRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 300;
    }

    @Override
    public String id() {
        return TerrainModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        TerrainManager terrainManager = new TerrainManager(context);
        context.getRootNode().attachChild(terrainManager.getTerrain());
        context.getRootNode().attachChild(terrainManager.getMountains());
        context.registerService(TerrainManager.class, terrainManager);
        context.getModuleRegistry().register(new TerrainModule(terrainManager), context);
    }
}
