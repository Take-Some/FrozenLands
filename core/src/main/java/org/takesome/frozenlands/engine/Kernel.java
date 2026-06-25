package org.takesome.frozenlands.engine;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import org.takesome.frozenlands.FrozenLands;
import org.takesome.frozenlands.engine.core.CoreModule;
import org.takesome.frozenlands.engine.lua.RuntimeManifestReporter;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderRegistry;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;
import org.takesome.frozenlands.engine.providers.model.ModelProvider;
import org.takesome.frozenlands.engine.providers.sound.SoundProvider;
import org.takesome.frozenlands.engine.save.SaveManager;
import org.takesome.frozenlands.engine.shaders.Shaders;
import org.takesome.frozenlands.engine.world.WorldUpdate;
import org.takesome.frozenlands.engine.world.sky.Sky;
import org.takesome.frozenlands.engine.world.spawn.SpawnManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class Kernel extends BaseAppState implements EngineContext {

    private final FrozenLands frozenLands;
    private final Logger logger;
    private final Map CONFIG;


    private final EngineProviders providers;
    private final ProviderRegistry providerRegistry;
    private final ModuleRegistry moduleRegistry;
    private final CoreModule coreModule;
    protected final SoundProvider soundProvider;
    protected final MaterialProvider materialProvider;
    protected final ModelProvider modelProvider;

    protected final Sky sky;
    private final TerrainManager terrainManager;
    private final Shaders shaders;
    private final WorldUpdate worldUpdate;
    private final SpawnManager spawnManager;
    private final SaveManager saveManager;
    private Player player;

    public Kernel(FrozenLands frozenLands) {
        this.frozenLands = frozenLands;
        this.CONFIG = frozenLands.getCONFIG();
        this.logger = frozenLands.getLogger();
        this.providerRegistry = new ProviderRegistry();
        this.moduleRegistry = new ModuleRegistry();
        this.coreModule = new CoreModule(this);

        KernelProviderBootstrap.KernelProviderRuntime providerRuntime = new KernelProviderBootstrap(
                this,
                providerRegistry,
                moduleRegistry,
                coreModule
        ).boot();
        this.providers = providerRuntime.providers();
        this.soundProvider = providerRuntime.soundProvider();
        this.materialProvider = providerRuntime.materialProvider();
        this.modelProvider = providerRuntime.modelProvider();

        KernelWorldBootstrap.KernelWorldRuntime worldRuntime = new KernelWorldBootstrap(this, frozenLands.getStateManager(), this::setPlayer).boot();
        this.sky = worldRuntime.sky();
        this.terrainManager = worldRuntime.terrainManager();
        this.shaders = worldRuntime.shaders();
        this.worldUpdate = worldRuntime.worldUpdate();
        this.spawnManager = worldRuntime.spawnManager();

        KernelModuleInstaller.KernelModuleRuntime moduleRuntime = new KernelModuleInstaller(this, moduleRegistry).install(worldRuntime);
        this.saveManager = moduleRuntime.saveManager();
        runCoreAutoRunScripts();
        RuntimeManifestReporter.reportIfRequested(this);
        if (Boolean.getBoolean("frozenlands.runtimeManifestExit")) {
            frozenLands.stop();
        }
    }

    private void runCoreAutoRunScripts() {
        List<Map<String, Object>> results = coreModule.runConfiguredAutoRunScripts();
        for (Map<String, Object> result : results) {
            if (Boolean.TRUE.equals(result.get("ok"))) {
                logger.info("Lua autorun executed: {}", result.get("script"));
            } else {
                logger.error("Lua autorun failed: {} {}", result.get("script"), result.get("message"));
            }
        }
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
    }

    @Override
    public Map getConfig() {
        return CONFIG;
    }

    @Override
    public AssetManager getAssetManager() {
        return frozenLands.getAssetManager();
    }

    @Override
    public AppStateManager appStateManager() {
        return frozenLands.getStateManager();
    }

    @Override
    public InputManager getInputManager() {
        return frozenLands.getInputManager();
    }

    @Override
    public SoundProvider getSoundManager() {
        return soundProvider;
    }

    @Override
    public MaterialProvider getMaterialManager() {
        return materialProvider;
    }

    @Override
    public Camera getCamera() {
        return frozenLands.getCamera();
    }

    @Override
    public BulletAppState getBulletAppState() {
        return frozenLands.getBulletAppState();
    }

    @Override
    public Node getRootNode() {
        return frozenLands.getRootNode();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    private void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public Sky getSky() {
        return sky;
    }

    @Override
    public ViewPort getViewPort() {
        return frozenLands.getViewPort();
    }

    @Override
    public Node getGuiNode() {
        return frozenLands.getGuiNode();
    }

    @Override
    public FilterPostProcessor getFpp() {
        return frozenLands.getFpp();
    }

    @Override
    public EngineProviders getProviders() {
        return providers;
    }

    @Override
    public ProviderRegistry getProviderRegistry() {
        return providerRegistry;
    }

    @Override
    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    public TerrainManager getTerrainManager() {
        return terrainManager;
    }

    public Shaders getShaders() {
        return shaders;
    }

    public WorldUpdate getWorldUpdate() {
        return worldUpdate;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public SaveManager getSaveManager() {
        return saveManager;
    }
}
