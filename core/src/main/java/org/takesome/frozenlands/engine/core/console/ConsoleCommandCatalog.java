package org.takesome.frozenlands.engine.core.console;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConsoleCommandCatalog {
    private static final String CORE_OWNER = "core";

    private final EngineContext context;

    ConsoleCommandCatalog(EngineContext context) {
        this.context = context;
    }

    Map<String, ConsoleCommandDescriptor> descriptorsByName() {
        Map<String, ConsoleCommandDescriptor> descriptors = new LinkedHashMap<>();
        addBaseCommands(descriptors);
        addDynamicCommands(descriptors);
        return descriptors;
    }

    List<ConsoleCommandDescriptor> descriptors() {
        List<ConsoleCommandDescriptor> descriptors = new ArrayList<>(descriptorsByName().values());
        descriptors.sort(Comparator.comparing(ConsoleCommandDescriptor::name));
        return List.copyOf(descriptors);
    }

    ConsoleCommandDescriptor descriptor(String name) {
        return descriptorsByName().get(name);
    }

    private void addBaseCommands(Map<String, ConsoleCommandDescriptor> descriptors) {
        descriptors.put("/help", base("/help", "Show console help or details for one command."));
        descriptors.put("/version", base("/version", "Show FrozenLands console/runtime version."));
        descriptors.put("/commandsList", base("/commandsList", "List base and dynamically exported module commands."));
    }

    private ConsoleCommandDescriptor base(String name, String description) {
        return new ConsoleCommandDescriptor(name, CORE_OWNER, "core", "engine.core", name.substring(1), description, false);
    }

    private void addDynamicCommands(Map<String, ConsoleCommandDescriptor> descriptors) {
        for (EngineModule module : context.getModuleRegistry().snapshot().values()) {
            String owner = owner(module.id());
            for (ModuleCommand command : module.commands().values()) {
                addDynamic(descriptors, module, command, "/" + owner + "." + command.id(), owner);
                addDynamic(descriptors, module, command, "/" + module.id() + "." + command.id(), owner);
            }
        }
    }

    private void addDynamic(Map<String, ConsoleCommandDescriptor> descriptors, EngineModule module, ModuleCommand command, String name, String owner) {
        descriptors.putIfAbsent(name, new ConsoleCommandDescriptor(
                name,
                owner,
                "module",
                module.id(),
                command.id(),
                command.description(),
                true
        ));
    }

    private String owner(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return "module";
        }
        return moduleId.startsWith("engine.") ? moduleId.substring("engine.".length()) : moduleId;
    }
}
