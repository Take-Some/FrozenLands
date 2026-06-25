package org.takesome.frozenlands.engine.player.input;

import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public abstract class UserInputAbstract extends AbstractControl implements ActionListener, AnalogListener {

    private boolean isInit = false;
    private boolean isJumping = false;
    private boolean isAttacking = false;
    private boolean isRunning = false;
    private float currentSpeed = 0.0f;
    private float walkSpeed = 4.0f;
    private float runSpeed = 8.0f;
    private float maxSmoothSpeedChange = 2.0f;
    private float rotationMultiplierWalking = 0.04f;
    private float rotationMultiplierRunning = 0.1f;
    private PlayerState playerState = PlayerState.STANDING;

    private HashMap<String, List<Object>> userInputConfig;

    protected abstract void init();

    protected abstract void movePlayer(Vector3f direction, float speedMultiplier, float tpf);

    @Override
    protected abstract void controlUpdate(float v);

    @Override
    protected abstract void controlRender(RenderManager renderManager, ViewPort viewPort);

    @Override
    public abstract void onAction(String s, boolean b, float v);

    @Override
    public abstract void onAnalog(String s, float v, float v1);

    protected abstract void inputInit(Stack<String> inputMaps);

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }

    public HashMap<String, List<Object>> getUserInputConfig() {
        return userInputConfig;
    }

    public void setUserInputConfig(HashMap<String, List<Object>> userInputConfig) {
        this.userInputConfig = userInputConfig;
    }

    public boolean isJumping() {
        return isJumping;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public void setJumping(boolean jumping) {
        isJumping = jumping;
    }

    public void setAttacking(boolean attacking) {
        isAttacking = attacking;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public float getRunSpeed() {
        return runSpeed;
    }

    public float getMaxSmoothSpeedChange() {
        return maxSmoothSpeedChange;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public float getPlayerDistanceAboveGround(Spatial spatial) {
        Vector3f characterPosition = spatial.getWorldTranslation();
        float characterHeight = 2.0f;
        return characterPosition.y - characterHeight * 0.5f;
    }

    public float getRotationMultiplierWalking() {
        return rotationMultiplierWalking;
    }

    public float getRotationMultiplierRunning() {
        return rotationMultiplierRunning;
    }
}
