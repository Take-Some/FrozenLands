package org.takesome.frozenlands.engine.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface TerrainService {
    long DEFAULT_SEED = 5072572807575456817L;

    Profile profile();

    TerrainChunk chunk(long seed, int chunkX, int chunkZ);

    TerrainSample sample(long seed, float worldX, float worldZ);

    default float walkableHeightAt(long seed, float worldX, float worldZ) {
        return sample(seed, worldX, worldZ).walkableHeight();
    }

    void invalidateChunk(long seed, int chunkX, int chunkZ);

    void clearCache();

    Map<String, Object> status();

    record Profile(
            String id,
            int abi,
            int version,
            int chunkSize,
            float cellSize,
            float visualSnowScale,
            float walkableSnowScale,
            float maxSnowSink,
            List<Map<String, Object>> heightLayers,
            Map<String, Object> climate,
            List<Map<String, Object>> biomeRules,
            List<Map<String, Object>> features,
            Map<String, Object> flags
    ) {
        public Profile {
            id = id == null || id.isBlank() ? "frozenlands.default" : id;
            abi = Math.max(1, abi);
            version = Math.max(1, version);
            chunkSize = Math.max(4, chunkSize);
            cellSize = cellSize <= 0f ? 1f : cellSize;
            visualSnowScale = Math.max(0f, visualSnowScale);
            walkableSnowScale = Math.max(0f, walkableSnowScale);
            maxSnowSink = Math.max(0f, maxSnowSink);
            heightLayers = immutableMapList(heightLayers);
            climate = immutableMap(climate);
            biomeRules = immutableMapList(biomeRules);
            features = immutableMapList(features);
            flags = immutableMap(flags);
        }

        public int gridSize() {
            return chunkSize + 1;
        }

        public static Profile defaults() {
            return new Profile(
                    "frozenlands.default",
                    1,
                    2,
                    64,
                    1f,
                    0.35f,
                    0.18f,
                    0.18f,
                    defaultHeightLayers(),
                    defaultClimate(),
                    defaultBiomeRules(),
                    defaultFeatures(),
                    defaultFlags()
            );
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("abi", abi);
            result.put("version", version);
            result.put("chunkSize", chunkSize);
            result.put("gridSize", gridSize());
            result.put("cellSize", cellSize);
            result.put("visualSnowScale", visualSnowScale);
            result.put("walkableSnowScale", walkableSnowScale);
            result.put("maxSnowSink", maxSnowSink);
            result.put("height", heightLayers);
            result.put("climate", climate);
            result.put("biomes", biomeRules);
            result.put("features", features);
            result.put("flags", flags);
            return result;
        }

        private static List<Map<String, Object>> immutableMapList(List<Map<String, Object>> source) {
            if (source == null || source.isEmpty()) {
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>(source.size());
            for (Map<String, Object> map : source) {
                result.add(immutableMap(map));
            }
            return Collections.unmodifiableList(result);
        }

        private static Map<String, Object> immutableMap(Map<String, Object> source) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }

        private static List<Map<String, Object>> defaultHeightLayers() {
            List<Map<String, Object>> layers = new ArrayList<>();
            layers.add(layer("base", 0.018f, 14.0f, 5, 2.0f, 0.50f, 11, false));
            layers.add(layer("ridges", 0.055f, 5.0f, 4, 2.1f, 0.45f, 21, true));
            layers.add(layer("snowdrift", 0.120f, 1.8f, 3, 2.0f, 0.55f, 31, false));
            return layers;
        }

        private static Map<String, Object> layer(String id, float frequency, float amplitude, int octaves,
                                                 float lacunarity, float persistence, int seedSalt, boolean ridged) {
            Map<String, Object> layer = new LinkedHashMap<>();
            layer.put("id", id);
            layer.put("frequency", frequency);
            layer.put("amplitude", amplitude);
            layer.put("octaves", octaves);
            layer.put("lacunarity", lacunarity);
            layer.put("persistence", persistence);
            layer.put("seed_salt", seedSalt);
            layer.put("ridged", ridged);
            return layer;
        }

        private static Map<String, Object> defaultClimate() {
            Map<String, Object> climate = new LinkedHashMap<>();
            climate.put("temperature_base", -28.0f);
            climate.put("temperature_amp", 12.0f);
            climate.put("temperature_frequency", 0.010f);
            climate.put("temperature_salt", 51);
            climate.put("temperature_height_loss", 0.10f);
            climate.put("moisture_frequency", 0.030f);
            climate.put("moisture_salt", 61);
            climate.put("wind_frequency", 0.045f);
            climate.put("wind_salt", 71);
            climate.put("roughness_frequency", 0.080f);
            climate.put("roughness_salt", 41);
            climate.put("ice_bias", 0.0f);
            climate.put("snow_bias", 0.0f);
            return climate;
        }

        private static List<Map<String, Object>> defaultBiomeRules() {
            List<Map<String, Object>> rules = new ArrayList<>();
            rules.add(biome("frozen_lake", "ice", Map.of("max_height", 2.0f, "min_ice", 0.72f)));
            rules.add(biome("cracked_ice", "cracked_ice", Map.of("max_height", 3.5f, "min_ice", 0.55f, "min_roughness", 0.46f)));
            rules.add(biome("geothermal_field", "steam", Map.of("min_temperature", -12.0f, "max_ice", 0.35f)));
            rules.add(biome("black_rock_ridge", "rock", Map.of("min_height", 15.0f, "min_roughness", 0.52f)));
            rules.add(biome("ruin_field", "ruin", Map.of("min_height", 4.0f, "max_height", 12.0f, "max_roughness", 0.55f)));
            rules.add(biome("snow_dunes", "snow", Map.of("max_height", 16.0f, "min_snow", 0.55f)));
            rules.add(biome("glacial_plain", "mixed", Map.of()));
            return rules;
        }

        private static Map<String, Object> biome(String id, String surface, Map<String, Object> predicates) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("surface", surface);
            result.putAll(predicates);
            return result;
        }

        private static List<Map<String, Object>> defaultFeatures() {
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(feature("dead_tree", List.of("snow_dunes", "glacial_plain"), 0.006f, 5, 101));
            result.add(feature("ice_crack", List.of("cracked_ice", "frozen_lake"), 0.025f, 2, 102));
            result.add(feature("black_rock", List.of("black_rock_ridge"), 0.018f, 3, 103));
            result.add(feature("supply_cache", List.of("ruin_field"), 0.002f, 12, 104));
            result.add(feature("steam_vent", List.of("geothermal_field"), 0.010f, 6, 105));
            return result;
        }

        private static Map<String, Object> feature(String id, List<String> biomes, float density, int minSpacing, int seedSalt) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("biomes", biomes);
            result.put("density", density);
            result.put("min_spacing", minSpacing);
            result.put("seed_salt", seedSalt);
            return result;
        }

        private static Map<String, Object> defaultFlags() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("walkable_max_roughness", 0.78f);
            result.put("icy_min", 0.58f);
            result.put("fragile_ice_min", 0.65f);
            result.put("fragile_stability_max", 0.42f);
            result.put("deep_snow_min", 0.45f);
            result.put("steep_min_roughness", 0.70f);
            result.put("warm_temperature_min", -12.0f);
            result.put("blocked_roughness_min", 0.68f);
            return result;
        }
    }

    record TerrainSample(
            float x,
            float z,
            float baseHeight,
            float snowDepth,
            float visualHeight,
            float walkableHeight,
            float ice,
            float temperature,
            float moisture,
            float wind,
            float roughness,
            float stability,
            String biome,
            String surface,
            int flags,
            String feature
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("x", x);
            result.put("z", z);
            result.put("baseHeight", baseHeight);
            result.put("snowDepth", snowDepth);
            result.put("visualHeight", visualHeight);
            result.put("walkableHeight", walkableHeight);
            result.put("ice", ice);
            result.put("temperature", temperature);
            result.put("moisture", moisture);
            result.put("wind", wind);
            result.put("roughness", roughness);
            result.put("stability", stability);
            result.put("biome", biome);
            result.put("surface", surface);
            result.put("flags", flags);
            result.put("feature", feature);
            return result;
        }
    }

    final class TerrainChunk {
        private final long seed;
        private final int chunkX;
        private final int chunkZ;
        private final Profile profile;
        private final TerrainSample[] samples;

        public TerrainChunk(long seed, int chunkX, int chunkZ, Profile profile, TerrainSample[] samples) {
            this.seed = seed;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.profile = profile == null ? Profile.defaults() : profile;
            int expected = this.profile.gridSize() * this.profile.gridSize();
            if (samples == null || samples.length != expected) {
                throw new IllegalArgumentException("Terrain chunk sample array length must be " + expected);
            }
            this.samples = samples;
        }

        public long seed() { return seed; }
        public int chunkX() { return chunkX; }
        public int chunkZ() { return chunkZ; }
        public Profile profile() { return profile; }
        public int gridSize() { return profile.gridSize(); }

        public TerrainSample vertex(int x, int z) {
            int clampedX = clamp(x, 0, gridSize() - 1);
            int clampedZ = clamp(z, 0, gridSize() - 1);
            return samples[clampedZ * gridSize() + clampedX];
        }

        public Map<String, Object> summary() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("seed", seed);
            result.put("chunkX", chunkX);
            result.put("chunkZ", chunkZ);
            result.put("profile", profile.id());
            result.put("profileVersion", profile.version());
            result.put("chunkSize", profile.chunkSize());
            result.put("gridSize", profile.gridSize());
            return result;
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
