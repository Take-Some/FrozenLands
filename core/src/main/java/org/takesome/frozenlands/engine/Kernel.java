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
import org.slf4j.Logger;
import org.takesome.frozenlands.FrozenLands;
import org.takesome.frozenlands.engine.core.CoreModule;
import org.takesome.frozenlands.engine.core.LuaEventHookState;
import org.takesome.frozenlands.engine.core.console.ConsoleCursorPolicyState;
import org.takesome.frozenlands.engine.core.console.ConsoleInteractionPolicyState;
import org.takesome.frozenlands.engine.core.console.CoreConsoleState;
import org.takesome.frozenlands.engine.lua.RuntimeManifestReporter;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.providers.ProviderRegistry;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class Kernel extends BaseAppState implements EngineContext {
    private final FrozenLands frozenLands;
    private final Logger logger;
    private final Map CONFIG;
    private final ProviderRegistry providerRegistry;
    private final ModuleRegistry moduleRegistry;
    private final CoreModule coreModule;
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();

    public Kernel(FrozenLands frozenLands) {
        this.frozenLands = frozenLands;
        this.CONFIG = frozenLands.getCONFIG();
        this.logger = frozenLands.getLogger();
        this.providerRegistry = new ProviderRegistry();
        this.moduleRegistry = new ModuleRegistry();
        this.coreModule = new CoreModule(this);

        registerService(ProviderRegistry.class, providerRegistry);
        registerService(ModuleRegistry.class, moduleRegistry);
        registerService(CoreModule.class, coreModule);

        moduleRegistry.register(coreModule, this);
        installRuntimeModules();
        attachCoreAppStates();
        runCoreAutoRunScripts();
        RuntimeManifestReporter.reportIfRequested(this);
        if (Boolean.getBoolean("frozenlands.runtimeManifestExit")) {
            frozenLands.stop();
        }
    }

    private void installRuntimeModules() {
        List<EngineRuntimeInstaller> installers = ServiceLoader.load(EngineRuntimeInstaller.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator
                        .comparingInt(EngineRuntimeInstaller::priority)
                        .thenComparing(EngineRuntimeInstaller::id)
                        .thenComparing(installer -> installer.getClass().getName()))
                .toList();

        for (EngineRuntimeInstaller installer : installers) {
            logger.info("Installing FrozenLands runtime module: {} [{}]", installer.id(), installer.getClass().getName());
            installer.install(this);
        }
    }

    private void attachCoreAppStates() {
        appStateManager().attach(new ConsoleInteractionPolicyState(this));
        appStateManager().attach(new ConsoleCursorPolicyState(this));
        appStateManager().attach(new CoreConsoleState(this));
        appStateManager().attach(new LuaEventHookState(this));
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
        return logger;
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
    public ProviderRegistry getProviderRegistry() {
        return providerRegistry;
    }

    @Override
    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    @Override
    public synchronized <T> void registerService(Class<T> serviceType, T service) {
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service must not be null: " + serviceType.getName());
        }
        services.put(serviceType, serviceType.cast(service));
    }

    @Override
    public synchronized <T> Optional<T> findService(Class<T> serviceType) {
        if (serviceType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(services.get(serviceType)).map(serviceType::cast);
    }
}
