package org.takesome.frozenlands.engine.ui.html;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.ui.Picture;
import org.takesome.frozenlands.engine.EngineContext;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

public final class HtmlUiState extends BaseAppState implements ActionListener {
    public static final String SERVICE_ID = "ui.html.runtime";
    public static final String DEVTOOLS_MAPPING = "ui.html.devtools.toggle";
    public static final String TOGGLE_MAPPING = "ui.html.screen.toggle";

    private static final float REFRESH_INTERVAL_SECONDS = 1f / 20f;

    private final EngineContext context;
    private final AWTLoader awtLoader = new AWTLoader();

    private FrozenLandsHtmlUiRuntime runtime;
    private JmeHtmlDomInputBridge inputBridge;
    private Picture surface;
    private Texture2D texture;
    private float refreshAccumulator;
    private int lastWidth;
    private int lastHeight;
    private long lastDocumentVersion = -1L;
    private boolean runtimeAvailable;
    private boolean devToolsMappingInstalled;
    private boolean screenToggleMappingInstalled;

    public HtmlUiState(EngineContext context) {
        this.context = context;
    }

    @Override
    protected void initialize(Application application) {
        if (Boolean.getBoolean("frozenlands.htmlUi.disabled")) {
            context.getLogger().warn("Html UI state disabled by system property frozenlands.htmlUi.disabled=true");
            return;
        }

        try {
            runtime = FrozenLandsHtmlUiRuntime.loadDefault(context);
            runtimeAvailable = true;
            context.registerService(SERVICE_ID, FrozenLandsHtmlUiRuntime.class, runtime);
            context.registerService(FrozenLandsHtmlUiRuntime.class, runtime);
            installDevToolsMapping();
            installInputBridge();
            createSurface();
            renderSurface(true);
            context.getLogger().info(
                    "Html UI mounted root={} entry={} styles={} scripts={} screen={}",
                    runtime.uiRoot(),
                    runtime.manifest().getEntry(),
                    runtime.manifest().getStyles().size(),
                    runtime.loadedScripts().size(),
                    runtime.screen()
            );
        } catch (IOException | RuntimeException exception) {
            runtimeAvailable = false;
            uninstallInputBridge();
            removeSurface();
            texture = null;
            runtime = null;
            context.getLogger().error("Html UI mount failed; continuing without HTML UI: {}", exception.toString(), exception);
        }
    }

    @Override
    protected void cleanup(Application application) {
        uninstallInputBridge();
        uninstallDevToolsMapping();
        removeSurface();
        texture = null;
        runtime = null;
        runtimeAvailable = false;
    }

    @Override
    protected void onEnable() {
        if (surface != null && texture != null) {
            surface.setCullHint(Picture.CullHint.Never);
        }
    }

    @Override
    protected void onDisable() {
        if (surface != null) {
            surface.setCullHint(Picture.CullHint.Always);
        }
    }

    @Override
    public void update(float tpf) {
        if (!runtimeAvailable || runtime == null || surface == null) {
            return;
        }

        refreshAccumulator += tpf;
        int width = width();
        int height = height();
        boolean resized = width != lastWidth || height != lastHeight;
        boolean mutated = runtime.documentVersion() != lastDocumentVersion;
        boolean timedRefresh = refreshAccumulator >= REFRESH_INTERVAL_SECONDS;
        if (resized || mutated || timedRefresh) {
            renderSurface(true);
            refreshAccumulator = 0f;
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed || runtime == null) {
            return;
        }
        if (DEVTOOLS_MAPPING.equals(name)) {
            try {
                runtime.openDevTools();
            } catch (RuntimeException exception) {
                context.getLogger().error("Html UI DevTools open failed: {}", exception.toString(), exception);
            }
            return;
        }
        if (TOGGLE_MAPPING.equals(name)) {
            toggleScreen();
        }
    }

    public boolean isRuntimeAvailable() {
        return runtimeAvailable;
    }

    public FrozenLandsHtmlUiRuntime runtime() {
        return runtime;
    }

    public Map<String, Object> status() {
        return Map.of(
                "available", runtimeAvailable,
                "screen", runtime == null ? "" : runtime.screen(),
                "documentVersion", runtime == null ? -1L : runtime.documentVersion(),
                "width", lastWidth,
                "height", lastHeight
        );
    }

