package org.takesome.frozenlands.engine.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared ABI contract for module and provider commands. */
public interface RuntimeCommand {
    String id();

    default String description() {
        return "";
    }

    Map<String, Object> execute(Map<String, Object> arguments);

    default Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("id", id());
        descriptor.put("description", description());
        return descriptor;
    }

    @FunctionalInterface
    interface Executor {
        Map<String, Object> execute(Map<String, Object> arguments);
    }
}
