package org.takesome.frozenlands.engine.providers;

import java.util.LinkedHashMap;
import java.util.Map;

public interface ProviderCommand {
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

    static ProviderCommand of(String id, String description, ProviderCommandExecutor executor) {
        return new ProviderCommand() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public Map<String, Object> execute(Map<String, Object> arguments) {
                return executor.execute(arguments);
            }
        };
    }

    @FunctionalInterface
    interface ProviderCommandExecutor {
        Map<String, Object> execute(Map<String, Object> arguments);
    }
}
