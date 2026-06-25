package org.takesome.frozenlands.engine.player;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.player.input.PlayerState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PlayerAnimationController extends AbstractControl implements AutoCloseable {
    private static final float MOMENTARY_FALLBACK_SECONDS = 0.85f;
    private static final float MOMENTARY_RESTORE_PAD_SECONDS = 0.04f;

    private final Player player;
    private final int playerRef;
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    private AnimComposer composer;
    private AnimControl legacyControl;
    private AnimChannel legacyChannel;
    private Set<String> availableClips = Set.of();
    private boolean ready;
    private boolean announcedRuntime;
    private String currentState = PlayerState.STANDING.name();
    private String currentClip;
    private String momentaryClip;
    private float momentaryRemainingSeconds;

    public PlayerAnimationController(Player player) {
        this.player = player;
        this.playerRef = player.runtimeId();
    }

    @Override
    protected void controlUpdate(float tpf) {
        initializeIfRequired();
        updateMomentaryAnimation(tpf);
    }

    private void initializeIfRequired() {
        if (ready) {
            return;
        }
        ready = true;
        Spatial visual = player.getPlayerModel().getPlayerSpatial();
        composer = findComposer(visual);
        legacyControl = findLegacyControl(visual);
        if (legacyControl != null) {
            legacyChannel = legacyControl.createChannel();
        }
        availableClips = collectAvailableClips();
        announceRuntime();
        subscribeEvents();
        applyState(PlayerState.STANDING.name(), "initial");
    }

    private void subscribeEvents() {
        if (!subscriptions.isEmpty()) {
            return;
        }
        subscriptions.add(player.subscribeEvent(EngineEventTopics.PLAYER_STATE_CHANGED, this::onPlayerStateChanged, true));
        subscriptions.add(player.subscribeEvent(EngineEventTopics.PLAYER_RUN_CHANGED, this::onRunChanged, true));
        subscriptions.add(player.subscribeEvent(EngineEventTopics.PLAYER_JUMP_REQUESTED, event -> applyMomentary("jump", event), false));
        subscriptions.add(player.subscribeEvent(EngineEventTopics.PLAYER_ATTACK_REQUESTED, event -> applyMomentary("attack", event), false));
    }

    private void onPlayerStateChanged(Map<String, Object> event) {
        if (!isOwnEvent(event)) {
            return;
        }
        Map<String, Object> payload = payload(event);
        currentState = string(payload, "to", PlayerState.STANDING.name());
        if (isMomentaryActive()) {
            return;
        }
        applyState(currentState, "state-event");
    }

    private void onRunChanged(Map<String, Object> event) {
        if (!isOwnEvent(event) || isMomentaryActive()) {
            return;
        }
        Map<String, Object> payload = payload(event);
        boolean running = bool(payload, "running");
        if (running && PlayerState.WALKING.name().equals(currentState)) {
            applyState(PlayerState.SPRINTING.name(), "run-event");
        } else if (!running && PlayerState.SPRINTING.name().equals(currentState)) {
            applyState(PlayerState.WALKING.name(), "run-event");
        }
    }

    private void applyMomentary(String action, Map<String, Object> event) {
        if (!isOwnEvent(event)) {
            return;
        }
        boolean played = playBestClip(action, aliasesFor(action), "event:" + action, false, true);
        if (!played || currentClip == null) {
            return;
        }
        momentaryClip = currentClip;
        momentaryRemainingSeconds = clipLengthSeconds(currentClip, MOMENTARY_FALLBACK_SECONDS) + MOMENTARY_RESTORE_PAD_SECONDS;
    }

    private void applyState(String state, String reason) {
        currentState = state == null || state.isBlank() ? PlayerState.STANDING.name() : state;
        playBestClip(currentState, aliasesFor(currentState), reason, true, false);
    }

    private void updateMomentaryAnimation(float tpf) {
        if (!isMomentaryActive()) {
            return;
        }
        momentaryRemainingSeconds -= Math.max(0f, tpf);
        if (momentaryRemainingSeconds > 0f) {
            return;
        }
        String ended = momentaryClip;
        momentaryClip = null;
        momentaryRemainingSeconds = 0f;
        applyState(currentState, "momentary-end:" + ended);
    }

    private boolean isMomentaryActive() {
        return momentaryClip != null && momentaryRemainingSeconds > 0f;
    }

    private boolean playBestClip(String state, List<String> aliases, String reason, boolean loop, boolean forceRestart) {
        if (availableClips.isEmpty()) {
            return false;
        }
        String clip = resolveClip(aliases);
        if (clip == null) {
            clip = availableClips.iterator().next();
        }
        if (clip.equals(currentClip) && !forceRestart) {
            return true;
        }
        if (composer != null) {
            composer.setCurrentAction(clip);
            composer.setTime(0.0d);
        } else if (legacyChannel != null) {
            legacyChannel.setAnim(clip);
            legacyChannel.setLoopMode(loop ? LoopMode.Loop : LoopMode.DontLoop);
            legacyChannel.setSpeed(1f);
        } else {
            return false;
        }
        String previous = currentClip;
        currentClip = clip;
        player.getLogger().info(
                "Player animation changed: {} -> {} state={} reason={} loop={} clips={}",
                previous,
                currentClip,
                state,
                reason,
                loop,
                availableClips
        );
        player.publishEvent(EngineEventTopics.PLAYER_ANIMATION_CHANGED, Map.of(
                "playerRef", playerRef,
                "from", previous == null ? "" : previous,
                "to", currentClip,
                "state", state,
                "reason", reason,
                "loop", loop
        ));
        return true;
    }

    private float clipLengthSeconds(String clip, float fallback) {
        if (clip == null) {
            return fallback;
        }
        if (composer != null) {
            AnimClip animClip = composer.getAnimClip(clip);
            if (animClip != null && animClip.getLength() > 0.0d) {
                return (float) animClip.getLength();
            }
        }
        if (legacyControl != null) {
            float length = legacyControl.getAnimationLength(clip);
            if (length > 0f) {
                return length;
            }
        }
        return fallback;
    }

    private String resolveClip(List<String> aliases) {
        for (String alias : aliases) {
            for (String clip : availableClips) {
                if (clip.equals(alias) || clip.equalsIgnoreCase(alias)) {
                    return clip;
                }
            }
        }
        for (String alias : aliases) {
            String normalizedAlias = normalize(alias);
            for (String clip : availableClips) {
                if (normalize(clip).contains(normalizedAlias)) {
                    return clip;
                }
            }
        }
        return null;
    }

    private List<String> aliasesFor(String stateOrAction) {
        String key = stateOrAction == null ? "" : stateOrAction.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "STANDING", "IDLE" -> List.of("idle", "stand", "standing", "breathing", "A-pose", "TPose", "T-pose");
            case "WALKING", "WALK" -> List.of("walk", "walking", "walk_forward", "idle");
            case "SPRINTING", "RUN", "RUNNING" -> List.of("run", "running", "sprint", "sprinting", "walk");
            case "FLYING", "AIR", "FALL", "FALLING" -> List.of("air", "flying", "fall", "falling", "jump", "idle");
            case "JUMP" -> List.of("jump", "air");
            case "ATTACK", "ATTACKING" -> List.of("attack", "shoot", "fire", "punch", "idle");
            default -> List.of(key, "idle");
        };
    }

    private Set<String> collectAvailableClips() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (composer != null) {
            Collection<String> clips = composer.getAnimClipsNames();
            if (clips != null) {
                clips.stream().filter(this::isGameplayClip).forEach(names::add);
            }
        }
        if (legacyControl != null) {
            legacyControl.getAnimationNames().stream().filter(this::isGameplayClip).forEach(names::add);
        }
        return Set.copyOf(names);
    }

    private boolean isGameplayClip(String clip) {
        String normalized = normalize(clip);
        return !normalized.startsWith("headturn");
    }

    private void announceRuntime() {
        if (announcedRuntime) {
            return;
        }
        announcedRuntime = true;
        if (composer == null && legacyControl == null) {
            player.getLogger().warn("Player animation runtime not found for visual={}", player.getPlayerModel().getPlayerSpatial().getName());
        } else {
            player.getLogger().info("Player animation runtime ready composer={} legacy={} clips={}", composer != null, legacyControl != null, availableClips);
        }
    }

    private AnimComposer findComposer(Spatial spatial) {
        if (spatial == null) {
            return null;
        }
        AnimComposer control = spatial.getControl(AnimComposer.class);
        if (control != null) {
            return control;
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                AnimComposer found = findComposer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private AnimControl findLegacyControl(Spatial spatial) {
        if (spatial == null) {
            return null;
        }
        AnimControl control = spatial.getControl(AnimControl.class);
        if (control != null) {
            return control;
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                AnimControl found = findLegacyControl(child);
                if (found != null) {
                    return found;
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

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean bool(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean bool ? bool : value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private long longNumber(Map<String, Object> payload, String key, long fallback) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? fallback : Long.parseLong(String.valueOf(value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    @Override
    public void close() {
        for (AutoCloseable subscription : subscriptions) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
        }
        subscriptions.clear();
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            close();
            ready = false;
            composer = null;
            legacyControl = null;
            legacyChannel = null;
            availableClips = Set.of();
            currentClip = null;
            momentaryClip = null;
            momentaryRemainingSeconds = 0f;
        }
        super.setSpatial(spatial);
    }
}
