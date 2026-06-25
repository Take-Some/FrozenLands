package org.takesome.frozenlands.engine.player.camera;

import com.jme3.renderer.Camera;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.player.Player;

import java.util.Map;

public final class PlayerCameraRig {
    private final Player owner;
    private final Camera camera;
    private final CameraFollowSpatial followControl;
    private final EngineContext context;
    private final int playerRef;
    private AutoCloseable viewToggleSubscription;
    private AutoCloseable viewRequestSubscription;
    private CameraViewMode viewMode;

    public PlayerCameraRig(Player owner, Camera camera, EngineContext context) {
        this.owner = owner;
        this.camera = camera;
        this.context = context;
        this.playerRef = owner.runtimeId();
        this.viewMode = owner.getPlayerOptions().getDefaultCameraView();
        this.followControl = new CameraFollowSpatial(camera, context, owner.getPlayerOptions(), owner.getPlayerModel().getCollisionProfile(), playerRef);
        owner.addControl(followControl);
        subscribeViewEvents();
        applyViewMode(viewMode, "initial");
    }

    public void toggleView() {
        applyViewMode(viewMode.next(), "toggle");
    }

    public void applyViewMode(CameraViewMode nextMode) {
        applyViewMode(nextMode, "direct");
    }

    public void applyViewMode(CameraViewMode nextMode, String reason) {
        CameraViewMode previous = viewMode;
        viewMode = nextMode == null ? CameraViewMode.FIRST_PERSON : nextMode;
        followControl.setViewMode(viewMode);
        boolean visualVisible = viewMode.showsPlayerVisual();
        owner.getPlayerModel().setVisualVisible(visualVisible, owner.getPlayerOptions().getCullHint());
        context.getLogger().info(
                "Player camera view changed: {} -> {} visualVisible={} reason={} transitionSeconds={}",
                previous,
                viewMode,
                visualVisible,
                reason,
                owner.getPlayerOptions().getCameraTransitionSeconds()
        );
        context.getModuleRegistry().publishEvent(EngineEventTopics.PLAYER_CAMERA_VIEW_CHANGED, Map.of(
                "playerRef", playerRef,
                "from", previous.name(),
                "to", viewMode.name(),
                "view", viewMode.name(),
                "reason", reason == null ? "" : reason,
                "visualVisible", visualVisible,
                "transitionSeconds", owner.getPlayerOptions().getCameraTransitionSeconds()
        ));
    }

    public Camera camera() {
        return camera;
    }

    public CameraFollowSpatial followControl() {
        return followControl;
    }

    public CameraViewMode viewMode() {
        return viewMode;
    }

    public void close() {
        close(viewToggleSubscription);
        close(viewRequestSubscription);
        viewToggleSubscription = null;
        viewRequestSubscription = null;
    }

    private void subscribeViewEvents() {
        viewToggleSubscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.PLAYER_CAMERA_VIEW_TOGGLE_REQUESTED,
                this::onViewToggleRequested,
                false
        );
        viewRequestSubscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.PLAYER_CAMERA_VIEW_REQUESTED,
                this::onViewRequested,
                false
        );
    }

    private void onViewToggleRequested(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        if (payload instanceof Map<?, ?> map && isOwnPlayer(map)) {
            toggleView();
        }
    }

    private void onViewRequested(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        if (!(payload instanceof Map<?, ?> map) || !isOwnPlayer(map)) {
            return;
        }
        CameraViewMode requested = CameraViewMode.parse(String.valueOf(map.get("view")), viewMode);
        applyViewMode(requested, "event-request");
    }

    private boolean isOwnPlayer(Map<?, ?> map) {
        Object ref = map.get("playerRef");
        return ref == null || playerRef == longValue(ref, -1L);
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

    private long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return fallback;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
