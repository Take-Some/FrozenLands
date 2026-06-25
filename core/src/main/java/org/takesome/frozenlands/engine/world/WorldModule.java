package org.takesome.frozenlands.engine.world;

import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;
import org.takesome.frozenlands.engine.world.spawn.SpawnManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WorldModule implements EngineModule {
    public static final String ID = "engine.world";

    private final TerrainManager terrainManager;
    private final SpawnManager spawnManager;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    public WorldModule(TerrainManager terrainManager, SpawnManager spawnManager) {
        this.terrainManager = terrainManager;
        this.spawnManager = spawnManager;
        registerCommands();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "World runtime and spawn orchestration";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private void registerCommands() {
        commands.put("status", ModuleCommand.of("status", "Return world runtime status", args -> status()));
        commands.put("spawnPlayer", ModuleCommand.of("spawnPlayer", "Force player spawn when terrain is not ready yet", args -> {
            spawnManager.spawnNow();
            return spawnManager.status();
        }));
    }

    private Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("spawn", spawnManager.status());
        result.put("terrain", terrainManager.status());
        return result;
    }
}
