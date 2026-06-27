package org.takesome.frozenlands.engine.player;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.events.EngineEventPayload;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.events.EventSubscriptionBag;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerToolController extends AbstractControl {
    private final Player player;
    private final PlayerRuntimeSettings settings;
    private final PlayerToolMount toolMount;
    private final PlayerGrindableInteractor interactor;
    private final EventSubscriptionBag subscriptions = new EventSubscriptionBag();
    private boolean equippedPublished;
    private int swingIndex;

    public PlayerToolController(Player player) {
        this.player = player;
        this.settings = player.getRuntimeSettings();
        this.toolMount = new PlayerToolMount(player, settings);
        this.interactor = new PlayerGrindableInteractor(player, settings);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial == null) {
            subscriptions.close();
            toolMount.detach();
            equippedPublished = false;
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        ensureInitialized();
        toolMount.update(tpf);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("tool", settings.toolName());
        status.put("model", settings.toolModel());
        status.put("range", settings.toolRange());
        status.put("damage", settings.toolDamage());
        status.put("swingIndex", swingIndex);
        status.putAll(toolMount.status());
        status.put("interaction", interactor.status());
        return status;
    }

    private void ensureInitialized() {
        toolMount.ensureLoaded();
        publishEquippedOnce();
        if (subscriptions.isEmpty()) {
            subscriptions.add(player.subscribeEvent(EngineEventTopics.PLAYER_ATTACK_REQUESTED, this::handleAttackEvent));
        }
    }

    private void publishEquippedOnce() {
        if (equippedPublished) {
            return;
        }
        equippedPublished = true;
        player.publishEvent(EngineEventTopics.PLAYER_TOOL_EQUIPPED, Map.of(
                "playerRef", player.runtimeId(),
                "tool", settings.toolName(),
                "model", settings.toolModel(),
                "mountMode", toolMount.mountMode(),
                "targetType", settings.interactionTargetType(),
                "handJoint", settings.toolHandJoint()
        ));
    }

    private void handleAttackEvent(Map<String, Object> event) {
        Map<String, Object> payload = EngineEventPayload.of(event);
        if (EngineEventPayload.integer(payload, "playerRef", -1L) != player.runtimeId()) {
            return;
        }
        swingIndex++;
        toolMount.triggerSwing();
        player.publishEvent(EngineEventTopics.PLAYER_TOOL_SWING, Map.of(
                "playerRef", player.runtimeId(),
                "tool", settings.toolName(),
                "swingIndex", swingIndex,
                "mountMode", toolMount.mountMode(),
                "targetType", settings.interactionTargetType()
        ));
        interactor.interactWithCameraTarget();
    }
}
