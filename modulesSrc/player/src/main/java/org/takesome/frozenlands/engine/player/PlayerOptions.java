package org.takesome.frozenlands.engine.player;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
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
    public Camera getFpsCam() { return fpsCam; }
    public void setFpsCam(Camera fpsCam) { this.fpsCam = fpsCam; }
    public BetterCharacterControl getCharacterControl() { return characterControl; }
    public void setCharacterControl(BetterCharacterControl characterControl) { this.characterControl = characterControl; }
    public void setInitialHealth(int initialHealth) { this.initialHealth = initialHealth; }
}
