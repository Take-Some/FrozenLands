package org.takesome.frozenlands.engine.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical conversion layer for runtime command payloads.
 *
 * <p>The engine moves command arguments through Java maps, JSON and Lua tables.
 * Keeping coercion rules here prevents modules from re-implementing slightly
 * different string/int/bool/map parsing.</p>
 */
public final class RuntimeMaps {
    private RuntimeMaps() {
    }

    public static Map<String, Object> map(Map<String, Object> source, String key) {
        if (source == null) {
            return Map.of();
        }
        return map(source.get(key));
    }

    public static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, val) -> result.put(String.valueOf(key), val));
        return result;
    }

    public static List<Object> list(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return List.of();
    }

    public static List<String> stringList(Object value) {
        List<Object> values = list(value);
        if (values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (Object item : values) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return List.copyOf(result);
    }

    public static String string(Map<String, Object> source, String key, String fallback) {
        Object value = source == null ? null : source.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public static int integer(Map<String, Object> source, String key, int fallback) {
        Object value = source == null ? null : source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static float floating(Map<String, Object> source, String key, float fallback) {
        Object value = source == null ? null : source.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static boolean bool(Map<String, Object> source, String key, boolean fallback) {
        Object value = source == null ? null : source.get(key);
        return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    public static Map<String, Object> mutableCopy(Map<String, Object> source) {
        return source == null || source.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    public static Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("error", message);
        return result;
    }
}
