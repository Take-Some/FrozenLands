package org.takesome.frozenlands.engine.world.terrain.gen;

import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings;
import org.takesome.frozenlands.engine.world.terrain.chunk.TerrainChunkTracker;
import org.takesome.frozenlands.engine.world.terrain.gen.mountains.MountGen;
import org.takesome.frozenlands.engine.world.terrain.gen.terrain.TerrainGen;

public class GenAdaptor {
    private final EngineContext app;
    private final TerrainChunkTracker chunkTracker;
    private final TerrainRuntimeSettings settings;
    private TerrainQuad distantTerrain;

    public GenAdaptor(EngineContext app, TerrainChunkTracker chunkTracker) {
        this.app = app;
        this.chunkTracker = chunkTracker;
        this.settings = new TerrainRuntimeSettings();
    }

    public TerrainQuad generateTerrain() {
        TerrainGen terrainGen = new TerrainGen(app, chunkTracker);
        return terrainGen.generateTerrain(settings.roughness(), settings.frequency(), settings.amplitude(),
                settings.lacunarity(), settings.octaves(), settings.noiseScale());
    }

    public TerrainQuad generateMountains() {
        MountGen mountGen = new MountGen(app);
        distantTerrain = mountGen.generateMountains();
        return distantTerrain;
    }

    public void update() {
        if (distantTerrain == null || app.getPlayer() == null) {
            return;
        }
        Vector3f playerLocation = app.getPlayer().getPlayerPosition();
        playerLocation.y = settings.mountainHeightOffset();
        distantTerrain.setLocalTranslation(playerLocation);
    }
}
