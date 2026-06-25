package org.takesome.frozenlands.engine.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.lua.LuaProviderBridge;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoreModule implements EngineModule {
    public static final String ID = "engine.core";

    private final EngineContext context;
    private final ScriptRuntime scripts;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public CoreModule(EngineContext context) {
        this.context = context;
        this.scripts = new ScriptRuntime(context);
        registerCommands();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Core Lua bridge, scripting support and console command routing";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    public List<Map<String, Object>> runConfiguredAutoRunScripts() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> response : scripts.runAutoRunScripts()) {
            publishScriptOutcome(response, true);
            results.add(response);
        }
        return results;
    }

    private void registerCommands() {
        commands.put("status", ModuleCommand.of("status", "Return core runtime status", args -> status()));
        commands.put("manifest", ModuleCommand.of("manifest", "Return full Lua runtime manifest", args -> bridge().exportRuntimeManifest()));
        commands.put("modules", ModuleCommand.of("modules", "List registered module ids", args -> result("modules", moduleIds())));
        commands.put("providers", ModuleCommand.of("providers", "List registered provider ids", args -> result("providers", providerIds())));
        commands.put("call.module", ModuleCommand.of("call.module", "Call another module command", this::callModule));
        commands.put("call.provider", ModuleCommand.of("call.provider", "Call a provider command", this::callProvider));
        commands.put("event.publish", ModuleCommand.of("event.publish", "Publish a module event", this::publishEvent));
        commands.put("events.snapshot", ModuleCommand.of("events.snapshot", "Snapshot Java-side module/provider events", args -> result("events", bridge().snapshotJavaEvents())));
        commands.put("events.drain", ModuleCommand.of("events.drain", "Drain Java-side module/provider events", args -> result("events", bridge().drainJavaEvents())));
        commands.put("script.manifest", ModuleCommand.of("script.manifest", "Return script runtime manifest", args -> scripts.manifest()));
        commands.put("script.list", ModuleCommand.of("script.list", "List indexed Lua scripts", args -> scripts.list()));
        commands.put("script.read", ModuleCommand.of("script.read", "Read one indexed Lua script", scripts::read));
        commands.put("script.run", ModuleCommand.of("script.run", "Execute one indexed Lua script through the core bridge", this::runScript));
        commands.put("script.autorun", ModuleCommand.of("script.autorun", "Execute configured Lua autorun scripts", args -> result("results", runConfiguredAutoRunScripts())));
        commands.put("console.execute", ModuleCommand.of("console.execute", "Execute a console command line", this::executeConsole));
    }

    private Map<String, Object> status() {
        Map<String, Object> status = result("module", ID);
        status.put("modules", moduleIds());
        status.put("providers", providerIds());
        status.put("scripting", scripts.manifest());
        return status;
    }

    private Map<String, Object> callModule(Map<String, Object> args) {
        return context.getModuleRegistry().call(stringArg(args, "module", ""), stringArg(args, "command", "status"), mapArg(args, "args"));
    }

    private Map<String, Object> callProvider(Map<String, Object> args) {
        return context.getProviderRegistry().call(stringArg(args, "provider", ""), stringArg(args, "command", "status"), mapArg(args, "args"));
    }

    private Map<String, Object> publishEvent(Map<String, Object> args) {
        return context.getModuleRegistry().publishEvent(stringArg(args, "topic", "core.event"), mapArg(args, "payload"));
    }

    private Map<String, Object> runScript(Map<String, Object> args) {
        String script = stringArg(args, "script", "startup.lua");
        context.getModuleRegistry().publishEvent("core.script.run.requested", Map.of("script", script));
        Map<String, Object> response = scripts.run(args);
        publishScriptOutcome(response, false);
        return response;
    }

    private void publishScriptOutcome(Map<String, Object> response, boolean autoRun) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("script", response.get("script"));
        payload.put("path", response.get("path"));
        payload.put("ok", response.get("ok"));
        payload.put("executed", response.get("executed"));
        payload.put("autoRun", autoRun);
        if (response.containsKey("error")) {
            payload.put("error", response.get("error"));
        }
        String topic = Boolean.TRUE.equals(response.get("ok")) ? "core.script.executed" : "core.script.failed";
        context.getModuleRegistry().publishEvent(topic, payload);
    }

    private Map<String, Object> executeConsole(Map<String, Object> args) {
        String line = stringArg(args, "line", "").trim();
        if (line.isBlank()) {
            return error("empty-console-line");
        }
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 2) {
            return error("console-line-must-be: <moduleId> <commandId> [jsonArgs]");
        }
        Map<String, Object> parsedArgs = parts.length == 3 ? parseJsonArgs(parts[2]) : Map.of();
        Map<String, Object> response = context.getModuleRegistry().call(parts[0], parts[1], parsedArgs);
        context.getModuleRegistry().publishEvent("core.console.executed", Map.of("line", line, "module", parts[0], "command", parts[1]));
        return response;
    }

    private Map<String, Object> parseJsonArgs(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (IOException e) {
            throw new IllegalArgumentException("Console JSON arguments are invalid: " + raw, e);
        }
    }

    private LuaProviderBridge bridge() {
        return new LuaProviderBridge(context);
    }

    private List<String> moduleIds() {
        return List.copyOf(context.getModuleRegistry().snapshot().keySet());
    }

    private List<String> providerIds() {
        return List.copyOf(context.getProviderRegistry().snapshot().keySet());
    }

    private Map<String, Object> mapArg(Map<String, Object> args, String key) {
        Object value = args == null ? null : args.get(key);
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> map = new LinkedHashMap<>();
            source.forEach((k, v) -> map.put(String.valueOf(k), v));
            return map;
        }
        return Map.of();
    }

    private String stringArg(Map<String, Object> args, String key, String fallback) {
        Object value = args == null ? null : args.get(key);
        return value == null ? fallback : String.valueOf(value);
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
