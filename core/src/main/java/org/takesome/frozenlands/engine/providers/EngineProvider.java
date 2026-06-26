package org.takesome.frozenlands.engine.providers;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface EngineProvider {
    String id();

    default List<String> dependencies() {
        return Collections.emptyList();
    }

    default Map<String, ProviderCommand> commands() {
        return Collections.emptyMap();
    }

    default String luaApiPath() {
        return ModuleIndexCatalog.defaultCatalog().luaPath(id(), "api");
    }

    default void register(EngineContext context) {
        // Provider-specific startup hook.
    }

    default void unregister(EngineContext context) {
        // Provider-specific shutdown hook.
    }

    default Map<String, Object> execute(String commandId, Map<String, Object> arguments) {
        ProviderCommand command = commands().get(commandId);
        if (command == null) {
            throw new IllegalArgumentException("Provider command is not registered: " + id() + "." + commandId);
        }
        return command.execute(arguments == null ? Collections.emptyMap() : arguments);
    }

    default Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("id", id());
        descriptor.put("class", getClass().getName());
        descriptor.put("luaApi", luaApiPath());
        descriptor.put("dependencies", dependencies());

        Map<String, Object> commandDescriptors = new LinkedHashMap<>();
        commands().forEach((commandId, command) -> commandDescriptors.put(commandId, command.luaDescriptor()));
        descriptor.put("commands", commandDescriptors);
        return descriptor;
    }
}
