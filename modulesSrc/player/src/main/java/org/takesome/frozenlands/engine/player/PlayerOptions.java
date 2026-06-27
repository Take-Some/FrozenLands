package org.takesome.frozenlands.engine.player;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.player.camera.CameraViewMode;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;

import java.util.Map;

public class PlayerOptions {
    private static final ModuleIndexCatalog MODULE_INDEX = ModuleIndexCatalog.defaultCatalog();

    private BetterCharacterControl characterControl;
    private String modelPath;
    private float scale;
    private Camera fpsCam;
    private Spatial.CullHint cullHint;
    private RenderQueue.ShadowMode shadowMode;
    private Vector3f jumpForce;
    private int initialHealth;
    private float mass;
    private String collisionSourceNode;
    private float collisionRadiusOverride;
    private float collisionHeightOverride;
    private float collisionRadiusScale;
    private float collisionHeightScale;
    private float minCollisionRadius;
    private float minCollisionHeight;
    private CameraViewMode defaultCameraView;
    private float firstPersonEyeHeight;
    private boolean firstPersonBodyVisible;
    private float firstPersonCameraForwardOffset;
    private float firstPersonCameraVerticalOffset;
    private float firstPersonLookAheadDistance;
    private float thirdPersonDistance;
    private float thirdPersonHeight;
    private float frontPersonDistance;
    private float frontPersonHeight;
    private float cameraTransitionSeconds;
    private float mouseSensitivity;
    private float minMouseSensitivity;
    private float maxMouseSensitivity;
    private boolean sanitizeSkinnedTangents;

    public PlayerOptions(Map<String, Object> options) {
        loadOptions(options);
    }

    public static PlayerOptions load(String path) {
        return new PlayerOptions(MODULE_INDEX.readJsonMap(path));
    }

    private void loadOptions(Map<String, Object> optionsMap) {
        this.modelPath = requiredAssetPath(optionsMap, "model", "player.model");
        this.scale = RuntimeMaps.floating(optionsMap, "scale", 1f);
        this.cullHint = Spatial.CullHint.valueOf(RuntimeMaps.string(optionsMap, "cullHint", "Never"));
        this.shadowMode = RenderQueue.ShadowMode.valueOf(RuntimeMaps.string(optionsMap, "shadowMode", "Cast"));
        this.jumpForce = new Vector3f(0, RuntimeMaps.floating(optionsMap, "jumpForce", 500f), 0);
        this.initialHealth = RuntimeMaps.integer(optionsMap, "initialHealth", 100);
        this.mass = RuntimeMaps.floating(optionsMap, "mass", 175f);

        Map<String, Object> modelPipeline = RuntimeMaps.map(optionsMap, "modelPipeline");
        this.sanitizeSkinnedTangents = RuntimeMaps.bool(modelPipeline, "sanitizeSkinnedTangents", true);

        Map<String, Object> collision = RuntimeMaps.map(optionsMap, "collision");
        this.collisionSourceNode = RuntimeMaps.string(collision, "sourceNode", "collision");
        this.collisionRadiusOverride = RuntimeMaps.floating(collision, "radiusOverride", -1f);
        this.collisionHeightOverride = RuntimeMaps.floating(collision, "heightOverride", -1f);
        this.collisionRadiusScale = RuntimeMaps.floating(collision, "radiusScale", 1.0f);
        this.collisionHeightScale = RuntimeMaps.floating(collision, "heightScale", 1.0f);
        this.minCollisionRadius = RuntimeMaps.floating(collision, "minRadius", RuntimeMaps.floating(optionsMap, "radius", 0.25f));
        this.minCollisionHeight = RuntimeMaps.floating(collision, "minHeight", RuntimeMaps.floating(optionsMap, "height", 1.0f));

        Map<String, Object> camera = RuntimeMaps.map(optionsMap, "camera");
        this.defaultCameraView = CameraViewMode.parse(RuntimeMaps.string(camera, "defaultView", "FIRST_PERSON"), CameraViewMode.FIRST_PERSON);
        this.firstPersonEyeHeight = RuntimeMaps.floating(camera, "firstPersonEyeHeight", 1.65f);
        this.firstPersonBodyVisible = RuntimeMaps.bool(camera, "firstPersonBodyVisible", true);
        this.firstPersonCameraForwardOffset = RuntimeMaps.floating(camera, "firstPersonCameraForwardOffset", 0.18f);
        this.firstPersonCameraVerticalOffset = RuntimeMaps.floating(camera, "firstPersonCameraVerticalOffset", -0.08f);
        this.firstPersonLookAheadDistance = Math.max(0.25f, RuntimeMaps.floating(camera, "firstPersonLookAheadDistance", 8.0f));
        this.thirdPersonDistance = RuntimeMaps.floating(camera, "thirdPersonDistance", 6.0f);
        this.thirdPersonHeight = RuntimeMaps.floating(camera, "thirdPersonHeight", 2.15f);

        Map<String, Object> input = RuntimeMaps.map(optionsMap, "input");
        this.minMouseSensitivity = Math.max(0.01f, RuntimeMaps.floating(input, "minMouseSensitivity", 0.2f));
        this.maxMouseSensitivity = Math.max(this.minMouseSensitivity, RuntimeMaps.floating(input, "maxMouseSensitivity", 3.0f));
        this.mouseSensitivity = clamp(RuntimeMaps.floating(input, "mouseSensitivity", 1.0f), minMouseSensitivity, maxMouseSensitivity);
        this.frontPersonDistance = RuntimeMaps.floating(camera, "frontPersonDistance", this.thirdPersonDistance * 0.85f);
        this.frontPersonHeight = RuntimeMaps.floating(camera, "frontPersonHeight", this.thirdPersonHeight);
        this.cameraTransitionSeconds = Math.max(0.01f, RuntimeMaps.floating(camera, "transitionSeconds", 0.32f));
    }







