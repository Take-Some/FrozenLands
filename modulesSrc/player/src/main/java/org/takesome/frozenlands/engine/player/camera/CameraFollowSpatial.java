package org.takesome.frozenlands.engine.player.camera;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.player.PlayerCollisionProfile;
import org.takesome.frozenlands.engine.player.PlayerOptions;

import java.util.LinkedHashMap;
import java.util.Map;

public class CameraFollowSpatial extends AbstractControl {
    private static final float MIN_CAMERA_PITCH = -80f * FastMath.DEG_TO_RAD;
    private static final float MAX_CAMERA_PITCH = 80f * FastMath.DEG_TO_RAD;
    private static final float LOOK_SMOOTHING_SPEED = 32f;

    private final Camera cam;
    private final EngineContext context;
    private final PlayerOptions options;
    private final PlayerCollisionProfile collisionProfile;
    private final int playerRef;
    private final Vector3f tmpOrigin = new Vector3f();
    private final Vector3f tmpDirection = new Vector3f();
    private final Vector3f tmpTarget = new Vector3f();
    private final Vector3f desiredLocation = new Vector3f();
    private final Vector3f desiredTarget = new Vector3f();
    private final Vector3f transitionStartLocation = new Vector3f();
    private final Vector3f transitionLocation = new Vector3f();

    private boolean ready;
    private BetterCharacterControl character;
    private Spatial cameraNode;
    private AutoCloseable cameraPauseSubscription;
    private AutoCloseable lookIntentSubscription;
    private boolean cameraFollowPaused;
    private CameraViewMode viewMode;
    private boolean transitionActive;
    private float transitionTime;
    private float cameraYaw;
    private float cameraPitch;
    private float targetCameraYaw;
    private float targetCameraPitch;

    public CameraFollowSpatial(Camera cam, EngineContext context, PlayerOptions options, PlayerCollisionProfile collisionProfile, int playerRef) {
        this.cam = cam;
        this.context = context;
        this.options = options;
        this.collisionProfile = collisionProfile;
        this.playerRef = playerRef;
        this.viewMode = options.getDefaultCameraView();
    }

    public void setViewMode(CameraViewMode viewMode) {
        CameraViewMode next = viewMode == null ? CameraViewMode.FIRST_PERSON : viewMode;
        if (this.viewMode == next && transitionActive) {
            return;
        }
        this.viewMode = next;
        transitionStartLocation.set(cam.getLocation());
        transitionTime = 0f;
        transitionActive = true;
    }

    public CameraViewMode getViewMode() {
        return viewMode;
    }

