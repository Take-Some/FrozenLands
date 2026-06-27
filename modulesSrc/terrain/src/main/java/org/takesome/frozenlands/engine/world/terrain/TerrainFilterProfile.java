package org.takesome.frozenlands.engine.world.terrain;

import org.takesome.frozenlands.engine.runtime.RuntimeMaps;

import java.util.Map;

public record TerrainFilterProfile(
        float perturbMagnitude,
        int erosionRadius,
        float erosionTalus,
        int smoothRadius,
        float smoothEffect,
        int iterations
) {
    public TerrainFilterProfile {
        erosionRadius = Math.max(0, erosionRadius);
        smoothRadius = Math.max(0, smoothRadius);
        iterations = Math.max(0, iterations);
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "perturbMagnitude", perturbMagnitude,
                "erosionRadius", erosionRadius,
                "erosionTalus", erosionTalus,
                "smoothRadius", smoothRadius,
                "smoothEffect", smoothEffect,
                "iterations", iterations
        );
    }

    public static TerrainFilterProfile from(Map<String, Object> source) {
        return new TerrainFilterProfile(
                RuntimeMaps.floating(source, "perturbMagnitude", 0.419f),
                RuntimeMaps.integer(source, "erosionRadius", 1),
                RuntimeMaps.floating(source, "erosionTalus", 0.711f),
                RuntimeMaps.integer(source, "smoothRadius", 1),
                RuntimeMaps.floating(source, "smoothEffect", 0.7f),
                RuntimeMaps.integer(source, "iterations", 1)
        );
    }
}
