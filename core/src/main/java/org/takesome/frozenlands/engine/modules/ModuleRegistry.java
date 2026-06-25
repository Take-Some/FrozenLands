package org.takesome.frozenlands.engine.modules;

import org.takesome.frozenlands.engine.EngineContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ModuleRegistry {
    private final Map<String, EngineModule> modulesById = new LinkedHashMap<>();
    private final ModuleEventBus eventBus = new ModuleEventBus();

    public EngineModule register(EngineModule module, EngineContext context) {
        if (module == null) {
            throw new IllegalArgumentException("Module must not be null");
        }
        if (module.id() == null || module.id().isBlank()) {
            throw new IllegalArgumentException("Module id must not be blank");
        }
        if (modulesById.containsKey(module.id())) {
            throw new IllegalStateException("Module already registered: " + module.id());
        }

        modulesById.put(module.id(), module);
        module.onRegister(context);
        publishEvent("module.registered", Map.of("module", module.id()));
        return module;
    }

    public Optional<EngineModule> find(String id) {
        return Optional.ofNullable(modulesById.get(id));
    }

    public EngineModule require(String id) {
        return find(id).orElseThrow(() -> new IllegalStateException("Module is not registered: " + id));
    }

    public Map<String, Object> call(String moduleId, String commandId, Map<String, Object> arguments) {
        Map<String, Object> result = require(moduleId).execute(commandId, arguments);
        publishEvent("module.command.executed", Map.of("module", moduleId, "command", commandId));
        return result;
    }

    public Map<String, Object> publishEvent(String topic, Map<String, Object> payload) {
        return eventBus.publish(topic, payload);
    }

    public ModuleEventBus getEventBus() {
        return eventBus;
    }

    public Map<String, EngineModule> snapshot() {
        return Collections.unmodifiableMap(modulesById);
    }

    public Map<String, Object> luaManifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        modulesById.forEach((id, module) -> manifest.put(id, module.luaDescriptor()));
        return Collections.unmodifiableMap(manifest);
    }
}
