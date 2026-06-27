package org.takesome.frozenlands.engine.shaders;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;

import java.util.Map;

public final class ShaderRuntimeSettings {
    private final LuaRuntimeConfig loader = new LuaRuntimeConfig();
    private final Map<String, Object> config = loader.read("engine.shaders");
    private final Map<String, Object> pipeline = loader.map(config, "pipeline");
    private final Map<String, Object> effects = loader.map(config, "effects");
    private final Map<String, Object> bloom = loader.map(config, "bloom");
    private final Map<String, Object> scattering = loader.map(config, "lightScattering");
    private final Map<String, Object> dof = loader.map(config, "dof");

    public boolean pipelineEnabled() { return bool(pipeline, "enabled", true); }
    public boolean effectEnabled(String id, boolean fallback) { return bool(loader.map(effects, id), "enabled", fallback); }
    public float bloomIntensity() { return loader.floating(bloom, "intensity", 1f); }
    public int bloomExposurePower() { return loader.integer(bloom, "exposurePower", 60); }
    public float lightDensity() { return loader.floating(scattering, "density", 0.5f); }
    public Vector3f lightDirection() {
        return new Vector3f(loader.floating(scattering, "x", 1477.1023f),
                loader.floating(scattering, "y", 381.164f),
                loader.floating(scattering, "z", -1769.0748f));
    }
    public int focusDistance() { return loader.integer(dof, "focusDistance", 0); }
    public int focusRange() { return loader.integer(dof, "focusRange", 100); }

    private boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values == null ? null : values.get(key);
        return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
}
