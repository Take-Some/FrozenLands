package org.takesome.frozenlands.engine.player;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.player.camera.PlayerCameraRig;
import org.takesome.frozenlands.engine.player.input.FPSViewControl;
import org.takesome.frozenlands.engine.player.input.UserInputHandler;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Consumer;

public class Player extends Node {

    private EngineContext kernelInterface;
    private PlayerOptions playerOptions;
    private PlayerSoundProvider playerSoundProvider;
    private PlayerModel playerModel;
    private UserInputHandler userInputHandler;
    private PlayerCameraRig cameraRig;
    private PhysicsSpace pspace;

    public Player(EngineContext kernel) {
        this.kernelInterface = kernel;
        this.pspace = kernel.getBulletAppState().getPhysicsSpace();

        playerOptions = PlayerOptions.load(ModuleIndexCatalog.defaultCatalog().configPath(PlayerModule.ID, "playerOptions"));
        playerSoundProvider = new PlayerSoundProvider(kernel);
        playerModel = new PlayerModel(kernel.getAssetManager(), playerOptions);
        playerModel.setCullHint(playerOptions.getCullHint());
        playerModel.setShadowMode(playerOptions.getShadowMode());
        this.attachChild(playerModel);
    }

    private void onSpawn() {
        this.playerSoundProvider.playSound("spawn");
    }

    public void addPlayer(Camera cam, Vector3f spawnPoint) {
        Player fpsPlayer = (Player) this.clone();
        playerOptions.setFpsCam(cam.clone());
        this.loadFPSLogicWorld(cam, fpsPlayer, spawnPoint);
        fpsPlayer.loadFPSLogicFPSView(cam, this.playerOptions.getFpsCam(), this.playerModel.getPlayerSpatial());
        pspace.addAll(this);
        kernelInterface.getRootNode().attachChild(this);
    }

    public void loadFPSLogicWorld(Camera cam, Spatial playerModel, Vector3f spawnPoint){
        updateModelBound();
        updateGeometricState();

        BoundingBox playerBounds = null;
        if (getWorldBound() instanceof BoundingBox) {
            playerBounds = (BoundingBox) getWorldBound();
        }

        float characterRadius = 0.35f;
        float characterHeight = 1.8f;
        if (playerBounds != null) {
            characterRadius = Math.max(0.25f, Math.max(playerBounds.getXExtent(), playerBounds.getZExtent()));
            characterHeight = Math.max(1.0f, playerBounds.getYExtent() * 4f);
        } else {
            getLogger().warn("Player world bound is not ready, using fallback character capsule size");
        }

        getLogger().debug("Player capsule radius={} height={}", characterRadius, characterHeight);
        playerOptions.setCharacterControl(new BetterCharacterControl(characterRadius, characterHeight, playerOptions.getMass()));
        playerOptions.getCharacterControl().setJumpForce(playerOptions.getJumpForce());
        addControl(playerOptions.getCharacterControl());

        // Spawn position
        playerOptions.getCharacterControl().warp(spawnPoint);

        userInputHandler = new UserInputHandler(this, ()-> playerModel.getControl(ActionsControl.class).shot(kernelInterface.getAssetManager(),cam.getLocation().add(cam.getDirection().mult(1)),cam.getDirection(),kernelInterface.getRootNode(), this.pspace));
        this.setPlayerHealth(playerOptions.getInitialHealth());

        // Load playerOptions logic
        addControl(userInputHandler);
        cameraRig = new PlayerCameraRig(this, getUserInputHandler(), cam, kernelInterface);
        addControl(new ActionsControl(this));
        addControl(new FPSViewControl(FPSViewControl.Mode.WORLD_SCENE));
        this.onSpawn();
    }

    public void loadFPSLogicFPSView(Camera cam, Camera fpsCam, Spatial playerSpatial) {
        addControl(new AbstractControl() {
            @Override
            protected void controlUpdate(float tpf) {
                setLocalTransform(playerSpatial.getWorldTransform());
                fpsCam.setLocation(cam.getLocation());
                fpsCam.lookAtDirection(cam.getDirection(), cam.getUp());
            }

            @Override
            protected void controlRender(RenderManager rm, ViewPort vp) {
            }
        });
        addControl(new FPSViewControl(FPSViewControl.Mode.WORLD_SCENE));
        addControl(new ActionsControl(this));
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
        return new Vector3f(Math.round(worldTranslation.x),
                Math.round(worldTranslation.y),
                Math.round(worldTranslation.z));
    }

    public UserInputHandler getUserInputHandler() {
        return userInputHandler;
    }

    public PlayerCameraRig getCameraRig() {
        return cameraRig;
    }

    public AssetManager getAssetManager() {
        return kernelInterface.getAssetManager();
    }

    public AppStateManager getStateManager() {
        return kernelInterface.appStateManager();
    }

    public InputManager getInputManager() {
        return kernelInterface.getInputManager();
    }

    public Node getRootNode() {
        return kernelInterface.getRootNode();
    }

    public Node getGuiNode() {
        return kernelInterface.getGuiNode();
    }

    public Map getConfig() {
        return kernelInterface.getConfig();
    }

    public PlayerOptions getPlayerOptions() {
        return playerOptions;
    }

    public PlayerModel getPlayerModel() {
        return playerModel;
    }

    public PlayerSoundProvider getPlayerSoundProvider() {
        return playerSoundProvider;
    }

    public void setPlayerHealth(int health){
        this.playerOptions.setInitialHealth(health);
    }
}