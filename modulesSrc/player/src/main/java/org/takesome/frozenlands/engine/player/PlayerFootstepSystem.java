package org.takesome.frozenlands.engine.player;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.player.input.PlayerState;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerFootstepSystem extends AbstractControl {
    private final Player player;
    private final PlayerRuntimeSettings settings;
    private BetterCharacterControl character;
    private float timer;
    private int stepIndex;
    private boolean wasMoving;

    public PlayerFootstepSystem(Player player) {
        this.player = player;
        this.settings = player.getRuntimeSettings();
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!settings.footstepsEnabled()) {
            return;
        }
        initializeCharacter();
        PlayerLocomotionState locomotion = player.getLocomotionState();
        if (!shouldStep(locomotion)) {
            timer = 0f;
            wasMoving = false;
            return;
        }

        float interval = interval(locomotion);
        if (!wasMoving) {
            timer = interval;
            wasMoving = true;
        } else {
            timer += tpf;
        }

        if (timer >= interval) {
            timer = 0f;
            emitStep(locomotion, interval);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private void initializeCharacter() {
        if (character == null && spatial != null) {
            character = spatial.getControl(BetterCharacterControl.class);
        }
    }

    private boolean shouldStep(PlayerLocomotionState locomotion) {
        if (character == null || !locomotion.onGround()) {
            return false;
        }
        if (locomotion.horizontalSpeed() < settings.footstepMinSpeed()) {
            return false;
        }
        return locomotion.state() == PlayerState.WALKING || locomotion.state() == PlayerState.SPRINTING;
    }

    private float interval(PlayerLocomotionState locomotion) {
        return locomotion.state() == PlayerState.SPRINTING
                ? settings.footstepRunInterval()
                : settings.footstepWalkInterval();
    }

    private void emitStep(PlayerLocomotionState locomotion, float interval) {
        stepIndex++;
        String gait = locomotion.state() == PlayerState.SPRINTING ? "sprinting" : "walking";
        boolean soundRequested = settings.footstepSoundEnabled();
        if (!settings.footstepEventsEnabled()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerRef", player.runtimeId());
        payload.put("stepIndex", stepIndex);
        payload.put("gait", gait);
        payload.put("state", locomotion.state().name());
        payload.put("horizontalSpeed", locomotion.horizontalSpeed());
        payload.put("currentSpeed", locomotion.currentSpeed());
        payload.put("interval", interval);
        payload.put("soundEvent", gait);
        payload.put("soundRequested", soundRequested);
        player.publishEvent(EngineEventTopics.PLAYER_FOOTSTEP, payload);
    }
}
