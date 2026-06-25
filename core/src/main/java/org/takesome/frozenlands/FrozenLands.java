package org.takesome.frozenlands;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.post.FilterPostProcessor;
import com.jme3.system.AppSettings;
import org.takesome.frozenlands.engine.Kernel;
import org.takesome.frozenlands.engine.config.ConfigReader;
import org.takesome.frozenlands.engine.icons.IcoFileParser;
import org.takesome.frozenlands.engine.icons.selection.IcoImageSelector;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FrozenLands extends SimpleApplication {
    private static final String WINDOW_ICON_ASSET = "FrozenLands.ico";


    private BulletAppState bulletAppState;
    private FilterPostProcessor fpp;
    private Map CONFIG;
    private int numSamples;

    private final Logger logger = LoggerFactory.getLogger(FrozenLands.class);

    public static void main(String[] args) {
        FrozenLands app = new FrozenLands();
        var cfg = new AppSettings(true);
        cfg.setVSync(false);
        cfg.setResolution(2560, 1440);
        cfg.setFullscreen(false);
        cfg.setSamples(16);
        cfg.setTitle("FrozenLands");
        app.setShowSettings(!Boolean.getBoolean("frozenlands.skipSettings"));
        app.setDisplayFps(true);
        app.setDisplayStatView(false);
        app.setSettings(cfg);
        setIcon(cfg);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        registerMutableAssetRoots();
        CONFIG = new ConfigReader(new String[]{"userInput"}).getCfgMaps();
        numSamples = getContext().getSettings().getSamples();
        bulletAppState = new BulletAppState();

        //bulletAppState.setDebugViewPorts(viewPort);
        //bulletAppState.setDebugEnabled(true);

        fpp = new FilterPostProcessor(assetManager);
        if (numSamples > 0) fpp.setNumSamples(numSamples);

        stateManager.attach(bulletAppState);
        stateManager.attach(new Kernel(this));
    }

    private void registerMutableAssetRoots() {
        for (String assetRoot : ModuleIndexCatalog.defaultCatalog().assetRootPaths()) {
            registerMutableAssetRoot(assetRoot);
        }
    }

    private void registerMutableAssetRoot(String path) {
        File root = new File(path);
        if (root.isDirectory()) {
            assetManager.registerLocator(root.getPath(), FileLocator.class);
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

    public BulletAppState getBulletAppState() {
        return this.bulletAppState;
    }

    public FilterPostProcessor getFpp() {
        return this.fpp;
    }

    public Map getCONFIG() {
        return this.CONFIG;
    }

    public Logger getLogger() {
        return this.logger;
    }
}
