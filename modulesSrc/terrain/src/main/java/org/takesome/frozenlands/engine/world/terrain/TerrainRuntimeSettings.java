package org.takesome.frozenlands.engine.world.terrain;

import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;

import java.util.Map;

public final class TerrainRuntimeSettings {
    private final LuaRuntimeConfig loader = new LuaRuntimeConfig();
    private final Map<String, Object> config = loader.read("engine.terrain");
    private final Map<String, Object> grid = loader.map(config, "grid");
    private final Map<String, Object> scale = loader.map(config, "scale");
    private final Map<String, Object> lod = loader.map(config, "lod");
    private final Map<String, Object> mountains = loader.map(config, "mountains");
    private final Map<String, Object> noise = loader.map(config, "noise");

    public int patchSize() { return loader.integer(grid, "patchSize", 65); }
    public int quadSize() { return loader.integer(grid, "quadSize", 257); }
    public float tileNoiseScale() { return loader.floating(grid, "tileNoiseScale", 96f); }
    public float scaleX() { return loader.floating(scale, "x", 1.35f); }
    public float scaleY() { return loader.floating(scale, "y", 1f); }
    public float scaleZ() { return loader.floating(scale, "z", 1.35f); }
    public int lodPatchSize() { return loader.integer(lod, "patchSize", 129); }
    public float lodMultiplier() { return loader.floating(lod, "multiplier", 2.2f); }
    public float mountainHeightOffset() { return loader.floating(mountains, "heightOffset", 50f); }
    public float roughness() { return loader.floating(noise, "roughness", 0.82f); }
    public float frequency() { return loader.floating(noise, "frequency", 0.1f); }
    public float amplitude() { return loader.floating(noise, "amplitude", 1.1f); }
    public float lacunarity() { return loader.floating(noise, "lacunarity", 2.12f); }
    public int octaves() { return loader.integer(noise, "octaves", 8); }
    public float noiseScale() { return loader.floating(noise, "scale", 0.02125f); }
}
