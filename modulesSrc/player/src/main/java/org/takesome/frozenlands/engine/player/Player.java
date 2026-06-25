package org.takesome.frozenlands.engine.player;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.player.camera.PlayerCameraRig;
import org.takesome.frozenlands.engine.player.input.FPSViewControl;
import org.takesome.frozenlands.engine.player.input.UserInputHandler;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.Map;
import java.util.function.Consumer;

public class Player extends Node {
    private final EngineContext kernelInterface;
    private final PhysicsSpace pspace;
    private final PlayerOptions playerOptions;
    private final PlayerSoundProvider playerSoundProvider;
    private final PlayerModel playerModel;

    private UserInputHandler userInputHandler;
    private PlayerCameraRig cameraRig;
    private PlayerAnimationController animationController;
    private PlayerHeadTurnController headTurnController;
    private ActionsControl actionsControl;

    public Player(EngineContext kernel) {
        this.kernelInterface = kernel;
        this.pspace = kernel.getBulletAppState().getPhysicsSpace();
        this.playerOptions = PlayerOptions.load(ModuleIndexCatalog.defaultCatalog().configPath(PlayerModule.ID, "playerOptions"));
        this.playerSoundProvider = new PlayerSoundProvider(kernel);
        this.playerModel = new PlayerModel(kernel.getAssetManager(), playerOptions);
        this.playerModel.setCullHint(playerOptions.getCullHint());
        this.playerModel.setShadowMode(playerOptions.getShadowMode());
        attachChild(playerModel);
    }

    public void addPlayer(Camera cam, Vector3f spawnPoint) {
        attachToSceneGraph();
        initializeCollisionFromModel(spawnPoint);
        initializeRuntimeControls(cam);
        pspace.addAll(this);
        onSpawn();
    }

    private void attachToSceneGraph() {
        if (getParent() == null) {
            kernelInterface.getRootNode().attachChild(this);
        }
    }

    private void initializeCollisionFromModel(Vector3f spawnPoint) {
        PlayerCollisionProfile collision = playerModel.getCollisionProfile();
        BetterCharacterControl character = new BetterCharacterControl(collision.radius(), collision.height(), collision.mass());
        character.setJumpForce(playerOptions.getJumpForce());
        playerOptions.setCharacterControl(character);
        addControl(character);
        character.warp(spawnPoint);
        getLogger().info(
                "Player collision from {} radius={} height={} mass={} boundsCenter={} boundsExtent={}",
                collision.source(),
                collision.radius(),
                collision.height(),
                collision.mass(),
                collision.boundsCenter(),
                collision.boundsExtent()
        );
    }

    private void initializeRuntimeControls(Camera cam) {
        actionsControl = new ActionsControl(this);
        addControl(actionsControl);

        userInputHandler = new UserInputHandler(this, () -> actionsControl.shot(
                kernelInterface.getAssetManager(),
                cam.getLocation().add(cam.getDirection().mult(1)),
                cam.getDirection(),
                kernelInterface.getRootNode(),
                pspace
        ));
        setPlayerHealth(playerOptions.getInitialHealth());
        addControl(userInputHandler);
        cameraRig = new PlayerCameraRig(this, cam, kernelInterface);
        animationController = new PlayerAnimationController(this);
        addControl(animationController);
        headTurnController = new PlayerHeadTurnController(this);
        playerModel.getPlayerSpatial().addControl(headTurnController);
        addControl(new FPSViewControl(FPSViewControl.Mode.WORLD_SCENE));
        logSpawnDiagnostics(cam);
    }

    private void onSpawn() {
        playerSoundProvider.playSound("spawn");
    }

    private void logSpawnDiagnostics(Camera cam) {
        updateGeometricState();
        playerModel.updateGeometricState();
        playerModel.getPlayerSpatial().updateGeometricState();
        getLogger().info(
                "Player spawn diagnostics playerWorld={} modelWorld={} visualWorld={} cameraLocation={} viewMode={} playerCull={} modelCull={} visualCull={}",
                getWorldTranslation(),
                playerModel.getWorldTranslation(),
                playerModel.getPlayerSpatial().getWorldTranslation(),
                cam.getLocation(),
                cameraRig == null ? "<none>" : cameraRig.viewMode(),
                getCullHint(),
                playerModel.getCullHint(),
                playerModel.getPlayerSpatial().getCullHint()
        );
    }

    public Map<String, Object> publishEvent(String topic, Map<String, Object> payload) {
        return kernelInterface.getModuleRegistry().publishEvent(topic, payload);
    }

    public Map<String, Object> publishLiveEvent(String topic, Map<String, Object> payload) {
        return kernelInterface.getModuleRegistry().publishLiveEvent(topic, payload);
    }

    public AutoCloseable subscribeEvent(String topic, Consumer<Map<String, Object>> listener) {
        return subscribeEvent(topic, listener, false);
    }

    public AutoCloseable subscribeEvent(String topic, Consumer<Map<String, Object>> listener, boolean replayLatest) {
        return kernelInterface.getModuleRegistry().getEventBus().subscribe(topic, listener, replayLatest);
    }

    public int runtimeId() {
        return System.identityHashCode(this);
    }

    public Logger getLogger() {
        return kernelInterface.getLogger();
    }

    public Vector3f getPlayerPosition() {
        if (userInputHandler != null) {
            return userInputHandler.getPlayerPosition();
        }
        Vector3f worldTranslation = getWorldTranslation();
        return new Vector3f(Math.round(worldTranslation.x), Math.round(worldTranslation.y), Math.round(worldTranslation.z));
    }

    public UserInputHandler getUserInputHandler() { return userInputHandler; }
    public PlayerCameraRig getCameraRig() { return cameraRig; }
    public PlayerAnimationController getAnimationController() { return animationController; }
    public PlayerHeadTurnController getHeadTurnController() { return headTurnController; }
    public AssetManager getAssetManager() { return kernelInterface.getAssetManager(); }
    public AppStateManager getStateManager() { return kernelInterface.appStateManager(); }
    public InputManager getInputManager() { return kernelInterface.getInputManager(); }
    public Node getRootNode() { return kernelInterface.getRootNode(); }
    public Node getGuiNode() { return kernelInterface.getGuiNode(); }
    public Map getConfig() { return kernelInterface.getConfig(); }
    public PlayerOptions getPlayerOptions() { return playerOptions; }
    public PlayerModel getPlayerModel() { return playerModel; }
    public PlayerSoundProvider getPlayerSoundProvider() { return playerSoundProvider; }

    public void setPlayerHealth(int health) {
        playerOptions.setInitialHealth(health);
    }
}
