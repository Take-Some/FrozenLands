package org.takesome.frozenlands.engine.modules;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleEventBus {
    private final List<Map<String, Object>> events = new ArrayList<>();

    public synchronized Map<String, Object> publish(String topic, Map<String, Object> payload) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Module event topic must not be blank");
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("topic", topic);
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload == null ? Collections.emptyMap() : new LinkedHashMap<>(payload));
        events.add(Collections.unmodifiableMap(event));
        return event;
    }

    public synchronized List<Map<String, Object>> snapshot() {
        return List.copyOf(events);
    }

    public synchronized List<Map<String, Object>> drain() {
        List<Map<String, Object>> drained = List.copyOf(events);
        events.clear();
        return drained;
    }
}
