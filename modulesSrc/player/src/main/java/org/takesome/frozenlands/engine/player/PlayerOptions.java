package org.takesome.frozenlands.engine.player;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.player.camera.CameraViewMode;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

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
    private float thirdPersonDistance;
    private float thirdPersonHeight;
    private float frontPersonDistance;
    private float frontPersonHeight;
    private float cameraTransitionSeconds;

    public PlayerOptions(Map<String, Object> options) {
        loadOptions(options);
    }

    public static PlayerOptions load(String path) {
        return new PlayerOptions(MODULE_INDEX.readJsonMap(path));
    }

    private void loadOptions(Map<String, Object> optionsMap) {
        this.modelPath = stringValue(optionsMap, "model", "Models/char.glb");
        this.scale = floatValue(optionsMap, "scale", 1f);
        this.cullHint = Spatial.CullHint.valueOf(stringValue(optionsMap, "cullHint", "Never"));
        this.shadowMode = RenderQueue.ShadowMode.valueOf(stringValue(optionsMap, "shadowMode", "Cast"));
        this.jumpForce = new Vector3f(0, floatValue(optionsMap, "jumpForce", 500f), 0);
        this.initialHealth = intValue(optionsMap, "initialHealth", 100);
        this.mass = floatValue(optionsMap, "mass", 175f);

        Map<String, Object> collision = mapValue(optionsMap, "collision");
        this.collisionSourceNode = stringValue(collision, "sourceNode", "collision");
        this.collisionRadiusOverride = floatValue(collision, "radiusOverride", -1f);
        this.collisionHeightOverride = floatValue(collision, "heightOverride", -1f);
        this.collisionRadiusScale = floatValue(collision, "radiusScale", 1.0f);
        this.collisionHeightScale = floatValue(collision, "heightScale", 1.0f);
        this.minCollisionRadius = floatValue(collision, "minRadius", floatValue(optionsMap, "radius", 0.25f));
        this.minCollisionHeight = floatValue(collision, "minHeight", floatValue(optionsMap, "height", 1.0f));

        Map<String, Object> camera = mapValue(optionsMap, "camera");
        this.defaultCameraView = CameraViewMode.parse(stringValue(camera, "defaultView", "FIRST_PERSON"), CameraViewMode.FIRST_PERSON);
        this.firstPersonEyeHeight = floatValue(camera, "firstPersonEyeHeight", 1.65f);
        this.thirdPersonDistance = floatValue(camera, "thirdPersonDistance", 6.0f);
        this.thirdPersonHeight = floatValue(camera, "thirdPersonHeight", 2.15f);
        this.frontPersonDistance = floatValue(camera, "frontPersonDistance", this.thirdPersonDistance * 0.85f);
        this.frontPersonHeight = floatValue(camera, "frontPersonHeight", this.thirdPersonHeight);
        this.cameraTransitionSeconds = Math.max(0.01f, floatValue(camera, "transitionSeconds", 0.32f));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Map<String, Object> options, String key) {
        Object value = options.get(key);
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private String stringValue(Map<String, Object> options, String key, String fallback) {
        Object value = options.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private float floatValue(Map<String, Object> options, String key, float fallback) {
        Object value = options.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    private int intValue(Map<String, Object> options, String key, int fallback) {
        Object value = options.get(key);
        return value instanceof Number number ? number.intValue() : value == null ? fallback : Integer.parseInt(String.valueOf(value));
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
    public float getThirdPersonDistance() { return thirdPersonDistance; }
    public float getThirdPersonHeight() { return thirdPersonHeight; }
    public float getFrontPersonDistance() { return frontPersonDistance; }
    public float getFrontPersonHeight() { return frontPersonHeight; }
    public float getCameraTransitionSeconds() { return cameraTransitionSeconds; }
    public Camera getFpsCam() { return fpsCam; }
    public void setFpsCam(Camera fpsCam) { this.fpsCam = fpsCam; }
    public BetterCharacterControl getCharacterControl() { return characterControl; }
    public void setCharacterControl(BetterCharacterControl characterControl) { this.characterControl = characterControl; }
    public void setInitialHealth(int initialHealth) { this.initialHealth = initialHealth; }
}
