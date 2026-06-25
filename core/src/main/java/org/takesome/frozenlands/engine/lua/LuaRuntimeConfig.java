package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LuaRuntimeConfig {
    private final ModuleIndexCatalog moduleIndexCatalog = ModuleIndexCatalog.defaultCatalog();

    public Map<String, Object> read(String moduleId) {
        String indexedPath = moduleIndexCatalog.optionalConfigPath(moduleId, "runtime");
        return indexedPath == null ? Map.of() : moduleIndexCatalog.readJsonMap(indexedPath);
    }

    public Map<String, Object> map(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Map.of();
    }

    public String string(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public int integer(Map<String, Object> source, String key, int fallback) {
        Object value = source.get(key);
        return value instanceof Number number ? number.intValue()
                : value == null ? fallback : Integer.parseInt(String.valueOf(value));
    }

    public float floating(Map<String, Object> source, String key, float fallback) {
        Object value = source.get(key);
        return value instanceof Number number ? number.floatValue()
                : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    public boolean bool(Map<String, Object> source, String key, boolean fallback) {
        Object value = source.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
}
