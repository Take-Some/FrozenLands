package org.takesome.frozenlands.engine.bootstrap;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.shaders.Shaders;
import org.takesome.frozenlands.engine.world.WorldUpdate;
import org.takesome.frozenlands.engine.world.sky.Sky;
import org.takesome.frozenlands.engine.world.spawn.SpawnManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;

import java.util.function.Consumer;

public final class WorldBootstrap {
    private final EngineContext context;
    private final Consumer<Player> playerConsumer;

    public WorldBootstrap(EngineContext context, Consumer<Player> playerConsumer) {
        this.context = context;
        this.playerConsumer = playerConsumer;
    }

    public WorldRuntime boot() {
        Sky sky = new Sky(context);
        sky.addSky();

        TerrainManager terrainManager = new TerrainManager(context);
        context.getRootNode().attachChild(terrainManager.getTerrain());
        context.getRootNode().attachChild(terrainManager.getMountains());

        Shaders shaders = new Shaders(context);
        WorldUpdate worldUpdate = new WorldUpdate(context);
        SpawnManager spawnManager = new SpawnManager(context, terrainManager, playerConsumer);

        return new WorldRuntime(sky, terrainManager, shaders, worldUpdate, spawnManager);
    }

    public static final class WorldRuntime {
        private final Sky sky;
        private final TerrainManager terrainManager;
        private final Shaders shaders;
        private final WorldUpdate worldUpdate;
        private final SpawnManager spawnManager;

        private WorldRuntime(Sky sky, TerrainManager terrainManager, Shaders shaders, WorldUpdate worldUpdate, SpawnManager spawnManager) {
            this.sky = sky;
            this.terrainManager = terrainManager;
            this.shaders = shaders;
            this.worldUpdate = worldUpdate;
            this.spawnManager = spawnManager;
        }

        public Sky getSky() { return sky; }
        public TerrainManager getTerrainManager() { return terrainManager; }
        public Shaders getShaders() { return shaders; }
        public WorldUpdate getWorldUpdate() { return worldUpdate; }
        public SpawnManager getSpawnManager() { return spawnManager; }
    }
}
