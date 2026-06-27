package org.takesome.frozenlands.engine.player;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerHeadTurnController extends AbstractControl implements AutoCloseable {
    private static final float SPRING_STIFFNESS = 78f;
    private static final float SPRING_DAMPING = 13f;
    private static final float INPUT_IMPULSE = 1.8f;
    private static final float SNAP_EPSILON = 0.0003f;
    private static final float ACTIVE_EPSILON = 0.001f;

    private final Player player;
    private final PlayerRuntimeSettings settings;
    private final int playerRef;
    private final Quaternion baseChestRotation = new Quaternion();
    private final Quaternion baseHeadRotation = new Quaternion();
    private final Quaternion baseNeckRotation = new Quaternion();
    private final Quaternion chestOffset = new Quaternion();
    private final Quaternion headOffset = new Quaternion();
    private final Quaternion neckOffset = new Quaternion();

    private AutoCloseable subscription;
    private Spatial chest;
    private Spatial head;
    private Spatial neck;
    private boolean ready;
    private float targetYaw;
    private float targetPitch;
    private float currentYaw;
    private float currentPitch;
    private float yawVelocity;
    private float pitchVelocity;

    public PlayerHeadTurnController(Player player) {
        this.player = player;
        this.settings = player.getRuntimeSettings();
        this.playerRef = player.runtimeId();
    }

    @Override
    protected void controlUpdate(float tpf) {
        initializeIfRequired();
        if (!ready || head == null || neck == null) {
            return;
        }
        updateSpring(Math.max(0f, tpf));
        if (hasActiveLookResponse()) {
            applyHeadTurn();
        }
    }

    private void initializeIfRequired() {
        if (ready) {
            return;
        }
        chest = findNamedSpatial(spatial, settings.skeletonChestNode());
        head = findNamedSpatial(spatial, settings.skeletonHeadNode());
        neck = findNamedSpatial(spatial, settings.skeletonNeckNode());
        if (head == null || neck == null) {
            player.getLogger().warn("Player head turn controller cannot find head/neck nodes expectedChest={} expectedHead={} expectedNeck={} chest={} head={} neck={}",
                    settings.skeletonChestNode(), settings.skeletonHeadNode(), settings.skeletonNeckNode(), chest, head, neck);
            ready = true;
            subscribe();
            return;
        }
        if (chest != null) {
            baseChestRotation.set(chest.getLocalRotation());
        }
        baseHeadRotation.set(head.getLocalRotation());
        baseNeckRotation.set(neck.getLocalRotation());
        ready = true;
        subscribe();
        player.getLogger().info(
                "Player head turn controller ready chest={} head={} neck={} response=spring",
                chest == null ? "<none>" : chest.getName(),
                head.getName(),
                neck.getName()
        );
    }

    private void subscribe() {
        if (subscription != null) {
            return;
        }
        subscription = player.subscribeEvent(EngineEventTopics.PLAYER_HEAD_TURN_REQUESTED, this::onHeadTurnRequested, true);
    }

    private void onHeadTurnRequested(Map<String, Object> event) {
        if (!isOwnEvent(event)) {
            return;
        }
        Map<String, Object> payload = payload(event);
        float nextYaw = number(payload, "yaw", targetYaw);
        float nextPitch = number(payload, "pitch", targetPitch);
        float delta = number(payload, "delta", 0f);
        String binding = string(payload, "binding", "");
        targetYaw = nextYaw;
        targetPitch = nextPitch;
        applyInputImpulse(binding, delta);
        publishChanged("event");
    }

    private void applyInputImpulse(String binding, float delta) {
        if (binding == null) {
            return;
        }
        if (binding.startsWith("Rotate_Left") || binding.startsWith("Rotate_Right")) {
            yawVelocity += delta * INPUT_IMPULSE;
        } else if (binding.startsWith("Rotate_Up") || binding.startsWith("Rotate_Down")) {
            pitchVelocity += delta * INPUT_IMPULSE;
        }
    }

    private void updateSpring(float tpf) {
        float yawError = targetYaw - currentYaw;
        float pitchError = targetPitch - currentPitch;
        yawVelocity += yawError * SPRING_STIFFNESS * tpf;
        pitchVelocity += pitchError * SPRING_STIFFNESS * tpf;
        float damping = FastMath.exp(-SPRING_DAMPING * tpf);
        yawVelocity *= damping;
        pitchVelocity *= damping;
        currentYaw += yawVelocity * tpf;
        currentPitch += pitchVelocity * tpf;

        if (Math.abs(yawError) < SNAP_EPSILON && Math.abs(yawVelocity) < SNAP_EPSILON) {
            currentYaw = targetYaw;
            yawVelocity = 0f;
        }
        if (Math.abs(pitchError) < SNAP_EPSILON && Math.abs(pitchVelocity) < SNAP_EPSILON) {
            currentPitch = targetPitch;
            pitchVelocity = 0f;
        }
    }

    private boolean hasActiveLookResponse() {
        return Math.abs(targetYaw) > ACTIVE_EPSILON
                || Math.abs(targetPitch) > ACTIVE_EPSILON
                || Math.abs(currentYaw) > ACTIVE_EPSILON
                || Math.abs(currentPitch) > ACTIVE_EPSILON
                || Math.abs(yawVelocity) > ACTIVE_EPSILON
                || Math.abs(pitchVelocity) > ACTIVE_EPSILON;
    }

    private void applyHeadTurn() {
        // Event-driven additive look response. Body/BetterCharacterControl view direction is intentionally untouched.
        // Do not touch chest here: chest/spine are owned by locomotion clips, otherwise walk/run lose their upper-body motion.
        neckOffset.fromAngles(currentPitch * 0.36f, currentYaw * 0.38f, -currentYaw * 0.018f);
        headOffset.fromAngles(currentPitch * 0.64f, currentYaw * 0.62f, -currentYaw * 0.012f);
        neck.setLocalRotation(baseNeckRotation.mult(neckOffset));
        head.setLocalRotation(baseHeadRotation.mult(headOffset));
    }

    private void publishChanged(String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerRef", playerRef);
        payload.put("yaw", targetYaw);
        payload.put("pitch", targetPitch);
        payload.put("yawDegrees", targetYaw * FastMath.RAD_TO_DEG);
        payload.put("pitchDegrees", targetPitch * FastMath.RAD_TO_DEG);
        payload.put("currentYaw", currentYaw);
        payload.put("currentPitch", currentPitch);
        payload.put("currentYawDegrees", currentYaw * FastMath.RAD_TO_DEG);
        payload.put("currentPitchDegrees", currentPitch * FastMath.RAD_TO_DEG);
        payload.put("reason", reason);
        payload.put("source", "PlayerHeadTurnController");
        player.publishLiveEvent(EngineEventTopics.PLAYER_HEAD_TURN_CHANGED, payload);
    }

    private Spatial findNamedSpatial(Spatial source, String name) {
        if (source == null || name == null || name.isBlank()) {
            return null;
        }
        if (name.equals(source.getName())) {
            return source;
        }
        if (source instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                Spatial result = findNamedSpatial(child, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private boolean isOwnEvent(Map<String, Object> event) {
        return longNumber(payload(event), "playerRef", -1L) == playerRef;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private float number(Map<String, Object> payload, String key, float fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    private long longNumber(Map<String, Object> payload, String key, long fallback) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? fallback : Long.parseLong(String.valueOf(value));
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    @Override
    public void close() {
        if (subscription == null) {
            return;
        }
        try {
            subscription.close();
        } catch (Exception ignored) {
        }
        subscription = null;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            close();
            ready = false;
            chest = null;
            head = null;
            neck = null;
            targetYaw = targetPitch = currentYaw = currentPitch = yawVelocity = pitchVelocity = 0f;
        }
        super.setSpatial(spatial);
    }
}
