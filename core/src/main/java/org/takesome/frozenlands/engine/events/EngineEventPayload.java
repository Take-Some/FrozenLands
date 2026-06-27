package org.takesome.frozenlands.engine.events;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EngineEventPayload {
    private EngineEventPayload() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> of(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        if (!(payload instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    public static String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public static boolean bool(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    public static float floating(Map<String, Object> payload, String key, float fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    public static long integer(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : value == null ? fallback : Long.parseLong(String.valueOf(value));
    }

    public static Map<String, Object> copy(Map<String, Object> payload) {
        return payload == null || payload.isEmpty() ? Map.of() : new LinkedHashMap<>(payload);
    }
}
