package org.takesome.frozenlands.engine.modules;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface EngineModule {
    String id();

    default String description() {
        return "";
    }

    default List<String> dependencies() {
        return Collections.emptyList();
    }

    default Map<String, ModuleCommand> commands() {
        return Collections.emptyMap();
    }

    default String luaApiPath() {
        return ModuleIndexCatalog.defaultCatalog().luaPath(id(), "api");
    }

    default void onRegister(EngineContext context) {
        // Module-specific startup hook.
    }

    default void onUnregister(EngineContext context) {
        // Module-specific shutdown hook.
    }

    default Map<String, Object> execute(String commandId, Map<String, Object> arguments) {
        ModuleCommand command = commands().get(commandId);
        if (command == null) {
            throw new IllegalArgumentException("Module command is not registered: " + id() + "." + commandId);
        }
        return command.execute(arguments == null ? Collections.emptyMap() : arguments);
    }

    default Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("id", id());
        descriptor.put("description", description());
        descriptor.put("dependencies", dependencies());
        descriptor.put("class", getClass().getName());
        descriptor.put("luaApi", luaApiPath());

        Map<String, Object> commandDescriptors = new LinkedHashMap<>();
        commands().forEach((commandId, command) -> commandDescriptors.put(commandId, command.luaDescriptor()));
        descriptor.put("commands", commandDescriptors);
        return descriptor;
    }

    static EngineModule descriptor(String id, String description) {
        return new EngineModule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String description() {
                return description;
            }
        };
    }
}
