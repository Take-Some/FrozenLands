package org.takesome.frozenlands.engine.terrain.module;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.terrain.TerrainService;

public final class TerrainServiceFactory {
    private TerrainServiceFactory() {
    }

    public static TerrainService create(EngineContext context) {
        return new GeneratedTerrainService(TerrainProfileLoader.load(context));
    }
}
