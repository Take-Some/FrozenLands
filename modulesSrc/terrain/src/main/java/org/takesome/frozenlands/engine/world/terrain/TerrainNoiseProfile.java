package org.takesome.frozenlands.engine.world.terrain;

import org.takesome.frozenlands.engine.runtime.RuntimeMaps;

import java.util.Map;

public record TerrainNoiseProfile(
        float roughness,
        float frequency,
        float amplitude,
        float lacunarity,
        int octaves,
        float scale
) {
    public TerrainNoiseProfile {
        octaves = Math.max(1, octaves);
        scale = positive(scale, 0.02125f);
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "roughness", roughness,
                "frequency", frequency,
                "amplitude", amplitude,
                "lacunarity", lacunarity,
                "octaves", octaves,
                "scale", scale
        );
    }

    public static TerrainNoiseProfile from(Map<String, Object> source) {
        return new TerrainNoiseProfile(
                RuntimeMaps.floating(source, "roughness", 0.82f),
                RuntimeMaps.floating(source, "frequency", 0.1f),
                RuntimeMaps.floating(source, "amplitude", 1.1f),
                RuntimeMaps.floating(source, "lacunarity", 2.12f),
                RuntimeMaps.integer(source, "octaves", 8),
                RuntimeMaps.floating(source, "scale", 0.02125f)
        );
    }

    private static float positive(float value, float fallback) {
        return value > 0f ? value : fallback;
    }
}
