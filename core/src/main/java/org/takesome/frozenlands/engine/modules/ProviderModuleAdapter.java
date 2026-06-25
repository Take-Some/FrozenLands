package org.takesome.frozenlands.engine.modules;

import org.takesome.frozenlands.engine.providers.EngineProvider;
import org.takesome.frozenlands.engine.providers.ProviderCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProviderModuleAdapter implements EngineModule {
    private final EngineProvider provider;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    public ProviderModuleAdapter(EngineProvider provider) {
        this.provider = provider;
        provider.commands().forEach((id, command) -> commands.put(id, adapt(command)));
    }

    @Override
    public String id() {
        return provider.id();
    }

    @Override
    public String description() {
        return "Provider module API for " + provider.id();
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private ModuleCommand adapt(ProviderCommand command) {
        return ModuleCommand.of(command.id(), command.description(), command::execute);
    }
}