    public void setScreen(String screen) {
        if (runtime == null) {
            return;
        }
        runtime.setScreen(screen);
        renderSurface(true);
    }

    public boolean handleEscape() {
        if (!runtimeAvailable || runtime == null) {
            return false;
        }
        String current = runtime.screen();
        String next = "pause".equals(current) ? "hud" : "pause";
        setScreen(next);
        context.getLogger().info("Html UI escape handled screen={} next={}", current, next);
        return true;
    }

    private void toggleScreen() {
        if (runtime == null) {
            return;
        }
        String current = runtime.screen();
        String next = "pause".equals(current) ? "hud" : "pause";
        setScreen(next);
        context.getLogger().info("Html UI screen toggled screen={}", next);
    }

    private void createSurface() {
        surface = new Picture("HtmlDomUiSurface");
        surface.setQueueBucket(RenderQueue.Bucket.Gui);
        surface.setCullHint(Picture.CullHint.Always);
        context.getGuiNode().attachChild(surface);
    }

    private void removeSurface() {
        if (surface != null) {
            surface.removeFromParent();
            surface = null;
        }
    }

    private void renderSurface(boolean force) {
        int width = width();
        int height = height();
        if (width <= 0 || height <= 0 || runtime == null || surface == null) {
            return;
        }
        if (!force && width == lastWidth && height == lastHeight && runtime.documentVersion() == lastDocumentVersion) {
            return;
        }

        BufferedImage image = runtime.renderToImage(width, height);
        Image jmeImage = awtLoader.load(image, true);
        if (texture == null) {
            texture = new Texture2D(jmeImage);
            texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            texture.setMagFilter(Texture.MagFilter.Bilinear);
            surface.setTexture(context.getAssetManager(), texture, true);
        } else {
            texture.setImage(jmeImage);
        }

        surface.setWidth(width);
        surface.setHeight(height);
        surface.setPosition(0f, 0f);
        surface.setCullHint(isEnabled() ? Picture.CullHint.Never : Picture.CullHint.Always);
        lastWidth = width;
        lastHeight = height;
        lastDocumentVersion = runtime.documentVersion();
    }

    private int width() {
        Camera camera = context.getCamera();
        return camera == null ? 1 : Math.max(1, camera.getWidth());
    }

    private int height() {
        Camera camera = context.getCamera();
        return camera == null ? 1 : Math.max(1, camera.getHeight());
    }

    private void installInputBridge() {
        if (context.getInputManager() == null || runtime == null) {
            return;
        }
        inputBridge = new JmeHtmlDomInputBridge(context, runtime);
        context.getInputManager().addRawInputListener(inputBridge);
    }

    private void uninstallInputBridge() {
        if (context.getInputManager() == null || inputBridge == null) {
            return;
        }
        context.getInputManager().removeRawInputListener(inputBridge);
        inputBridge = null;
    }

    private void installDevToolsMapping() {
        InputManager inputManager = context.getInputManager();
        if (inputManager == null) {
            return;
        }
        if (!inputManager.hasMapping(DEVTOOLS_MAPPING)) {
            inputManager.addMapping(DEVTOOLS_MAPPING, new KeyTrigger(KeyInput.KEY_F12));
            devToolsMappingInstalled = true;
        }
        if (!inputManager.hasMapping(TOGGLE_MAPPING)) {
            inputManager.addMapping(TOGGLE_MAPPING, new KeyTrigger(KeyInput.KEY_F10));
            screenToggleMappingInstalled = true;
        }
        inputManager.addListener(this, DEVTOOLS_MAPPING, TOGGLE_MAPPING);
    }

    private void uninstallDevToolsMapping() {
        InputManager inputManager = context.getInputManager();
        if (inputManager == null) {
            return;
        }
        inputManager.removeListener(this);
        if (devToolsMappingInstalled && inputManager.hasMapping(DEVTOOLS_MAPPING)) {
            inputManager.deleteMapping(DEVTOOLS_MAPPING);
        }
        if (screenToggleMappingInstalled && inputManager.hasMapping(TOGGLE_MAPPING)) {
            inputManager.deleteMapping(TOGGLE_MAPPING);
        }
        devToolsMappingInstalled = false;
        screenToggleMappingInstalled = false;
    }
}
