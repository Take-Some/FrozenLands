package org.takesome.frozenlands;

import com.jme3.app.Application;
import com.jme3.app.LegacyApplication;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.post.FilterPostProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.takesome.frozenlands.engine.Kernel;
import org.takesome.frozenlands.engine.config.ConfigReader;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.icons.IcoFileParser;
import org.takesome.frozenlands.engine.icons.selection.IcoImageSelector;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.takesome.frozenlands.logging.LoggingBootstrap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class FrozenLands implements Application {
    private static final String WINDOW_ICON_ASSET = "FrozenLands.ico";
    private static final String FOCUS_TOGGLE_MAPPING = "engine.application.focus.toggle";

    private final RuntimeHost runtime = new RuntimeHost();
    private final Node rootNode = new Node("Root Node");
    private final Node guiNode = new Node("Gui Node");
    private final Logger logger = LoggerFactory.getLogger(FrozenLands.class);

    private BulletAppState bulletAppState;
    private FilterPostProcessor fpp;
    private Kernel kernel;
    private Map CONFIG;
    private int numSamples;
    private boolean focusPaused;

    public static void main(String[] args) {
        LoggingBootstrap.install();
        FrozenLands app = new FrozenLands();
        AppSettings cfg = PreLaunchSettingsDialog.request("FrozenLands");
        if (cfg == null) {
            return;
        }
        setIcon(cfg);
        app.setSettings(cfg);
        app.start();
    }

    private void initializeRuntimeHost() {
        runtime.setLostFocusBehavior(LostFocusBehavior.Disabled);
        runtime.gainFocus();
        runtime.enableInput();
        initializeSceneGraph();
        installFocusToggleMapping();
        initializeRuntime();
    }

    private void updateRuntimeHost() {
        float tpf = runtime.getTimer().getTimePerFrame();
        runtime.getStateManager().update(tpf);
        rootNode.updateLogicalState(tpf);
        guiNode.updateLogicalState(tpf);
        rootNode.updateGeometricState();
        guiNode.updateGeometricState();
        RenderManager renderManager = runtime.getRenderManager();
        runtime.getStateManager().render(renderManager);
        renderManager.render(tpf, runtime.getContext().isRenderable());
        runtime.getStateManager().postRender();
    }

    private void initializeSceneGraph() {
        rootNode.setCullHint(Spatial.CullHint.Never);
        guiNode.setCullHint(Spatial.CullHint.Never);
        guiNode.setQueueBucket(RenderQueue.Bucket.Gui);
        runtime.getViewPort().attachScene(rootNode);
        runtime.getGuiViewPort().attachScene(guiNode);
    }

    private void initializeRuntime() {
        registerMutableAssetRoots();
        CONFIG = new ConfigReader(new String[]{"userInput"}).getCfgMaps();
        numSamples = runtime.getContext().getSettings().getSamples();
        bulletAppState = new BulletAppState();

        fpp = new FilterPostProcessor(runtime.getAssetManager());
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

        runtime.getStateManager().attach(bulletAppState);
        kernel = new Kernel(this);
        runtime.getStateManager().attach(kernel);
    }

    private void installFocusToggleMapping() {
        InputManager inputManager = runtime.getInputManager();
        if (!inputManager.hasMapping(FOCUS_TOGGLE_MAPPING)) {
            inputManager.addMapping(FOCUS_TOGGLE_MAPPING, new KeyTrigger(KeyInput.KEY_ESCAPE));
        }
        inputManager.addListener(runtime, FOCUS_TOGGLE_MAPPING);
    }

    private void cleanupFocusToggleMapping() {
        InputManager inputManager = runtime.getInputManager();
        if (inputManager == null) {
            return;
        }
        inputManager.removeListener(runtime);
        if (inputManager.hasMapping(FOCUS_TOGGLE_MAPPING)) {
            inputManager.deleteMapping(FOCUS_TOGGLE_MAPPING);
        }
    }

    private void registerMutableAssetRoots() {
        for (String assetRoot : ModuleIndexCatalog.defaultCatalog().assetRootPaths()) {
            registerMutableAssetRoot(assetRoot);
        }
    }

    private void registerMutableAssetRoot(String path) {
        File root = new File(path);
        if (root.isDirectory()) {
            runtime.getAssetManager().registerLocator(root.getPath(), FileLocator.class);
        }
    }

    private static void setIcon(AppSettings settings) {
        try {
            Path iconPath = resolveAssetPath(WINDOW_ICON_ASSET);
            if (iconPath == null) {
                return;
            }
            BufferedImage[] decodedIcons = new IcoFileParser().parse(iconPath);
            BufferedImage[] windowIcons = IcoImageSelector.pickBestIcons(decodedIcons);
            if (windowIcons.length > 0) {
                settings.setIcons(windowIcons);
            }
        } catch (IOException | IllegalArgumentException ignored) {
        }
    }

    private static Path resolveAssetPath(String assetPath) {
        for (String assetRoot : ModuleIndexCatalog.defaultCatalog().assetRootPaths()) {
            Path candidate = Path.of(assetRoot).resolve(assetPath).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public Node getGuiNode() {
        return guiNode;
    }

    public BulletAppState getBulletAppState() {
        return bulletAppState;
    }

    public FilterPostProcessor getFpp() {
        return fpp;
    }

    public Map getCONFIG() {
        return CONFIG;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override public LostFocusBehavior getLostFocusBehavior() { return runtime.getLostFocusBehavior(); }
    @Override public void setLostFocusBehavior(LostFocusBehavior behavior) { runtime.setLostFocusBehavior(behavior); }
    @Override public boolean isPauseOnLostFocus() { return runtime.isPauseOnLostFocus(); }
    @Override public void setPauseOnLostFocus(boolean pauseOnLostFocus) { runtime.setPauseOnLostFocus(pauseOnLostFocus); }
    @Override public void setSettings(AppSettings settings) { runtime.setSettings(settings); }
    @Override public void setTimer(Timer timer) { runtime.setTimer(timer); }
    @Override public Timer getTimer() { return runtime.getTimer(); }
    @Override public AssetManager getAssetManager() { return runtime.getAssetManager(); }
    @Override public InputManager getInputManager() { return runtime.getInputManager(); }
    @Override public AppStateManager getStateManager() { return runtime.getStateManager(); }
    @Override public RenderManager getRenderManager() { return runtime.getRenderManager(); }
    @Override public Renderer getRenderer() { return runtime.getRenderer(); }
    @Override public AudioRenderer getAudioRenderer() { return runtime.getAudioRenderer(); }
    @Override public Listener getListener() { return runtime.getListener(); }
    @Override public JmeContext getContext() { return runtime.getContext(); }
    @Override public Camera getCamera() { return runtime.getCamera(); }
    @Override public void start() { runtime.start(); }
    @Override public void start(boolean waitFor) { runtime.start(waitFor); }
    @Override public void setAppProfiler(AppProfiler profiler) { runtime.setAppProfiler(profiler); }
    @Override public AppProfiler getAppProfiler() { return runtime.getAppProfiler(); }
    @Override public void restart() { runtime.restart(); }
    @Override public void stop() { runtime.stop(); }
    @Override public void stop(boolean waitFor) { runtime.stop(waitFor); }
    @Override public <V> Future<V> enqueue(Callable<V> callable) { return runtime.enqueue(callable); }
    @Override public void enqueue(Runnable runnable) { runtime.enqueue(runnable); }
    @Override public ViewPort getGuiViewPort() { return runtime.getGuiViewPort(); }
    @Override public ViewPort getViewPort() { return runtime.getViewPort(); }

    private void toggleApplicationFocus() {
        setApplicationFocusPaused(!focusPaused, "escape");
    }

    private void setApplicationFocusPaused(boolean paused, String reason) {
        if (focusPaused == paused) {
            return;
        }
        focusPaused = paused;
        boolean focused = !paused;
        boolean cursorVisible = paused;

        if (runtime.getInputManager() != null) {
            runtime.getInputManager().setCursorVisible(cursorVisible);
        }

        if (kernel != null) {
            kernel.getModuleRegistry().publishEvent(EngineEventTopics.APPLICATION_FOCUS_CHANGED, Map.of(
                    "focused", focused,
                    "paused", paused,
                    "reason", reason
            ));
            kernel.getModuleRegistry().publishEvent(EngineEventTopics.CURSOR_VISIBILITY_REQUESTED, Map.of("visible", cursorVisible));
            kernel.getModuleRegistry().publishEvent(EngineEventTopics.CAMERA_FOLLOW_PAUSE_REQUESTED, Map.of("paused", paused));
            kernel.getModuleRegistry().publishEvent(EngineEventTopics.CAMERA_LOOK_INPUT_ENABLED_REQUESTED, Map.of("enabled", focused));
            kernel.getModuleRegistry().publishEvent(EngineEventTopics.PLAYER_INPUT_ENABLED_REQUESTED, Map.of("enabled", focused));
        }

        logger.info("Application focus {} by {}", focused ? "resumed" : "paused", reason);
    }

    private final class RuntimeHost extends LegacyApplication implements ActionListener {
        @Override
        public void initialize() {
            super.initialize();
            initializeRuntimeHost();
        }

        private void enableInput() {
            inputEnabled = true;
        }

        @Override
        public void update() {
            super.update();
            updateRuntimeHost();
        }

        @Override
        public void destroy() {
            cleanupFocusToggleMapping();
            super.destroy();
        }

        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (FOCUS_TOGGLE_MAPPING.equals(name) && isPressed) {
                toggleApplicationFocus();
            }
        }
    }
}