    private String requiredAssetPath(Map<String, Object> options, String key, String configKey) {
        String value = RuntimeMaps.string(options, key, "").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Required player asset path is not configured: " + configKey);
        }
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public String getModelPath() { return modelPath; }
    public float getScale() { return scale; }
    public Spatial.CullHint getCullHint() { return cullHint; }
    public RenderQueue.ShadowMode getShadowMode() { return shadowMode; }
    public Vector3f getJumpForce() { return jumpForce; }
    public int getInitialHealth() { return initialHealth; }
    public float getMass() { return mass; }
    public String getCollisionSourceNode() { return collisionSourceNode; }
    public float getCollisionRadiusOverride() { return collisionRadiusOverride; }
    public float getCollisionHeightOverride() { return collisionHeightOverride; }
    public float getCollisionRadiusScale() { return collisionRadiusScale; }
    public float getCollisionHeightScale() { return collisionHeightScale; }
    public float getMinCollisionRadius() { return minCollisionRadius; }
    public float getMinCollisionHeight() { return minCollisionHeight; }
    public CameraViewMode getDefaultCameraView() { return defaultCameraView; }
    public float getFirstPersonEyeHeight() { return firstPersonEyeHeight; }
    public boolean isFirstPersonBodyVisible() { return firstPersonBodyVisible; }
    public float getFirstPersonCameraForwardOffset() { return firstPersonCameraForwardOffset; }
    public float getFirstPersonCameraVerticalOffset() { return firstPersonCameraVerticalOffset; }
    public float getFirstPersonLookAheadDistance() { return firstPersonLookAheadDistance; }
    public float getThirdPersonDistance() { return thirdPersonDistance; }
    public float getThirdPersonHeight() { return thirdPersonHeight; }
    public float getFrontPersonDistance() { return frontPersonDistance; }
    public float getFrontPersonHeight() { return frontPersonHeight; }
    public float getCameraTransitionSeconds() { return cameraTransitionSeconds; }
    public Camera getFpsCam() { return fpsCam; }
    public void setFpsCam(Camera fpsCam) { this.fpsCam = fpsCam; }
    public float getMouseSensitivity() { return mouseSensitivity; }
    public float getMinMouseSensitivity() { return minMouseSensitivity; }
    public float getMaxMouseSensitivity() { return maxMouseSensitivity; }
    public float setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = clamp(mouseSensitivity, minMouseSensitivity, maxMouseSensitivity);
        return this.mouseSensitivity;
    }
    public boolean sanitizeSkinnedTangents() { return sanitizeSkinnedTangents; }
    public BetterCharacterControl getCharacterControl() { return characterControl; }
    public void setCharacterControl(BetterCharacterControl characterControl) { this.characterControl = characterControl; }
    public void setInitialHealth(int initialHealth) { this.initialHealth = initialHealth; }
}
