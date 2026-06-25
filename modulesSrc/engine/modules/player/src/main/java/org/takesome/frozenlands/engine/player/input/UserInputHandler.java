package org.takesome.frozenlands.engine.player.input;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.ui.PlayerHud;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class UserInputHandler extends UserInputAbstract {

    private Player playerInterface;
    private BetterCharacterControl characterControl;
    private PlayerHud playerHud;
    private final Runnable attackCallback;
    private boolean[] directions = new boolean[4];
    final Vector3f tmpV3 = new Vector3f();
    float[] angles = {0, 0, 0};

    public UserInputHandler(Player player, Runnable attackCallback) {
        this.playerInterface = player;
        this.attackCallback = attackCallback;
        setUserInputConfig((HashMap<String, List<Object>>) player.getConfig().get("userInput"));
        this.playerHud = new PlayerHud(player);
    }

    @Override
    public void init() {
        if (!isInit()) {
            characterControl = spatial.getControl(BetterCharacterControl.class);
            if (characterControl == null) {
                playerInterface.getLogger().error(getClass() + " can be attached only to a spatial that has a BetterCharacterControl");
                return;
            }
            playerHud.initialize();
            playerInterface.getInputManager().setCursorVisible(false);
            inputInit(UserInputHelper.getInputMaps(getUserInputConfig()));
            setInit(true);
        }
    }

    @Override
    protected void inputInit(Stack<String> inputMaps) {
        InputManager inputManager = playerInterface.getInputManager();
        inputMaps.forEach(inputMap -> getUserInputConfig().get(inputMap).forEach(inputLine -> {
            InputType inputType = InputType.valueOf(inputMap.toUpperCase());
            int inputKey = (Integer) ((HashMap<?, ?>) inputLine).get("inputKey");
            String inputName = (String) ((HashMap<?, ?>) inputLine).get("inputName");

            switch (inputType) {
                case KEYBOARD -> inputManager.addMapping(inputName, new KeyTrigger(inputKey));
                case MOUSEAXIS -> {
                    boolean negative = (Boolean) ((HashMap<?, ?>) inputLine).get("negative");
                    inputManager.addMapping(inputName, new MouseAxisTrigger(inputKey, negative));
                }
                case MOUSEBUTTONS -> inputManager.addMapping(inputName, new MouseButtonTrigger(inputKey));
            }

            inputManager.addListener(this, inputName);
        }));
    }

    @Override
    protected void controlUpdate(float tpf) {
        Vector3f walkDirection = new Vector3f(directions[0] ? 1 : directions[1] ? -1 : 0, 0, directions[2] ? 1 : directions[3] ? -1 : 0);
        movePlayer(walkDirection, 1.0f, tpf);
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        float rotationMultiplier = getPlayerState().equals(PlayerState.SPRINTING) ? this.getRotationMultiplierWalking() : this.getRotationMultiplierRunning();
        value *= this.getCurrentSpeed() * rotationMultiplier;

        switch (name) {
            case "Rotate_Left" -> angles[1] += value;
            case "Rotate_Right" -> angles[1] -= value;
            case "Rotate_Up" -> angles[0] -= value;
            case "Rotate_Down" -> angles[0] += value;
        }
        Quaternion tmpRot = new Quaternion();
        angles[0] = FastMath.clamp(angles[0], -0.85f, 1.1f);
        tmpV3.set(Vector3f.UNIT_Z);
        tmpRot.fromAngles(angles);
        tmpRot.multLocal(tmpV3);
        characterControl.setViewDirection(tmpV3);

        Vector3f moveDirection = new Vector3f(directions[0] ? 1 : directions[1] ? -1 : 0, 0, directions[2] ? 1 : directions[3] ? -1 : 0);
        movePlayer(moveDirection, value, tpf);
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Left" -> directions[0] = isPressed;
            case "Right" -> directions[1] = isPressed;
            case "Up" -> directions[2] = isPressed;
            case "Down" -> directions[3] = isPressed;
            case "Attack" -> {
                this.setAttacking(isPressed);
                if (isPressed) {
                    attackCallback.run();
                }
            }
            case "Jump" -> {
                this.setJumping(isPressed);
                if (isPressed) {
                    characterControl.jump();
                    characterControl.setPhysicsDamping(0);
                }
            }
            case "Run" -> this.setRunning(isPressed);
        }
    }

    @Override
    protected void movePlayer(Vector3f direction, float speedMultiplier, float tpf) {
        init();
        Quaternion tmpQtr = new Quaternion();
        float targetSpeed = this.isRunning() ? this.getRunSpeed() : this.getWalkSpeed();
        float speedChange = targetSpeed - this.getCurrentSpeed();
        float actualSpeedChange = Math.signum(speedChange) * Math.min(this.getMaxSmoothSpeedChange() * tpf, Math.abs(speedChange));
        this.setCurrentSpeed(this.getCurrentSpeed() + actualSpeedChange);
        direction.multLocal(this.getCurrentSpeed() * speedMultiplier);

        tmpV3.set(characterControl.getViewDirection());
        tmpV3.y = 0;
        tmpV3.normalizeLocal();
        tmpQtr.lookAt(tmpV3, Vector3f.UNIT_Y);
        tmpQtr.multLocal(direction);

        characterControl.setWalkDirection(direction);
        characterControl.setPhysicsDamping(0.9f);
        setPlayerState(direction, tpf);

        this.playerHud.updateLabelTexts(
                new String[]{
                        "posX",
                        "posY",
                        "posZ"
                },
                new String[]{
                        String.valueOf(this.getPlayerPosition().x),
                        String.valueOf(this.getPlayerPosition().y),
                        String.valueOf(this.getPlayerPosition().z),
                });
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Only for rendering
    }


    protected void setPlayerState(Vector3f walkDirection, float tpf) {
        if (walkDirection.lengthSquared() == 0 && this.getCurrentSpeed() == this.getWalkSpeed()) {
            setPlayerState(PlayerState.STANDING);
        } else {
            if (walkDirection.lengthSquared() > 0) {
                if (!characterControl.isOnGround() && Math.abs(this.getPlayerDistanceAboveGround(spatial)) <= 1) {
                    setPlayerState(PlayerState.FLYING);
                } else {
                    if (this.getCurrentSpeed() > this.getWalkSpeed()) {
                        setPlayerState(PlayerState.SPRINTING);
                    } else {
                        setPlayerState(PlayerState.WALKING);
                    }
                }
            }
        }
    }


    public Vector3f getPlayerPosition() {
        if (spatial != null) {
            Vector3f worldTranslation = spatial.getWorldTranslation();
            float x = Math.round(worldTranslation.x);
            float y = Math.round(worldTranslation.y);
            float z = Math.round(worldTranslation.z);
            return new Vector3f(x, y, z);
        } else {
            return new Vector3f(0, 0, 0);
        }
    }
}
