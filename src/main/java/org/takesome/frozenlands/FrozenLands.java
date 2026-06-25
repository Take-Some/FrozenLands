package org.takesome.frozenlands;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.post.FilterPostProcessor;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import org.takesome.frozenlands.engine.Kernel;
import org.takesome.frozenlands.engine.config.ConfigReader;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FrozenLands extends SimpleApplication {

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
        GuiGlobals.initialize(this);
        numSamples = getContext().getSettings().getSamples();
        bulletAppState = new BulletAppState();

        //bulletAppState.setDebugViewPorts(viewPort);
        //bulletAppState.setDebugEnabled(true);

        GuiGlobals globals = GuiGlobals.getInstance();
        BaseStyles.loadStyleResources("themes/medieval/medieval.groovy");
        globals.getStyles().setDefaultStyle("medieval");

        fpp = new FilterPostProcessor(assetManager);
        if (numSamples > 0) fpp.setNumSamples(numSamples);

        stateManager.attach(bulletAppState);
        stateManager.attach(new Kernel(this));
    }

    private void registerMutableAssetRoots() {
        registerMutableAssetRoot("src/main/resources");
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
            BufferedImage[] icons = new BufferedImage[] {
                    ImageIO.read(FrozenLands.class.getResource( "/test64.png" )),
                    ImageIO.read(FrozenLands.class.getResource( "/test32.png" )),
                    ImageIO.read(FrozenLands.class.getResource( "/test16.png" ))
            };
            settings.setIcons(icons);
        } catch(IOException | IllegalArgumentException e) {}
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
