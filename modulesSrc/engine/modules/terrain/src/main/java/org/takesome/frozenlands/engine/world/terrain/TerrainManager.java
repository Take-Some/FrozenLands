package org.takesome.frozenlands.engine.world.terrain;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.world.terrain.chunk.TerrainChunkTracker;
import org.takesome.frozenlands.engine.world.terrain.gen.GenAdaptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class TerrainManager {
    private final TerrainChunkTracker chunkTracker = new TerrainChunkTracker();
    private final GenAdaptor terrainBuilder;
    private TerrainQuad terrain;
    private TerrainQuad mountains;

    public TerrainManager(EngineContext app) {
        terrainBuilder = new GenAdaptor(app, chunkTracker);
        generateTerrain();
    }

    private void generateTerrain() {
        terrain = terrainBuilder.generateTerrain();
        mountains = terrainBuilder.generateMountains();
    }

    public TerrainQuad getTerrain() {
        return terrain;
    }

    public TerrainQuad getMountains() {
        return mountains;
    }

    public TerrainChunkTracker getChunkTracker() {
        return chunkTracker;
    }

    public Optional<Float> getHeightAt(float x, float z) {
        if (terrain == null) {
            return Optional.empty();
        }
        float height = terrain.getHeight(new Vector2f(x, z));
        if (Float.isNaN(height) || Float.isInfinite(height)) {
            return Optional.empty();
        }
        return Optional.of(height);
    }

    public Optional<Vector3f> findSafeSpawnLocation(float x, float z, float clearance) {
        return getHeightAt(x, z).map(height -> new Vector3f(x, height + clearance, z));
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("terrain", terrain != null);
        status.put("mountains", mountains != null);
        status.put("chunks", chunkTracker.status());
        return status;
    }

    public void update(float tpf) {
        terrainBuilder.update();
    }
}
