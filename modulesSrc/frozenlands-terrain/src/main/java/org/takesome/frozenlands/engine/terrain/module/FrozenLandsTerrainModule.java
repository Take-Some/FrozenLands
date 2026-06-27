package org.takesome.frozenlands.engine.terrain.module;

import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;
import org.takesome.frozenlands.engine.terrain.TerrainService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class FrozenLandsTerrainModule implements EngineModule {
    static final String ID = "frozenlands.terrain";

    private final TerrainService terrainService;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    FrozenLandsTerrainModule(TerrainService terrainService) {
        this.terrainService = terrainService;
        commands.put("status", ModuleCommand.of("status", "Return terrain service status", args -> terrainService.status()));
        commands.put("profile", ModuleCommand.of("profile", "Return active terrain profile", args -> terrainService.profile().toMap()));
        commands.put("sample", ModuleCommand.of("sample", "Sample terrain at world x/z", this::sample));
        commands.put("chunk", ModuleCommand.of("chunk", "Generate or return cached terrain chunk summary", this::chunk));
        commands.put("clearCache", ModuleCommand.of("clearCache", "Clear terrain service cache", args -> {
            terrainService.clearCache();
            return terrainService.status();
        }));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "FrozenLands deterministic terrain generation service";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private Map<String, Object> sample(Map<String, Object> args) {
        long seed = RuntimeMaps.integer(args, "seed", 0);
        float x = RuntimeMaps.floating(args, "x", 0f);
        float z = RuntimeMaps.floating(args, "z", 0f);
        return terrainService.sample(seed, x, z).toMap();
    }

    private Map<String, Object> chunk(Map<String, Object> args) {
        long seed = RuntimeMaps.integer(args, "seed", 0);
        int chunkX = RuntimeMaps.integer(args, "chunkX", 0);
        int chunkZ = RuntimeMaps.integer(args, "chunkZ", 0);
        return terrainService.chunk(seed, chunkX, chunkZ).summary();
    }
}
