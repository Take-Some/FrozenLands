package org.takesome.frozenlands.engine.services;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record EngineServiceDescriptor(
        String id,
        String serviceType,
        String implementationType,
        String source,
        Instant registeredAt,
        boolean replacement
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("serviceType", serviceType);
        map.put("implementationType", implementationType);
        map.put("source", source);
        map.put("registeredAt", registeredAt.toString());
        map.put("replacement", replacement);
        return map;
    }
}