    private void initialize() {
        if (ready) {
            return;
        }
        ready = true;
        subscribeCameraPause();
        subscribeLookIntent();
        cameraNode = findNamedSpatial(spatial, "camera");
        character = spatial.getControl(BetterCharacterControl.class);
        if (character == null) {
            spatial.depthFirstTraversal(candidate -> {
                BetterCharacterControl control = candidate.getControl(BetterCharacterControl.class);
                if (control != null) {
                    character = control;
                }
            });
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        initialize();
        if (cameraFollowPaused || character == null) {
            return;
        }

        updateCameraLook(tpf);
        computeDesiredCamera();
        applyDesiredCamera(tpf);
    }

    private void updateCameraLook(float tpf) {
        float alpha = 1f - FastMath.exp(-LOOK_SMOOTHING_SPEED * Math.max(0f, tpf));
        cameraYaw += shortestAngleDelta(cameraYaw, targetCameraYaw) * alpha;
        cameraPitch += (targetCameraPitch - cameraPitch) * alpha;
    }

    private float shortestAngleDelta(float from, float to) {
        float delta = to - from;
        while (delta > FastMath.PI) {
            delta -= FastMath.TWO_PI;
        }
        while (delta < -FastMath.PI) {
            delta += FastMath.TWO_PI;
        }
        return delta;
    }

    private void computeDesiredCamera() {
        switch (viewMode) {
            case FIRST_PERSON -> computeFirstPersonCamera();
            case THIRD_PERSON -> computeThirdPersonCamera();
            case FRONT_PERSON -> computeFrontPersonCamera();
        }
    }

    private void applyDesiredCamera(float tpf) {
        if (!transitionActive) {
            cam.setLocation(desiredLocation);
            cam.lookAt(desiredTarget, Vector3f.UNIT_Y);
            return;
        }

        transitionTime += Math.max(0f, tpf);
        float duration = options.getCameraTransitionSeconds();
        float alpha = Math.min(1f, transitionTime / duration);
        float eased = alpha * alpha * (3f - 2f * alpha);
        transitionLocation.set(transitionStartLocation).interpolateLocal(desiredLocation, eased);
        cam.setLocation(transitionLocation);
        cam.lookAt(desiredTarget, Vector3f.UNIT_Y);
        if (alpha >= 1f) {
            transitionActive = false;
            cam.setLocation(desiredLocation);
            cam.lookAt(desiredTarget, Vector3f.UNIT_Y);
        }
    }

    private void computeFirstPersonCamera() {
        if (cameraNode != null) {
            desiredLocation.set(cameraNode.getWorldTranslation());
        } else {
            desiredLocation.set(spatial.getWorldTranslation());
            desiredLocation.y += eyeHeight();
        }
        lookDirection(true);
        desiredTarget.set(desiredLocation).addLocal(tmpDirection);
    }

    private void computeThirdPersonCamera() {
        baseOriginAndDirection();
        desiredTarget.set(tmpTarget);
        desiredLocation.set(tmpOrigin);
        desiredLocation.y += options.getThirdPersonHeight();
        desiredLocation.subtractLocal(tmpDirection.mult(options.getThirdPersonDistance()));
    }

    private void computeFrontPersonCamera() {
        baseOriginAndDirection();
        desiredTarget.set(tmpTarget);
        desiredLocation.set(tmpOrigin);
        desiredLocation.y += options.getFrontPersonHeight();
        desiredLocation.addLocal(tmpDirection.mult(options.getFrontPersonDistance()));
    }

    private void baseOriginAndDirection() {
        tmpOrigin.set(spatial.getWorldTranslation());
        lookDirection(false);
        tmpTarget.set(tmpOrigin);
        tmpTarget.y += eyeHeight();
    }

    private void lookDirection(boolean includePitch) {
        float pitch = includePitch ? cameraPitch : 0f;
        float cosPitch = FastMath.cos(pitch);
        tmpDirection.set(
                FastMath.sin(cameraYaw) * cosPitch,
                FastMath.sin(pitch),
                FastMath.cos(cameraYaw) * cosPitch
        );
        if (tmpDirection.lengthSquared() < 0.0001f) {
            tmpDirection.set(Vector3f.UNIT_Z);
        } else {
            tmpDirection.normalizeLocal();
        }
    }

    private float eyeHeight() {
        float configured = options.getFirstPersonEyeHeight();
        if (configured > 0f) {
            return configured;
        }
        return Math.max(1.0f, collisionProfile.height() * 0.85f);
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

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial == null) {
            unsubscribeCameraPause();
            unsubscribeLookIntent();
            ready = false;
            character = null;
            cameraNode = null;
            transitionActive = false;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private void subscribeCameraPause() {
        if (context == null || cameraPauseSubscription != null) {
            return;
        }
        cameraPauseSubscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.CAMERA_FOLLOW_PAUSE_REQUESTED,
                this::onCameraPauseRequested,
                true
        );
    }

    private void subscribeLookIntent() {
        if (context == null || lookIntentSubscription != null) {
            return;
        }
        lookIntentSubscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.PLAYER_LOOK_INTENT,
                this::onLookIntent,
                true
        );
    }

    private void unsubscribeCameraPause() {
        if (cameraPauseSubscription == null) {
            return;
        }
        try {
            cameraPauseSubscription.close();
        } catch (Exception ignored) {
        }
        cameraPauseSubscription = null;
    }

    private void unsubscribeLookIntent() {
        if (lookIntentSubscription == null) {
            return;
        }
        try {
            lookIntentSubscription.close();
        } catch (Exception ignored) {
        }
        lookIntentSubscription = null;
    }

    private void onCameraPauseRequested(Map<String, Object> event) {
        Map<String, Object> payload = payload(event);
        cameraFollowPaused = Boolean.TRUE.equals(payload.get("paused"));
    }

    private void onLookIntent(Map<String, Object> event) {
        Map<String, Object> payload = payload(event);
        if (longNumber(payload, "playerRef", -1L) != playerRef) {
            return;
        }
        targetCameraYaw = number(payload, "yaw", targetCameraYaw);
        targetCameraPitch = FastMath.clamp(number(payload, "pitch", targetCameraPitch), MIN_CAMERA_PITCH, MAX_CAMERA_PITCH);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        if (payload instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
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
}
