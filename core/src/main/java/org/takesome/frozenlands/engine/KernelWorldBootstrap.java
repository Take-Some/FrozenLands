package org.takesome.frozenlands.engine;

import com.jme3.app.state.AppStateManager;
import org.takesome.frozenlands.engine.bootstrap.WorldBootstrap;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.shaders.Shaders;
import org.takesome.frozenlands.engine.world.WorldUpdate;
import org.takesome.frozenlands.engine.world.sky.Sky;
import org.takesome.frozenlands.engine.world.spawn.SpawnManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;

import java.util.function.Consumer;

/** Builds and attaches the world-facing runtime services used by Kernel. */
public final class KernelWorldBootstrap {
    private final EngineContext context;
    private final AppStateManager stateManager;
    private final Consumer<Player> playerConsumer;

    public KernelWorldBootstrap(EngineContext context, AppStateManager stateManager, Consumer<Player> playerConsumer) {
        this.context = context;
        this.stateManager = stateManager;
        this.playerConsumer = playerConsumer;
    }

    public KernelWorldRuntime boot() {
        WorldBootstrap.WorldRuntime worldRuntime = new WorldBootstrap(context, playerConsumer).boot();
        stateManager.attach(worldRuntime.getShaders());
        stateManager.attach(worldRuntime.getWorldUpdate());
        stateManager.attach(worldRuntime.getSpawnManager());

        return new KernelWorldRuntime(
                worldRuntime.getSky(),
                worldRuntime.getTerrainManager(),
                worldRuntime.getShaders(),
                worldRuntime.getWorldUpdate(),
                worldRuntime.getSpawnManager()
        );
    }

    public record KernelWorldRuntime(
            Sky sky,
            TerrainManager terrainManager,
            Shaders shaders,
            WorldUpdate worldUpdate,
            SpawnManager spawnManager
    ) {
    }
}
