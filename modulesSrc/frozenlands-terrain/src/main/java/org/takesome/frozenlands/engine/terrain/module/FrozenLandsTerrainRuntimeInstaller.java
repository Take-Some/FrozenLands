package org.takesome.frozenlands.engine.terrain.module;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;
import org.takesome.frozenlands.engine.terrain.TerrainService;

public final class FrozenLandsTerrainRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 250;
    }

    @Override
    public String id() {
        return FrozenLandsTerrainModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        TerrainService service = context.findService(TerrainService.class)
                .orElseGet(() -> TerrainServiceFactory.create(context));
        context.registerService(TerrainService.class, service);
        context.registerService(FrozenLandsTerrainModule.ID, TerrainService.class, service);
        context.getModuleRegistry().register(new FrozenLandsTerrainModule(service), context);
        context.getLogger().info(
                "FrozenLands terrain service registered profile={} chunkSize={}",
                service.profile().id(),
                service.profile().chunkSize()
        );
    }
}
