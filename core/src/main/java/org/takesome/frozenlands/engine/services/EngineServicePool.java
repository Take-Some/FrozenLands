package org.takesome.frozenlands.engine.services;

import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EngineServicePool {
    private final Logger logger;
    private final Map<Class<?>, Object> servicesByType = new ConcurrentHashMap<>();
    private final Map<String, Object> servicesById = new ConcurrentHashMap<>();
    private final Map<String, EngineServiceDescriptor> descriptors = new ConcurrentHashMap<>();

    public EngineServicePool(Logger logger) {
        this.logger = logger;
    }

    public <T> void register(Class<T> serviceType, T service) {
        register(serviceType.getName(), serviceType, service, "type");
    }

    public <T> void register(String id, Class<T> serviceType, T service) {
        register(id, serviceType, service, "runtime");
    }

    public synchronized <T> void register(String id, Class<T> serviceType, T service, String source) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("service id must not be blank");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service must not be null: " + serviceType.getName());
        }

        T cast = serviceType.cast(service);
        boolean replacedType = servicesByType.put(serviceType, cast) != null;
        boolean replacedId = servicesById.put(id, cast) != null;
        boolean replacement = replacedType || replacedId || descriptors.containsKey(id);
        EngineServiceDescriptor descriptor = new EngineServiceDescriptor(
                id,
                serviceType.getName(),
                service.getClass().getName(),
                source == null || source.isBlank() ? "runtime" : source,
                Instant.now(),
                replacement
        );
        descriptors.put(id, descriptor);

        if (logger != null) {
            logger.info(
                    "ServicePool registered id={} serviceType={} impl={} source={} replacement={}",
                    id,
                    serviceType.getName(),
                    service.getClass().getName(),
                    descriptor.source(),
                    replacement
            );
        }
    }

    public <T> Optional<T> find(Class<T> serviceType) {
        if (serviceType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(servicesByType.get(serviceType)).map(serviceType::cast);
    }

    public <T> Optional<T> find(String id, Class<T> serviceType) {
        if (id == null || id.isBlank() || serviceType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(servicesById.get(id)).map(serviceType::cast);
    }

    public List<Map<String, Object>> snapshot() {
        List<EngineServiceDescriptor> sorted = new ArrayList<>(descriptors.values());
        sorted.sort(Comparator.comparing(EngineServiceDescriptor::id));
        List<Map<String, Object>> result = new ArrayList<>(sorted.size());
        for (EngineServiceDescriptor descriptor : sorted) {
            result.add(descriptor.toMap());
        }
        return result;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("services", descriptors.size());
        status.put("typedServices", servicesByType.size());
        status.put("namedServices", servicesById.size());
        return status;
    }
}
