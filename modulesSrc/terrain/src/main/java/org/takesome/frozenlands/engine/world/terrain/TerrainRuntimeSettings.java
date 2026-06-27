package org.takesome.frozenlands.engine.world.terrain;

import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;

import java.util.ArrayList;
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
    private final Map<String, Object> trees = loader.map(config, "trees");
    private final Map<String, Object> treeCollision = loader.map(trees, "collision");
    private final Map<String, Object> treeHealth = loader.map(trees, "health");
    private final Map<String, Object> placements = loader.map(config, "placements");
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
    public float roughness() { return loader.floating(noise, "roughness", 0.82f); }
    public float frequency() { return loader.floating(noise, "frequency", 0.1f); }
    public float amplitude() { return loader.floating(noise, "amplitude", 1.1f); }
    public float lacunarity() { return loader.floating(noise, "lacunarity", 2.12f); }
    public int octaves() { return Math.max(1, loader.integer(noise, "octaves", 8)); }
    public float noiseScale() { return positive(loader.floating(noise, "scale", 0.02125f), 0.02125f); }
    public float perturbMagnitude() { return loader.floating(filters, "perturbMagnitude", 0.419f); }
    public int erosionRadius() { return Math.max(0, loader.integer(filters, "erosionRadius", 1)); }
    public float erosionTalus() { return loader.floating(filters, "erosionTalus", 0.711f); }
    public int terrainSmoothRadius() { return Math.max(0, loader.integer(filters, "smoothRadius", 1)); }
    public float terrainSmoothEffect() { return loader.floating(filters, "smoothEffect", 0.7f); }
    public int filterIterations() { return Math.max(0, loader.integer(filters, "iterations", 1)); }

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

    public List<PlacementGroup> placementGroups() {
        List<PlacementGroup> groups = new ArrayList<>();
        Object rawGroups = placements.get("groups");
        for (Object raw : RuntimeMaps.list(rawGroups)) {
            Map<String, Object> map = RuntimeMaps.map(raw);
            if (!map.isEmpty()) {
                PlacementGroup group = placementGroup(map, groups.size());
                if (!group.modelPaths().isEmpty()) {
                    groups.add(group);
                }
            }
        }
        if (groups.isEmpty()) {
            groups.add(legacyTreePlacementGroup());
        }
        return List.copyOf(groups);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("grid", Map.of("patchSize", patchSize(), "quadSize", quadSize(), "tileNoiseScale", tileNoiseScale()));
        result.put("scale", Map.of("x", scaleX(), "y", scaleY(), "z", scaleZ()));
        result.put("lod", Map.of("patchSize", lodPatchSize(), "multiplier", lodMultiplier()));
        result.put("materials", Map.of("terrain", terrainMaterial(), "mountains", mountainMaterial()));
        result.put("noise", Map.of(
                "roughness", roughness(),
                "frequency", frequency(),
                "amplitude", amplitude(),
                "lacunarity", lacunarity(),
                "octaves", octaves(),
                "scale", noiseScale()
        ));
        result.put("filters", Map.of(
                "perturbMagnitude", perturbMagnitude(),
                "erosionRadius", erosionRadius(),
                "erosionTalus", erosionTalus(),
                "smoothRadius", terrainSmoothRadius(),
                "smoothEffect", terrainSmoothEffect(),
                "iterations", filterIterations()
        ));
        result.put("placements", placementGroups().stream().map(PlacementGroup::toMap).toList());
        result.put("events", Map.of(
                "tileAttachedTopic", tileAttachedTopic(),
                "tileDetachedTopic", tileDetachedTopic(),
                "tileCollisionReadyTopic", tileCollisionReadyTopic(),
                "collisionReadyTopic", collisionReadyTopic()
        ));
        return result;
    }

    private PlacementGroup placementGroup(Map<String, Object> source, int index) {
        Map<String, Object> scaleMap = RuntimeMaps.map(source.get("scale"));
        Map<String, Object> rotationMap = RuntimeMaps.map(source.get("rotation"));
        Map<String, Object> fitMap = RuntimeMaps.map(source.get("fit"));
        Map<String, Object> collisionMap = RuntimeMaps.map(source.get("collision"));
        Map<String, Object> gameplayMap = RuntimeMaps.map(source.get("gameplay"));
        List<String> models = modelPaths(source);
        String id = RuntimeMaps.string(source, "id", "terrain_asset_group_" + index);
        String kind = RuntimeMaps.string(source, "kind", id);
        float scaleMin = positive(RuntimeMaps.floating(scaleMap, "min", 1f), 1f);
        float scaleMax = Math.max(scaleMin, RuntimeMaps.floating(scaleMap, "max", scaleMin));
        return new PlacementGroup(
                id,
                RuntimeMaps.bool(source, "enabled", true),
                kind,
                models,
                Math.max(0, RuntimeMaps.integer(source, "minPerTile", 0)),
                Math.max(0, RuntimeMaps.integer(source, "maxPerTile", 0)),
                Math.max(1, RuntimeMaps.integer(source, "placementAttempts", 8)),
                positive(RuntimeMaps.floating(source, "rayStartHeight", 800f), 800f),
                scaleMin,
                scaleMax,
                RuntimeMaps.floating(rotationMap, "yawMinDeg", -180f),
                RuntimeMaps.floating(rotationMap, "yawMaxDeg", 180f),
                Math.max(0f, RuntimeMaps.floating(fitMap, "edgePadding", 1f)),
                Math.max(0f, RuntimeMaps.floating(fitMap, "footprintPadding", 0.5f)),
                Math.max(0f, RuntimeMaps.floating(fitMap, "maxSurfaceDelta", 1.5f)),
                Math.max(0f, RuntimeMaps.floating(fitMap, "minSpacing", 2f)),
                RuntimeMaps.bool(collisionMap, "enabled", false),
                RuntimeMaps.bool(collisionMap, "mesh", false),
                Math.max(0f, RuntimeMaps.floating(collisionMap, "proxyRadiusFactor", 0.14f)),
                Math.max(0.01f, RuntimeMaps.floating(collisionMap, "proxyRadiusMin", 0.22f)),
                Math.max(0.01f, RuntimeMaps.floating(collisionMap, "proxyRadiusMax", 0.55f)),
                Math.max(0f, RuntimeMaps.floating(collisionMap, "proxyHalfHeightFactor", 0.38f)),
                Math.max(0.01f, RuntimeMaps.floating(collisionMap, "proxyHalfHeightMin", 1.35f)),
                Math.max(0.01f, RuntimeMaps.floating(collisionMap, "proxyHalfHeightMax", 3.6f)),
                RuntimeMaps.floating(collisionMap, "proxyYOffset", 0.08f),
                RuntimeMaps.bool(gameplayMap, "grindable", false),
                RuntimeMaps.integer(gameplayMap, "kindCode", 0),
                RuntimeMaps.floating(gameplayMap, "baseHealth", 0f),
                RuntimeMaps.floating(gameplayMap, "healthPerScale", 0f)
        );
    }

    private List<String> modelPaths(Map<String, Object> source) {
        List<String> models = RuntimeMaps.stringList(source.get("models"));
        if (!models.isEmpty()) {
            return models;
        }
        String single = RuntimeMaps.string(source, "model", "");
        return single == null || single.isBlank() ? List.of() : List.of(single);
    }

    private PlacementGroup legacyTreePlacementGroup() {
        return new PlacementGroup(
                "legacy_trees",
                true,
                "tree",
                treeModelPaths(),
                minTreesPerTile(),
                maxTreesPerTile(),
                treePlacementAttempts(),
                treeRayStartHeight(),
                0.42f,
                1.35f,
                -180f,
                180f,
                1.25f,
                0.65f,
                1.65f,
                2.75f,
                treeCollisionEnabled(),
                treeCollisionMeshEnabled(),
                0.14f,
                0.22f,
                0.55f,
                0.38f,
                1.35f,
                3.6f,
                0.08f,
                true,
                1,
                treeBaseHealth(),
                treeHealthPerScale()
        );
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

    public record PlacementGroup(
            String id,
            boolean enabled,
            String kind,
            List<String> modelPaths,
            int minPerTile,
            int maxPerTile,
            int placementAttempts,
            float rayStartHeight,
            float scaleMin,
            float scaleMax,
            float yawMinDeg,
            float yawMaxDeg,
            float edgePadding,
            float footprintPadding,
            float maxSurfaceDelta,
            float minSpacing,
            boolean collisionEnabled,
            boolean collisionMesh,
            float proxyRadiusFactor,
            float proxyRadiusMin,
            float proxyRadiusMax,
            float proxyHalfHeightFactor,
            float proxyHalfHeightMin,
            float proxyHalfHeightMax,
            float proxyYOffset,
            boolean grindable,
            int gameplayKindCode,
            float baseHealth,
            float healthPerScale
    ) {
        public PlacementGroup {
            id = id == null || id.isBlank() ? "terrain_asset_group" : id;
            kind = kind == null || kind.isBlank() ? id : kind;
            modelPaths = modelPaths == null ? List.of() : List.copyOf(modelPaths);
            minPerTile = Math.max(0, minPerTile);
            maxPerTile = Math.max(minPerTile, maxPerTile);
            placementAttempts = Math.max(1, placementAttempts);
            rayStartHeight = rayStartHeight > 0f ? rayStartHeight : 800f;
            scaleMin = scaleMin > 0f ? scaleMin : 1f;
            scaleMax = Math.max(scaleMin, scaleMax);
            edgePadding = Math.max(0f, edgePadding);
            footprintPadding = Math.max(0f, footprintPadding);
            maxSurfaceDelta = Math.max(0f, maxSurfaceDelta);
            minSpacing = Math.max(0f, minSpacing);
            proxyRadiusFactor = Math.max(0f, proxyRadiusFactor);
            proxyRadiusMin = Math.max(0.01f, proxyRadiusMin);
            proxyRadiusMax = Math.max(proxyRadiusMin, proxyRadiusMax);
            proxyHalfHeightFactor = Math.max(0f, proxyHalfHeightFactor);
            proxyHalfHeightMin = Math.max(0.01f, proxyHalfHeightMin);
            proxyHalfHeightMax = Math.max(proxyHalfHeightMin, proxyHalfHeightMax);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("enabled", enabled);
            result.put("kind", kind);
            result.put("models", modelPaths);
            result.put("minPerTile", minPerTile);
            result.put("maxPerTile", maxPerTile);
            result.put("placementAttempts", placementAttempts);
            result.put("rayStartHeight", rayStartHeight);
            result.put("scale", Map.of("min", scaleMin, "max", scaleMax));
            result.put("rotation", Map.of("yawMinDeg", yawMinDeg, "yawMaxDeg", yawMaxDeg));
            result.put("fit", Map.of(
                    "edgePadding", edgePadding,
                    "footprintPadding", footprintPadding,
                    "maxSurfaceDelta", maxSurfaceDelta,
                    "minSpacing", minSpacing
            ));
            result.put("collision", Map.of(
                    "enabled", collisionEnabled,
                    "mesh", collisionMesh,
                    "proxyRadiusFactor", proxyRadiusFactor,
                    "proxyRadiusMin", proxyRadiusMin,
                    "proxyRadiusMax", proxyRadiusMax,
                    "proxyHalfHeightFactor", proxyHalfHeightFactor,
                    "proxyHalfHeightMin", proxyHalfHeightMin,
                    "proxyHalfHeightMax", proxyHalfHeightMax,
                    "proxyYOffset", proxyYOffset
            ));
            result.put("gameplay", Map.of(
                    "grindable", grindable,
                    "kindCode", gameplayKindCode,
                    "baseHealth", baseHealth,
                    "healthPerScale", healthPerScale
            ));
            return result;
        }
    }
}
