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
import org.takesome.frozenlands.engine.bootstrap.WorldBootstrap;
import org.takesome.frozenlands.engine.core.CoreModule;
import org.takesome.frozenlands.engine.lua.RuntimeManifestReporter;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.player.PlayerModule;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderRegistry;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;
import org.takesome.frozenlands.engine.providers.model.ModelProvider;
import org.takesome.frozenlands.engine.providers.sound.SoundProvider;
import org.takesome.frozenlands.engine.save.SaveManager;
import org.takesome.frozenlands.engine.save.SaveModule;
import org.takesome.frozenlands.engine.shaders.ShaderModule;
import org.takesome.frozenlands.engine.shaders.Shaders;
import org.takesome.frozenlands.engine.world.WorldModule;
import org.takesome.frozenlands.engine.world.WorldUpdate;
import org.takesome.frozenlands.engine.world.effect.ParticleModule;
import org.takesome.frozenlands.engine.world.sky.Sky;
import org.takesome.frozenlands.engine.world.spawn.SpawnManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainManager;
import org.takesome.frozenlands.engine.world.terrain.TerrainModule;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class Kernel extends BaseAppState implements EngineContext {

    private final FrozenLands frozenLands;
    private final Logger logger;
    private final Map CONFIG;

    protected final AssetManager assetManager;
    protected final ViewPort viewPort;
    protected final AppStateManager stateManager;
    protected final Camera camera;
    protected final BulletAppState bulletAppState;
    protected final Node rootNode;
    protected final Node guiNode;
    protected final FilterPostProcessor fpp;
    protected final InputManager inputManager;

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
    protected Player player;

    public Kernel(FrozenLands frozenLands) {
        this.frozenLands = frozenLands;
        this.stateManager = frozenLands.getStateManager();
        this.assetManager = frozenLands.getAssetManager();
        this.inputManager = frozenLands.getInputManager();
        this.camera = frozenLands.getCamera();
        this.rootNode = frozenLands.getRootNode();
        this.viewPort = frozenLands.getViewPort();
        this.bulletAppState = frozenLands.getBulletAppState();
        this.fpp = frozenLands.getFpp();
        this.CONFIG = frozenLands.getCONFIG();
        this.logger = frozenLands.getLogger();
        this.guiNode = frozenLands.getGuiNode();

        this.providerRegistry = new ProviderRegistry();
        this.moduleRegistry = new ModuleRegistry();
        this.coreModule = new CoreModule(this);
        this.moduleRegistry.register(coreModule, this);

        this.providers = EngineProviders.bootstrap(this, providerRegistry);
        this.soundProvider = providers.getSoundProvider();
        this.materialProvider = providers.getMaterialProvider();
        this.modelProvider = providers.getModelProvider();
        this.moduleRegistry.register(EngineModule.descriptor("engine.providers", "Provider registry exposed to Lua bridge"), this);

        WorldBootstrap.WorldRuntime worldRuntime = new WorldBootstrap(this, this::setPlayer).boot();
        this.sky = worldRuntime.getSky();
        this.terrainManager = worldRuntime.getTerrainManager();
        this.shaders = worldRuntime.getShaders();
        this.worldUpdate = worldRuntime.getWorldUpdate();
        this.spawnManager = worldRuntime.getSpawnManager();
        this.stateManager.attach(shaders);
        this.stateManager.attach(worldUpdate);
        this.stateManager.attach(spawnManager);
        this.moduleRegistry.register(new WorldModule(terrainManager, spawnManager), this);
        this.moduleRegistry.register(new TerrainModule(terrainManager), this);
        this.moduleRegistry.register(new ShaderModule(shaders), this);
        this.moduleRegistry.register(new ParticleModule(worldUpdate.getParticleManager()), this);
        this.moduleRegistry.register(new PlayerModule(this), this);
        this.saveManager = new SaveManager(this);
        this.moduleRegistry.register(new SaveModule(saveManager), this);
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
        return assetManager;
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
        return camera;
    }

    @Override
    public BulletAppState getBulletAppState() {
        return bulletAppState;
    }

    @Override
    public Node getRootNode() {
        return rootNode;
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
        return this.viewPort;
    }

    @Override
    public Node getGuiNode() {
        return guiNode;
    }

    @Override
    public FilterPostProcessor getFpp() {
        return this.fpp;
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
