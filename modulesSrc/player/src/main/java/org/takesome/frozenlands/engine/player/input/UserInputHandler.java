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
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.ui.PlayerHud;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class UserInputHandler extends UserInputAbstract {
    private static final String LEFT = "Left";
    private static final String RIGHT = "Right";
    private static final String UP = "Up";
    private static final String DOWN = "Down";
    private static final String ATTACK = "Attack";
    private static final String JUMP = "Jump";
    private static final String RUN = "Run";

    private final Player playerInterface;
    private final Runnable attackCallback;
    private final int playerRef;
    private final boolean[] directions = new boolean[4];
    private final Vector3f tmpV3 = new Vector3f();
    private final float[] angles = {0, 0, 0};

    private BetterCharacterControl characterControl;
    private PlayerHud playerHud;
    private AutoCloseable actionSubscription;
    private AutoCloseable analogSubscription;
    private AutoCloseable moveIntentSubscription;
    private AutoCloseable cameraLookInputSubscription;
    private AutoCloseable consoleVisibilitySubscription;
    private boolean cameraLookInputEnabled = true;

    public UserInputHandler(Player player, Runnable attackCallback) {
        this.playerInterface = player;
        this.attackCallback = attackCallback;
        this.playerRef = player.runtimeId();
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
            subscribeInputEvents();
            inputInit(UserInputHelper.getInputMaps(getUserInputConfig()));
            ensureCameraLookMappings();
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
            publish(EngineEventTopics.PLAYER_INPUT_MAPPING_REGISTERED, Map.of(
                    "playerRef", playerRef,
                    "binding", inputName,
                    "type", inputType.name(),
                    "inputKey", inputKey
            ));
        }));
    }

    @Override
    protected void controlUpdate(float tpf) {
        init();
        if (!isInit()) {
            return;
        }
        publishMoveIntent(new Vector3f(directions[0] ? 1 : directions[1] ? -1 : 0, 0, directions[2] ? 1 : directions[3] ? -1 : 0), 1.0f, tpf);
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!cameraLookInputEnabled && name.startsWith("Rotate_")) {
            return;
        }
        publishLive(EngineEventTopics.PLAYER_INPUT_ANALOG, Map.of(
                "playerRef", playerRef,
                "binding", name,
                "value", value,
                "tpf", tpf
        ));
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        publish(EngineEventTopics.PLAYER_INPUT_ACTION, Map.of(
                "playerRef", playerRef,
                "binding", binding,
                "pressed", isPressed,
                "tpf", tpf
        ));
    }

    @Override
    protected void movePlayer(Vector3f direction, float speedMultiplier, float tpf) {
        init();
        Quaternion tmpQtr = new Quaternion();
        float targetSpeed = isRunning() ? getRunSpeed() : getWalkSpeed();
        float speedChange = targetSpeed - getCurrentSpeed();
        float actualSpeedChange = Math.signum(speedChange) * Math.min(getMaxSmoothSpeedChange() * tpf, Math.abs(speedChange));
        setCurrentSpeed(getCurrentSpeed() + actualSpeedChange);
        direction.multLocal(getCurrentSpeed() * speedMultiplier);

        tmpV3.set(characterControl.getViewDirection());
        tmpV3.y = 0;
        tmpV3.normalizeLocal();
        tmpQtr.lookAt(tmpV3, Vector3f.UNIT_Y);
        tmpQtr.multLocal(direction);

        characterControl.setWalkDirection(direction);
        characterControl.setPhysicsDamping(0.9f);
        updatePlayerState(direction, tpf);
        updateHudPosition();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Only for rendering
    }

    @Override
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial == null) {
            closeSubscriptions();
        }
    }

    public Vector3f getPlayerPosition() {
        if (spatial != null) {
            Vector3f worldTranslation = spatial.getWorldTranslation();
            return new Vector3f(Math.round(worldTranslation.x), Math.round(worldTranslation.y), Math.round(worldTranslation.z));
        }
        return new Vector3f(0, 0, 0);
    }

    private void subscribeInputEvents() {
        if (actionSubscription != null) {
            return;
        }
        actionSubscription = playerInterface.subscribeEvent(EngineEventTopics.PLAYER_INPUT_ACTION, this::handleActionEvent);
        analogSubscription = playerInterface.subscribeEvent(EngineEventTopics.PLAYER_INPUT_ANALOG, this::handleAnalogEvent);
        moveIntentSubscription = playerInterface.subscribeEvent(EngineEventTopics.PLAYER_MOVE_INTENT, this::handleMoveIntentEvent);
        cameraLookInputSubscription = playerInterface.subscribeEvent(EngineEventTopics.CAMERA_LOOK_INPUT_ENABLED_REQUESTED, this::handleCameraLookInputEnabledEvent, true);
        consoleVisibilitySubscription = playerInterface.subscribeEvent(EngineEventTopics.CONSOLE_VISIBILITY_CHANGED, this::handleConsoleVisibilityEvent, true);
    }

    private void handleCameraLookInputEnabledEvent(Map<String, Object> event) {
        applyCameraLookInputEnabled(bool(payload(event), "enabled"));
    }

    private void handleConsoleVisibilityEvent(Map<String, Object> event) {
        applyCameraLookInputEnabled(!bool(payload(event), "open"));
    }

    private void applyCameraLookInputEnabled(boolean enabled) {
        cameraLookInputEnabled = enabled;
        if (enabled) {
            ensureCameraLookMappings();
        }
    }

    private void ensureCameraLookMappings() {
        InputManager inputManager = playerInterface.getInputManager();
        List<Object> mouseAxis = getUserInputConfig().get("mouseAxis");
        if (mouseAxis == null) {
            return;
        }
        mouseAxis.forEach(inputLine -> {
            String inputName = String.valueOf(((HashMap<?, ?>) inputLine).get("inputName"));
            if (!inputName.startsWith("Rotate_") || inputManager.hasMapping(inputName)) {
                return;
            }
            int inputKey = (Integer) ((HashMap<?, ?>) inputLine).get("inputKey");
            boolean negative = (Boolean) ((HashMap<?, ?>) inputLine).get("negative");
            inputManager.addMapping(inputName, new MouseAxisTrigger(inputKey, negative));
            inputManager.addListener(this, inputName);
        });
    }

    private void handleActionEvent(Map<String, Object> event) {
        if (!isOwnEvent(event)) {
            return;
        }
        Map<String, Object> payload = payload(event);
        String binding = string(payload, "binding", "");
        boolean isPressed = bool(payload, "pressed");
        switch (binding) {
            case LEFT -> directions[0] = isPressed;
            case RIGHT -> directions[1] = isPressed;
            case UP -> directions[2] = isPressed;
            case DOWN -> directions[3] = isPressed;
            case ATTACK -> handleAttack(isPressed);
            case JUMP -> handleJump(isPressed);
            case RUN -> handleRun(isPressed);
            default -> publish("player.input.unhandled", Map.of("playerRef", playerRef, "binding", binding));
        }
    }

    private void handleAnalogEvent(Map<String, Object> event) {
        if (!isOwnEvent(event)) {
            return;
        }
        Map<String, Object> payload = payload(event);
        String name = string(payload, "binding", "");
        float value = number(payload, "value", 0f);
        float tpf = number(payload, "tpf", 0f);
        float rotationMultiplier = isRunning() ? getRotationMultiplierRunning() : getRotationMultiplierWalking();
        value *= rotationMultiplier;

        switch (name) {
            case "Rotate_Left" -> angles[1] += value;
            case "Rotate_Right" -> angles[1] -= value;
            case "Rotate_Up" -> angles[0] -= value;
            case "Rotate_Down" -> angles[0] += value;
            default -> { }
        }
        publishLookIntent(name, value);
        applyLookDirection();
        publishMoveIntent(new Vector3f(directions[0] ? 1 : directions[1] ? -1 : 0, 0, directions[2] ? 1 : directions[3] ? -1 : 0), value, tpf);
    }

    private void handleMoveIntentEvent(Map<String, Object> event) {
        if (!isOwnEvent(event)) {
            return;
        }
        Map<String, Object> payload = payload(event);
        Vector3f direction = new Vector3f(number(payload, "x", 0f), 0f, number(payload, "z", 0f));
        movePlayer(direction, number(payload, "speedMultiplier", 1f), number(payload, "tpf", 0f));
    }

    private void handleAttack(boolean isPressed) {
        setAttacking(isPressed);
        if (isPressed) {
            publish(EngineEventTopics.PLAYER_ATTACK_REQUESTED, Map.of("playerRef", playerRef));
            attackCallback.run();
        }
    }

    private void handleJump(boolean isPressed) {
        setJumping(isPressed);
        if (isPressed) {
            publish(EngineEventTopics.PLAYER_JUMP_REQUESTED, Map.of("playerRef", playerRef));
            characterControl.jump();
            characterControl.setPhysicsDamping(0);
        }
    }

    private void handleRun(boolean isPressed) {
        setRunning(isPressed);
        publish(EngineEventTopics.PLAYER_RUN_CHANGED, Map.of("playerRef", playerRef, "running", isPressed));
    }

    private void applyLookDirection() {
        Quaternion tmpRot = new Quaternion();
        angles[0] = FastMath.clamp(angles[0], -0.85f, 1.1f);
        tmpV3.set(Vector3f.UNIT_Z);
        tmpRot.fromAngles(angles);
        tmpRot.multLocal(tmpV3);
        characterControl.setViewDirection(tmpV3);
    }

    private void publishLookIntent(String binding, float value) {
        publishLive(EngineEventTopics.PLAYER_LOOK_INTENT, Map.of(
                "playerRef", playerRef,
                "binding", binding,
                "value", value,
                "pitch", angles[0],
                "yaw", angles[1]
        ));
    }

    private void publishMoveIntent(Vector3f direction, float speedMultiplier, float tpf) {
        publishLive(EngineEventTopics.PLAYER_MOVE_INTENT, Map.of(
                "playerRef", playerRef,
                "x", direction.x,
                "z", direction.z,
                "speedMultiplier", speedMultiplier,
                "tpf", tpf
        ));
    }

    private void updatePlayerState(Vector3f walkDirection, float tpf) {
        PlayerState next = getPlayerState();
        if (walkDirection.lengthSquared() == 0 && getCurrentSpeed() == getWalkSpeed()) {
            next = PlayerState.STANDING;
        } else if (walkDirection.lengthSquared() > 0) {
            if (!characterControl.isOnGround() && Math.abs(getPlayerDistanceAboveGround(spatial)) <= 1) {
                next = PlayerState.FLYING;
            } else if (getCurrentSpeed() > getWalkSpeed()) {
                next = PlayerState.SPRINTING;
            } else {
                next = PlayerState.WALKING;
            }
        }
        transitionPlayerState(next, tpf);
    }

    private void transitionPlayerState(PlayerState next, float tpf) {
        PlayerState previous = getPlayerState();
        if (previous == next) {
            return;
        }
        setPlayerState(next);
        publish(EngineEventTopics.PLAYER_STATE_CHANGED, Map.of(
                "playerRef", playerRef,
                "from", previous.name(),
                "to", next.name(),
                "tpf", tpf
        ));
    }

    private void updateHudPosition() {
        Vector3f position = getPlayerPosition();
        playerHud.updateLabelTexts(
                new String[]{"posX", "posY", "posZ"},
                new String[]{String.valueOf(position.x), String.valueOf(position.y), String.valueOf(position.z)}
        );
    }

    private boolean isOwnEvent(Map<String, Object> event) {
        return longNumber(payload(event), "playerRef", -1L) == playerRef;
    }

    private Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        if (payload instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean bool(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private float number(Map<String, Object> payload, String key, float fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    private long longNumber(Map<String, Object> payload, String key, long fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.longValue() : value == null ? fallback : Long.parseLong(String.valueOf(value));
    }

    private void publish(String topic, Map<String, Object> payload) {
        playerInterface.publishEvent(topic, payload);
    }

    private void publishLive(String topic, Map<String, Object> payload) {
        playerInterface.publishLiveEvent(topic, payload);
    }

    private void closeSubscriptions() {
        close(actionSubscription);
        close(analogSubscription);
        close(moveIntentSubscription);
        close(cameraLookInputSubscription);
        close(consoleVisibilitySubscription);
        actionSubscription = null;
        analogSubscription = null;
        moveIntentSubscription = null;
        cameraLookInputSubscription = null;
        consoleVisibilitySubscription = null;
    }

    private void close(AutoCloseable subscription) {
        if (subscription == null) {
            return;
        }
        try {
            subscription.close();
        } catch (Exception ignored) {
        }
    }
}
