package org.takesome.frozenlands.engine.terrain.module;

import org.takesome.frozenlands.engine.runtime.RuntimeMaps;
import org.takesome.frozenlands.engine.terrain.TerrainService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class GeneratedTerrainService implements TerrainService {
    private final Profile profile;
    private final Profile fallback = Profile.defaults();
    private final ConcurrentMap<ChunkKey, TerrainChunk> chunks = new ConcurrentHashMap<>();

    GeneratedTerrainService(Profile profile) {
        this.profile = profile == null ? Profile.defaults() : profile;
    }

    @Override public Profile profile() { return profile; }

    @Override
    public TerrainChunk chunk(long seed, int chunkX, int chunkZ) {
        return chunks.computeIfAbsent(new ChunkKey(seed, chunkX, chunkZ, profile.version()), this::generateChunk);
    }

    @Override
    public TerrainSample sample(long seed, float worldX, float worldZ) {
        float gridX = worldX / profile.cellSize();
        float gridZ = worldZ / profile.cellSize();
        int globalX = floorToInt(gridX);
        int globalZ = floorToInt(gridZ);
        int chunkX = Math.floorDiv(globalX, profile.chunkSize());
        int chunkZ = Math.floorDiv(globalZ, profile.chunkSize());
        TerrainChunk chunk = chunk(seed, chunkX, chunkZ);
        return bilinear(chunk, gridX - chunkX * profile.chunkSize(), gridZ - chunkZ * profile.chunkSize());
    }

    @Override public void invalidateChunk(long seed, int chunkX, int chunkZ) { chunks.remove(new ChunkKey(seed, chunkX, chunkZ, profile.version())); }
    @Override public void clearCache() { chunks.clear(); }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("profile", profile.toMap());
        result.put("cachedChunks", chunks.size());
        result.put("cache", "local-concurrent-map");
        return result;
    }

    private TerrainChunk generateChunk(ChunkKey key) {
        int gridSize = profile.gridSize();
        TerrainSample[] samples = new TerrainSample[gridSize * gridSize];
        for (int z = 0; z < gridSize; z++) {
            for (int x = 0; x < gridSize; x++) {
                int wx = key.chunkX() * profile.chunkSize() + x;
                int wz = key.chunkZ() * profile.chunkSize() + z;
                samples[z * gridSize + x] = generateSample(key.seed(), wx, wz);
            }
        }
        return new TerrainChunk(key.seed(), key.chunkX(), key.chunkZ(), profile, samples);
    }

    private TerrainSample generateSample(long seed, int gridX, int gridZ) {
        float x = gridX * profile.cellSize();
        float z = gridZ * profile.cellSize();
        Map<String, Object> climate = mergedMap(fallback.climate(), profile.climate());
        float baseHeight = height(seed, x, z);
        float roughness = clamp01(0.35f + Math.abs(noise(seed, x, z, f(climate, "roughness_frequency", 0.08f), i(climate, "roughness_salt", 41))) * 0.65f);
        float temperature = f(climate, "temperature_base", -28f)
                + noise(seed, x, z, f(climate, "temperature_frequency", 0.01f), i(climate, "temperature_salt", 51)) * f(climate, "temperature_amp", 12f)
                - Math.max(0f, baseHeight) * f(climate, "temperature_height_loss", 0.10f);
        float moisture = clamp01(0.5f + noise(seed, x, z, f(climate, "moisture_frequency", 0.03f), i(climate, "moisture_salt", 61)) * 0.5f);
        float wind = clamp01(0.5f + noise(seed, x, z, f(climate, "wind_frequency", 0.045f), i(climate, "wind_salt", 71)) * 0.5f);
        float ice = clamp01(0.25f + moisture * 0.55f + clamp01(-temperature / 55f) * 0.35f - roughness * 0.20f + f(climate, "ice_bias", 0f));
        float snowDepth = clamp01(0.20f + moisture * 0.80f + clamp01(-temperature / 60f) * 0.25f - wind * 0.18f + f(climate, "snow_bias", 0f)) * 1.25f;
        float stability = clamp01(1.0f - roughness * 0.45f - ice * 0.20f + snowDepth * 0.08f);
        Map<String, Object> biomeRule = biomeRule(baseHeight, roughness, ice, snowDepth, temperature);
        String biome = RuntimeMaps.string(biomeRule, "id", "glacial_plain");
        String surface = RuntimeMaps.string(biomeRule, "surface", surfaceFallback(ice, snowDepth, roughness));
        int flags = flags(biome, ice, snowDepth, roughness, stability, temperature);
        String feature = feature(seed, gridX, gridZ, biome);
        float visualHeight = baseHeight + snowDepth * profile.visualSnowScale();
        float walkableHeight = baseHeight + Math.min(snowDepth * profile.walkableSnowScale(), profile.maxSnowSink());
        return new TerrainSample(x, z, baseHeight, snowDepth, visualHeight, walkableHeight, ice,
                temperature, moisture, wind, roughness, stability, biome, surface, flags, feature);
    }

    private float height(long seed, float x, float z) {
        List<Map<String, Object>> layers = profile.heightLayers().isEmpty() ? fallback.heightLayers() : profile.heightLayers();
        float result = 0f;
        for (Map<String, Object> layer : layers) {
            float value = octaveNoise(seed, x, z, f(layer, "frequency", 0.01f), i(layer, "octaves", 1),
                    f(layer, "lacunarity", 2f), f(layer, "persistence", 0.5f), i(layer, "seed_salt", 0));
            if (RuntimeMaps.bool(layer, "ridged", false)) value = 1f - Math.abs(value);
            result += value * f(layer, "amplitude", 1f);
        }
        return result;
    }

    private Map<String, Object> biomeRule(float height, float roughness, float ice, float snow, float temperature) {
        List<Map<String, Object>> rules = profile.biomeRules().isEmpty() ? fallback.biomeRules() : profile.biomeRules();
        Map<String, Object> fallbackRule = null;
        for (Map<String, Object> rule : rules) {
            if (RuntimeMaps.bool(rule, "fallback", false)) fallbackRule = rule;
            if (within(rule, "height", height) && within(rule, "roughness", roughness) && within(rule, "ice", ice)
                    && within(rule, "snow", snow) && within(rule, "temperature", temperature)) return rule;
        }
        return fallbackRule == null ? Map.of("id", "glacial_plain", "surface", "mixed") : fallbackRule;
    }

    private boolean within(Map<String, Object> rule, String field, float value) {
        if (rule.containsKey("min_" + field) && value < RuntimeMaps.floating(rule, "min_" + field, Float.NEGATIVE_INFINITY)) return false;
        return !rule.containsKey("max_" + field) || value <= RuntimeMaps.floating(rule, "max_" + field, Float.POSITIVE_INFINITY);
    }

    private String feature(long seed, int x, int z, String biome) {
        List<Map<String, Object>> rules = profile.features().isEmpty() ? fallback.features() : profile.features();
        for (Map<String, Object> rule : rules) {
            if (RuntimeMaps.stringList(rule.get("biomes")).contains(biome)) {
                int spacing = Math.max(1, i(rule, "min_spacing", 1));
                int cellX = Math.floorDiv(x, spacing);
                int cellZ = Math.floorDiv(z, spacing);
                if (hash01(seed, cellX, cellZ, i(rule, "seed_salt", 0)) < f(rule, "density", 0f)) {
                    return RuntimeMaps.string(rule, "id", null);
                }
            }
        }
        return null;
    }

    private int flags(String biome, float ice, float snow, float roughness, float stability, float temperature) {
        Map<String, Object> cfg = mergedMap(fallback.flags(), profile.flags());
        int flags = 0;
        if (roughness < f(cfg, "walkable_max_roughness", 0.78f)) flags |= 1;
        if (ice > f(cfg, "icy_min", 0.58f)) flags |= 1 << 1;
        if (ice > f(cfg, "fragile_ice_min", 0.65f) && stability < f(cfg, "fragile_stability_max", 0.42f)) flags |= 1 << 2;
        if (snow > f(cfg, "deep_snow_min", 0.45f)) flags |= 1 << 3;
        if (roughness > f(cfg, "steep_min_roughness", 0.70f)) flags |= 1 << 4;
        if (temperature > f(cfg, "warm_temperature_min", -12f) || "geothermal_field".equals(biome)) flags |= 1 << 5;
        if (roughness > f(cfg, "blocked_roughness_min", 0.68f) || "frozen_lake".equals(biome)) flags |= 1 << 6;
        return flags;
    }

    private String surfaceFallback(float ice, float snow, float roughness) {
        if (ice > 0.70f) return "ice";
        if (ice > 0.52f && roughness > 0.45f) return "cracked_ice";
        return snow > 0.35f ? "snow" : "mixed";
    }

    private static TerrainSample bilinear(TerrainChunk chunk, float localX, float localZ) {
        int max = chunk.gridSize() - 1;
        int x = clamp(Math.round(localX), 0, max);
        int z = clamp(Math.round(localZ), 0, max);
        return chunk.vertex(x, z);
    }

    private static float octaveNoise(long seed, float x, float z, float frequency, int octaves, float lacunarity, float persistence, int salt) {
        float sum = 0f, amp = 1f, norm = 0f, freq = frequency;
        for (int i = 0; i < Math.max(1, octaves); i++) {
            sum += noise(seed, x, z, freq, salt + i * 131) * amp;
            norm += amp;
            amp *= persistence;
            freq *= lacunarity;
        }
        return norm == 0f ? 0f : sum / norm;
    }

    private static float noise(long seed, float x, float z, float frequency, int salt) {
        int ix = floorToInt(x * frequency);
        int iz = floorToInt(z * frequency);
        return hash01(seed, ix, iz, salt) * 2f - 1f;
    }

    private static float hash01(long seed, int x, int z, int salt) {
        long h = seed + x * 374761393L + z * 668265263L + salt * 2246822519L;
        h ^= h >>> 13;
        h *= 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFFL) / (float) 0x1000000;
    }

    private static Map<String, Object> mergedMap(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (base != null) result.putAll(base);
        if (override != null) result.putAll(override);
        return result;
    }

    private static float f(Map<String, Object> map, String key, float fallback) { return RuntimeMaps.floating(map, key, fallback); }
    private static int i(Map<String, Object> map, String key, int fallback) { return RuntimeMaps.integer(map, key, fallback); }
    private static int floorToInt(float value) { return (int) Math.floor(value); }
    private static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private record ChunkKey(long seed, int chunkX, int chunkZ, int profileVersion) {}
}
