package org.takesome.frozenlands.engine.world.terrain;

import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TerrainRuntimeSettings {
    private final LuaRuntimeConfig loader = new LuaRuntimeConfig();
    private final Map<String, Object> config = loader.read("engine.terrain");
    private final Map<String, Object> grid = loader.map(config, "grid");
    private final Map<String, Object> scale = loader.map(config, "scale");
    private final Map<String, Object> lod = loader.map(config, "lod");
    private final Map<String, Object> materials = loader.map(config, "materials");
    private final Map<String, Object> mountains = loader.map(config, "mountains");
    private final Map<String, Object> mountainScale = loader.map(mountains, "scale");
    private final Map<String, Object> noise = loader.map(config, "noise");
    private final Map<String, Object> filters = loader.map(config, "filters");
    private final TerrainNoiseProfile noiseProfile = TerrainNoiseProfile.from(noise);
    private final TerrainFilterProfile filterProfile = TerrainFilterProfile.from(filters);
    private final Map<String, Object> trees = loader.map(config, "trees");
    private final Map<String, Object> treeCollision = loader.map(trees, "collision");
    private final Map<String, Object> treeHealth = loader.map(trees, "health");
    private final Map<String, Object> placements = loader.map(config, "placements");
    private final TerrainPlacementProfile placementProfile
            = TerrainPlacementProfile.from(placements, trees, treeCollision, treeHealth);
    private final Map<String, Object> events = loader.map(config, "events");
    private final Map<String, Object> collision = loader.map(config, "collision");

    public int patchSize() { return validTerrainSize(loader.integer(grid, "patchSize", 65), 65); }
    public int quadSize() { return validTerrainSize(loader.integer(grid, "quadSize", 513), 513); }
    public float tileNoiseScale() { return positive(loader.floating(grid, "tileNoiseScale", 96f), 96f); }
    public float scaleX() { return loader.floating(scale, "x", 1.35f); }
    public float scaleY() { return loader.floating(scale, "y", 1f); }
    public float scaleZ() { return loader.floating(scale, "z", 1.35f); }
    public int lodPatchSize() { return validTerrainSize(loader.integer(lod, "patchSize", 65), 65); }
    public float lodMultiplier() { return positive(loader.floating(lod, "multiplier", 2.0f), 2.0f); }
    public String terrainMaterial() { return loader.string(materials, "terrain", "terrain#default"); }
    public String mountainMaterial() { return loader.string(materials, "mountains", "terrain#mount"); }
    public String mountainHeightMap() { return requiredAssetPath(loader.string(mountains, "heightMap", ""), "terrain.mountains.heightMap"); }
    public int mountainPatchSize() { return validTerrainSize(loader.integer(mountains, "patchSize", 65), 65); }
    public int mountainQuadSize() { return validTerrainSize(loader.integer(mountains, "quadSize", 2049), 2049); }
    public float mountainSmoothAmount() { return loader.floating(mountains, "smoothAmount", 0.65f); }
    public int mountainSmoothRadius() { return Math.max(0, loader.integer(mountains, "smoothRadius", 1)); }
    public byte mountainFlatten() { return (byte) Math.max(0, loader.integer(mountains, "flatten", 2)); }
    public float mountainHeightOffset() { return loader.floating(mountains, "heightOffset", 50f); }
    public float mountainScaleX() { return loader.floating(mountainScale, "x", 6f); }
    public float mountainScaleY() { return loader.floating(mountainScale, "y", 19f); }
    public float mountainScaleZ() { return loader.floating(mountainScale, "z", 6f); }
    public float roughness() { return noiseProfile.roughness(); }
    public float frequency() { return noiseProfile.frequency(); }
    public float amplitude() { return noiseProfile.amplitude(); }
    public float lacunarity() { return noiseProfile.lacunarity(); }
    public int octaves() { return noiseProfile.octaves(); }
    public float noiseScale() { return noiseProfile.scale(); }
    public float perturbMagnitude() { return filterProfile.perturbMagnitude(); }
    public int erosionRadius() { return filterProfile.erosionRadius(); }
    public float erosionTalus() { return filterProfile.erosionTalus(); }
    public int terrainSmoothRadius() { return filterProfile.smoothRadius(); }
    public float terrainSmoothEffect() { return filterProfile.smoothEffect(); }
    public int filterIterations() { return filterProfile.iterations(); }

    public List<String> treeModelPaths() {
        List<String> paths = RuntimeMaps.stringList(trees.get("models"));
        if (paths.isEmpty()) {
            throw new IllegalStateException("Terrain tree asset model list is empty: terrain.trees.models");
        }
        return paths;
    }

    public int minTreesPerTile() { return Math.max(0, loader.integer(trees, "minPerTile", 80)); }
    public int maxTreesPerTile() { return Math.max(minTreesPerTile(), loader.integer(trees, "maxPerTile", 160)); }
    public int treePlacementAttempts() { return Math.max(1, loader.integer(trees, "placementAttempts", 16)); }
    public float treeRayStartHeight() { return positive(loader.floating(trees, "rayStartHeight", 800f), 800f); }
    public boolean treeCollisionEnabled() { return loader.bool(treeCollision, "enabled", true); }
    public boolean treeCollisionMeshEnabled() { return loader.bool(treeCollision, "mesh", false); }
    public float treeBaseHealth() { return loader.floating(treeHealth, "base", 80f); }
    public float treeHealthPerScale() { return loader.floating(treeHealth, "perScale", 45f); }


    public String tileAttachedTopic() { return loader.string(events, "tileAttachedTopic", "terrain.tile.attached"); }
    public String tileDetachedTopic() { return loader.string(events, "tileDetachedTopic", "terrain.tile.detached"); }
    public String tileCollisionReadyTopic() { return loader.string(events, "tileCollisionReadyTopic", "terrain.tile.collision.ready"); }
    public String collisionReadyTopic() { return loader.string(events, "collisionReadyTopic", "terrain.collision.ready"); }
    public String terrainCollisionMode() { return loader.string(collision, "mode", "heightfield").trim().toLowerCase(); }
    public boolean terrainCollisionLogReady() { return loader.bool(collision, "logReady", false); }

    public List<TerrainPlacementGroup> placementGroups() {
        return placementProfile.groups();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("grid", Map.of("patchSize", patchSize(), "quadSize", quadSize(), "tileNoiseScale", tileNoiseScale()));
        result.put("scale", Map.of("x", scaleX(), "y", scaleY(), "z", scaleZ()));
        result.put("lod", Map.of("patchSize", lodPatchSize(), "multiplier", lodMultiplier()));
        result.put("materials", Map.of("terrain", terrainMaterial(), "mountains", mountainMaterial()));
        result.put("noise", noiseProfile.toMap());
        result.put("filters", filterProfile.toMap());
        result.put("placements", placementGroups().stream().map(TerrainPlacementGroup::toMap).toList());
        result.put("events", Map.of(
                "tileAttachedTopic", tileAttachedTopic(),
                "tileDetachedTopic", tileDetachedTopic(),
                "tileCollisionReadyTopic", tileCollisionReadyTopic(),
                "collisionReadyTopic", collisionReadyTopic()
        ));
        return result;
    }

    private static int validTerrainSize(int value, int fallback) {
        int normalized = value < 3 ? fallback : value;
        return normalized % 2 == 0 ? normalized + 1 : normalized;
    }

    private static float positive(float value, float fallback) {
        return value > 0f ? value : fallback;
    }

    private static String requiredAssetPath(String value, String configKey) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required terrain asset path is not configured: " + configKey);
        }
        return value;
    }

}
