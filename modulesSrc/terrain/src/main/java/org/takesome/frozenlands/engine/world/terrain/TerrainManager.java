package org.takesome.frozenlands.engine.world.terrain;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.config.Constants;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;
import org.takesome.frozenlands.engine.terrain.TerrainService;
import org.takesome.frozenlands.engine.terrain.module.TerrainServiceFactory;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings.PlacementGroup;
import org.takesome.frozenlands.engine.world.terrain.chunk.TerrainChunkTracker;
import org.takesome.frozenlands.engine.world.terrain.gen.GenAdaptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TerrainManager {
    private final TerrainChunkTracker chunkTracker = new TerrainChunkTracker();
    private final TerrainRuntimeSettings settings = new TerrainRuntimeSettings();
    private final GenAdaptor terrainBuilder;
    private final TerrainService terrainService;
    private TerrainQuad terrain;
    private TerrainQuad mountains;

    public TerrainManager(EngineContext app) {
        this.terrainService = app.findService(TerrainService.class).orElseGet(() -> TerrainServiceFactory.create(app));
        this.terrainBuilder = new GenAdaptor(app, chunkTracker);
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

    public TerrainService getTerrainService() {
        return terrainService;
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

    public boolean isTerrainCollisionReady() {
        return chunkTracker.hasCollisionReadyChunks();
    }

    public Optional<Float> getWalkableHeightAt(float x, float z) {
        Optional<Float> visualHeight = getHeightAt(x, z);
        if (visualHeight.isPresent()) {
            return visualHeight;
        }
        try {
            return Optional.of(terrainService.walkableHeightAt(TerrainService.DEFAULT_SEED, x, z));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> sampleGameplayTerrain(float x, float z) {
        try {
            return Optional.of(terrainService.sample(TerrainService.DEFAULT_SEED, x, z).toMap());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Vector3f> findSafeSpawnLocation(float x, float z, float clearance) {
        return getWalkableHeightAt(x, z).map(height -> new Vector3f(x, height + clearance, z));
    }

    public Map<String, Object> settingsSnapshot() {
        return settings.toMap();
    }

    public List<Map<String, Object>> placementGroups() {
        return settings.placementGroups().stream().map(PlacementGroup::toMap).toList();
    }

    public Map<String, Object> validatePlacement(Map<String, Object> args) {
        List<PlacementGroup> groups = settings.placementGroups();
        PlacementGroup group = selectGroup(groups, RuntimeMaps.string(args, "group", RuntimeMaps.string(args, "groupId", "")));
        float x = RuntimeMaps.floating(args, "x", 0f);
        float z = RuntimeMaps.floating(args, "z", 0f);
        float radius = RuntimeMaps.floating(args, "radius", Math.max(0.5f, group.footprintPadding() + group.edgePadding()));
        float sampleRadius = Math.max(0.1f, radius + group.footprintPadding());
        float diagonal = sampleRadius * 0.70710677f;
        float[][] offsets = {
                {0f, 0f},
                {sampleRadius, 0f}, {-sampleRadius, 0f}, {0f, sampleRadius}, {0f, -sampleRadius},
                {diagonal, diagonal}, {-diagonal, diagonal}, {diagonal, -diagonal}, {-diagonal, -diagonal}
        };

        List<Map<String, Object>> samples = new ArrayList<>(offsets.length);
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean allResolved = true;
        boolean aboveWater = true;
        for (float[] offset : offsets) {
            float sx = x + offset[0];
            float sz = z + offset[1];
            Optional<Float> height = getWalkableHeightAt(sx, sz);
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("x", sx);
            sample.put("z", sz);
            sample.put("resolved", height.isPresent());
            if (height.isPresent()) {
                float y = height.get();
                sample.put("height", y);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                if (y <= Constants.WATER_LEVEL_HEIGHT) {
                    aboveWater = false;
                }
            } else {
                allResolved = false;
            }
            samples.add(sample);
        }

        float delta = allResolved ? maxY - minY : Float.POSITIVE_INFINITY;
        boolean valid = allResolved && aboveWater && delta <= group.maxSurfaceDelta();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("valid", valid);
        result.put("group", group.id());
        result.put("kind", group.kind());
        result.put("x", x);
        result.put("z", z);
        result.put("radius", radius);
        result.put("sampleRadius", sampleRadius);
        result.put("maxSurfaceDelta", group.maxSurfaceDelta());
        result.put("surfaceDelta", allResolved ? delta : null);
        result.put("aboveWater", aboveWater);
        result.put("samples", samples);
        if (!valid) {
            result.put("reason", !allResolved ? "terrain-height-missing" : !aboveWater ? "below-or-at-water-level" : "surface-delta-too-high");
        }
        return result;
    }

    private PlacementGroup selectGroup(List<PlacementGroup> groups, String groupId) {
        if (groupId != null && !groupId.isBlank()) {
            for (PlacementGroup group : groups) {
                if (group.id().equals(groupId)) {
                    return group;
                }
            }
        }
        return groups.isEmpty() ? new TerrainRuntimeSettings().placementGroups().get(0) : groups.get(0);
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("terrain", terrain != null);
        status.put("mountains", mountains != null);
        status.put("chunks", chunkTracker.status());
        status.put("terrainCollisionReady", isTerrainCollisionReady());
        status.put("terrainService", terrainService.status());
        status.put("settings", settingsSnapshot());
        return status;
    }

    public void update(float tpf) {
        terrainBuilder.update();
    }
}
