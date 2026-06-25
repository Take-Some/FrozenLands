package org.takesome.frozenlands.engine.shaders;

import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShadowSettings {
    private static final String MODULE_ID = "engine.shaders";

    private int shadowMapSize;
    private int splits;
    private float shadowZExtend;
    private float shadowIntensity;
    private int edgesThickness;

    public ShadowSettings() {
        LuaRuntimeConfig loader = new LuaRuntimeConfig();
        Map<String, Object> config = loader.read(MODULE_ID);
        Map<String, Object> shadows = loader.map(config, "shadows");
        shadowMapSize = loader.integer(shadows, "shadowMapSize", 2048);
        splits = loader.integer(shadows, "splits", 3);
        shadowZExtend = loader.floating(shadows, "shadowZExtend", 180f);
        shadowIntensity = loader.floating(shadows, "shadowIntensity", 0.45f);
        edgesThickness = loader.integer(shadows, "edgesThickness", 3);
    }

    public int getShadowMapSize() { return shadowMapSize; }
    public int getSplits() { return splits; }
    public float getShadowZExtend() { return shadowZExtend; }
    public float getShadowIntensity() { return shadowIntensity; }
    public int getEdgesThickness() { return edgesThickness; }

    public void update(Map<String, Object> values) {
        if (values == null) return;
        shadowMapSize = intArg(values, "shadowMapSize", shadowMapSize);
        splits = intArg(values, "splits", splits);
        shadowZExtend = floatArg(values, "shadowZExtend", shadowZExtend);
        shadowIntensity = floatArg(values, "shadowIntensity", shadowIntensity);
        edgesThickness = intArg(values, "edgesThickness", edgesThickness);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("shadowMapSize", shadowMapSize);
        map.put("splits", splits);
        map.put("shadowZExtend", shadowZExtend);
        map.put("shadowIntensity", shadowIntensity);
        map.put("edgesThickness", edgesThickness);
        return map;
    }

    private int intArg(Map<String, Object> values, String name, int fallback) {
        Object value = values.get(name);
        return value instanceof Number number ? number.intValue() : value == null ? fallback : Integer.parseInt(String.valueOf(value));
    }

    private float floatArg(Map<String, Object> values, String name, float fallback) {
        Object value = values.get(name);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }
}
