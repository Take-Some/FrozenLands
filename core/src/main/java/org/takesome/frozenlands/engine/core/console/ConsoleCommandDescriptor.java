package org.takesome.frozenlands.engine.core.console;

public record ConsoleCommandDescriptor(
        String name,
        String owner,
        String source,
        String moduleId,
        String commandId,
        String description,
        boolean dynamic
) {
}
