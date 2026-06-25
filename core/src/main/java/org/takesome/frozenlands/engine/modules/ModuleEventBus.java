package org.takesome.frozenlands.engine.modules;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ModuleEventBus {
    private final List<Map<String, Object>> events = new ArrayList<>();
    private final Map<String, List<Consumer<Map<String, Object>>>> listenersByTopic = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> latestByTopic = new LinkedHashMap<>();

    public Map<String, Object> publish(String topic, Map<String, Object> payload) {
        return publish(topic, payload, true);
    }

    public Map<String, Object> publishLive(String topic, Map<String, Object> payload) {
        return publish(topic, payload, false);
    }

    private Map<String, Object> publish(String topic, Map<String, Object> payload, boolean record) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Module event topic must not be blank");
        }
        Map<String, Object> event = event(topic, payload);
        List<Consumer<Map<String, Object>>> listeners;
        synchronized (this) {
            latestByTopic.put(topic, Collections.unmodifiableMap(event));
            if (record) {
                events.add(Collections.unmodifiableMap(event));
            }
            listeners = List.copyOf(listenersByTopic.getOrDefault(topic, List.of()));
        }
        for (Consumer<Map<String, Object>> listener : listeners) {
            listener.accept(event);
        }
        return event;
    }

    public AutoCloseable subscribe(String topic, Consumer<Map<String, Object>> listener) {
        return subscribe(topic, listener, false);
    }

    public AutoCloseable subscribe(String topic, Consumer<Map<String, Object>> listener, boolean replayLatest) {
        Map<String, Object> latest;
        synchronized (this) {
            if (topic == null || topic.isBlank()) {
                throw new IllegalArgumentException("Module event topic must not be blank");
            }
            if (listener == null) {
                throw new IllegalArgumentException("Module event listener must not be null");
            }
            listenersByTopic.computeIfAbsent(topic, ignored -> new ArrayList<>()).add(listener);
            latest = latestByTopic.get(topic);
        }
        if (replayLatest && latest != null) {
            listener.accept(latest);
        }
        return () -> unsubscribe(topic, listener);
    }

    public synchronized List<Map<String, Object>> snapshot() {
        return List.copyOf(events);
    }

    public synchronized List<Map<String, Object>> drain() {
        List<Map<String, Object>> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    private synchronized void unsubscribe(String topic, Consumer<Map<String, Object>> listener) {
        List<Consumer<Map<String, Object>>> listeners = listenersByTopic.get(topic);
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            listenersByTopic.remove(topic);
        }
    }

    private Map<String, Object> event(String topic, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("topic", topic);
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload == null ? Collections.emptyMap() : new LinkedHashMap<>(payload));
        return event;
    }
}
