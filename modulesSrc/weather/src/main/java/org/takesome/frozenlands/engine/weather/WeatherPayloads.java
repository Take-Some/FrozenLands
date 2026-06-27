package org.takesome.frozenlands.engine.weather;

import com.jme3.math.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

final class WeatherPayloads {
    private WeatherPayloads() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, val) -> result.put(String.valueOf(key), val));
        return result;
    }

    static Vector3f vector(Map<String, Object> payload, Vector3f fallback) {
        Vector3f base = fallback == null ? Vector3f.ZERO : fallback;
        return new Vector3f(
                number(payload, "x", base.x),
                number(payload, "y", base.y),
                number(payload, "z", base.z)
        );
    }

    static Map<String, Object> vectorMap(Vector3f vector) {
        Vector3f value = vector == null ? Vector3f.ZERO : vector;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("x", value.x);
        result.put("y", value.y);
        result.put("z", value.z);
        return result;
    }

    static String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    static boolean bool(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    static float number(Map<String, Object> payload, String key, float fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }
}
