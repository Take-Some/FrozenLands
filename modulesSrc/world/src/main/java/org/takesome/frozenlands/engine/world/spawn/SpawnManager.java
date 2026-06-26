package org.takesome.frozenlands.engine.world.spawn;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpawnManager extends BaseAppState {
    private final EngineContext context;
    private final TerrainManager terrainManager;
    private final float spawnX;
    private final float spawnZ;
    private final float spawnClearance;
    private final Vector3f fallbackSpawn;
    private Player player;
    private int framesWaited;

    public SpawnManager(EngineContext context, TerrainManager terrainManager) {
        this.context = context;
        this.terrainManager = terrainManager;

        LuaRuntimeConfig loader = new LuaRuntimeConfig();
        Map<String, Object> config = loader.read("engine.world");
        Map<String, Object> spawn = loader.map(config, "spawn");
        this.spawnX = loader.floating(spawn, "x", 0f);
        this.spawnZ = loader.floating(spawn, "z", 0f);
        this.spawnClearance = loader.floating(spawn, "clearance", 4f);
        this.fallbackSpawn = new Vector3f(loader.floating(spawn, "fallbackX", 0f),
                loader.floating(spawn, "fallbackY", 150f),
                loader.floating(spawn, "fallbackZ", 0f));
    }

    @Override
    protected void initialize(Application application) {
    }

    @Override
    protected void cleanup(Application application) {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        if (player != null) {
            return;
        }
        framesWaited++;

        Optional<Vector3f> terrainSpawn = terrainManager.findSafeSpawnLocation(spawnX, spawnZ, spawnClearance);
        if (terrainSpawn.isPresent()) {
            spawn(terrainSpawn.get(), "terrain-ready");
        }
    }

    public Player spawnNow() {
        if (player != null) {
            return player;
        }
        Vector3f spawnLocation = terrainManager.findSafeSpawnLocation(spawnX, spawnZ, spawnClearance)
                .orElse(fallbackSpawn.clone());
        spawn(spawnLocation, "manual");
        return player;
    }

    public boolean isSpawned() {
        return player != null;
    }

    public Player getPlayer() {
        return player;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("spawned", isSpawned());
        status.put("framesWaited", framesWaited);
        status.put("terrainReady", terrainManager.findSafeSpawnLocation(spawnX, spawnZ, spawnClearance).isPresent());
        if (player != null) {
            Vector3f position = player.getPlayerPosition();
            status.put("x", position.x);
            status.put("y", position.y);
            status.put("z", position.z);
        }
        return status;
    }

    private void spawn(Vector3f spawnLocation, String reason) {
        player = new Player(context);
        player.addPlayer(context.getCamera(), spawnLocation);
        context.registerService(Player.class, player);
        context.getModuleRegistry().publishEvent("world.player.spawned", Map.of(
                "reason", reason,
                "x", spawnLocation.x,
                "y", spawnLocation.y,
                "z", spawnLocation.z
        ));
    }
}
