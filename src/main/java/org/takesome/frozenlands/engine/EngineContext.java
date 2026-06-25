package org.takesome.frozenlands.engine;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderRegistry;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;
import org.takesome.frozenlands.engine.providers.sound.SoundProvider;
import org.takesome.frozenlands.engine.world.sky.Sky;
import org.slf4j.Logger;

import java.util.Map;

public interface EngineContext {
    Node getRootNode();
    Node getGuiNode();
    Sky getSky();
    ViewPort getViewPort();
    FilterPostProcessor getFpp();
    AssetManager getAssetManager();
    Map getConfig();
    AppStateManager appStateManager();
    InputManager getInputManager();
    Player getPlayer();
    Camera getCamera();
    Logger getLogger();
    SoundProvider getSoundManager();
    MaterialProvider getMaterialManager();
    BulletAppState getBulletAppState();
    EngineProviders getProviders();
    ProviderRegistry getProviderRegistry();
    ModuleRegistry getModuleRegistry();
}

