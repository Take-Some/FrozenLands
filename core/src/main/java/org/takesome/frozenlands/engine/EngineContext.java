package org.takesome.frozenlands.engine;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.takesome.frozenlands.engine.modules.ModuleRegistry;
import org.takesome.frozenlands.engine.providers.ProviderRegistry;

import java.util.Map;
import java.util.Optional;

public interface EngineContext {
    Node getRootNode();
    Node getGuiNode();
    ViewPort getViewPort();
    FilterPostProcessor getFpp();
    AssetManager getAssetManager();
    Map getConfig();
    AppStateManager appStateManager();
    InputManager getInputManager();
    Camera getCamera();
    Logger getLogger();
    BulletAppState getBulletAppState();
    ProviderRegistry getProviderRegistry();
    ModuleRegistry getModuleRegistry();

    <T> void registerService(Class<T> serviceType, T service);

    <T> Optional<T> findService(Class<T> serviceType);

    default <T> T requireService(Class<T> serviceType) {
        return findService(serviceType).orElseThrow(() -> new IllegalStateException(
                "Required engine runtime service is not registered: " + serviceType.getName()
        ));
    }

    default <T> T serviceOrNull(Class<T> serviceType) {
        return findService(serviceType).orElse(null);
    }
}
