package org.takesome.frozenlands.engine.core.console;

import org.takesome.frozenlands.engine.EngineContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoreConsole {
    public static final String VERSION = "FrozenLands Console 0.1.0";

    private final EngineContext context;
    private final ConsoleArgumentParser argumentParser = new ConsoleArgumentParser();
    private final ConsoleCommandCatalog commandCatalog;

    public CoreConsole(EngineContext context) {
        this.context = context;
        this.commandCatalog = new ConsoleCommandCatalog(context);
    }

    public Map<String, Object> execute(String line) {
        ConsoleRequest request = argumentParser.parse(line);
        if (request.command().isBlank()) {
            return error("empty-console-line");
        }
        if (!request.command().startsWith("/")) {
            return error("console-command-must-start-with-slash");
        }
        return switch (request.command()) {
            case "/help" -> help(request.rawArguments());
            case "/version" -> version();
            case "/commandsList" -> commandsList();
            default -> executeDynamic(request);
        };
    }

    public Map<String, Object> help(String commandName) {
        String normalized = normalizeCommandName(commandName);
        if (!normalized.isBlank()) {
            ConsoleCommandDescriptor descriptor = commandCatalog.descriptor(normalized);
            return descriptor == null ? error("unknown-console-command: " + normalized) : result("command", descriptorMap(descriptor));
        }
        Map<String, Object> response = result("owner", "engine.core");
        response.put("version", VERSION);
        response.put("usage", "/help [command], /version, /commandsList, /<module>.<command> [json|key=value]");
        response.put("baseCommands", List.of("/help", "/version", "/commandsList"));
        response.put("dynamicCommands", commandCatalog.descriptors().stream().filter(ConsoleCommandDescriptor::dynamic).map(ConsoleCommandDescriptor::name).toList());
        return response;
    }

    public Map<String, Object> version() {
        Map<String, Object> response = result("version", VERSION);
        response.put("owner", "engine.core");
        response.put("module", "engine.core");
        response.put("registeredCommands", commandCatalog.descriptors().size());
        return response;
    }

    public Map<String, Object> commandsList() {
        Map<String, Object> response = result("commands", commandCatalog.descriptors().stream().map(this::descriptorMap).toList());
        response.put("owner", "engine.core");
        response.put("count", commandCatalog.descriptors().size());
        return response;
    }

    public Map<String, Object> complete(String prefix) {
        String normalized = normalizeCommandName(prefix == null || prefix.isBlank() ? "/" : prefix);
        List<ConsoleCommandDescriptor> matches = commandCatalog.descriptors().stream()
                .filter(descriptor -> descriptor.name().startsWith(normalized))
                .toList();
        List<String> names = matches.stream().map(ConsoleCommandDescriptor::name).toList();
        Map<String, Object> response = result("prefix", normalized);
        response.put("completion", longestCommonPrefix(names, normalized));
        response.put("matches", matches.stream().map(this::descriptorMap).toList());
        response.put("count", matches.size());
        return response;
    }

    private Map<String, Object> executeDynamic(ConsoleRequest request) {
        ConsoleCommandDescriptor descriptor = commandCatalog.descriptor(request.command());
        if (descriptor == null || !descriptor.dynamic()) {
            return error("unknown-console-command: " + request.command());
        }
        Map<String, Object> moduleResponse = context.getModuleRegistry().call(descriptor.moduleId(), descriptor.commandId(), request.arguments());
        context.getModuleRegistry().publishEvent("core.console.executed", Map.of(
                "command", request.command(),
                "module", descriptor.moduleId(),
                "moduleCommand", descriptor.commandId()
        ));
        Map<String, Object> response = result("command", request.command());
        response.put("module", descriptor.moduleId());
        response.put("moduleCommand", descriptor.commandId());
        response.put("response", moduleResponse);
        return response;
    }

    private Map<String, Object> descriptorMap(ConsoleCommandDescriptor descriptor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", descriptor.name());
        map.put("owner", descriptor.owner());
        map.put("source", descriptor.source());
        map.put("module", descriptor.moduleId());
        map.put("command", descriptor.commandId());
        map.put("description", descriptor.description());
        map.put("dynamic", descriptor.dynamic());
        return map;
    }

    private String normalizeCommandName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String token = value.trim().split("\s+", 2)[0];
        return token.startsWith("/") ? token : "/" + token;
    }

    private String longestCommonPrefix(List<String> values, String fallback) {
        if (values.isEmpty()) {
            return fallback;
        }
        String prefix = values.get(0);
        for (String value : values) {
            while (!value.startsWith(prefix) && !prefix.isEmpty()) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix.isBlank() ? fallback : prefix;
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("error", message);
        return result;
    }
}
