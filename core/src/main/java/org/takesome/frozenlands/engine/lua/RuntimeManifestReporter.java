package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.EngineContext;

import java.util.List;
import java.util.Map;

public final class RuntimeManifestReporter {
    private static final String FLAG = "frozenlands.runtimeManifest";

    private RuntimeManifestReporter() {
    }

    public static void reportIfRequested(EngineContext context) {
        if (!Boolean.getBoolean(FLAG)) {
            return;
        }

        Map<String, Object> manifest = new LuaProviderBridge(context).exportRuntimeManifest();
        Map<?, ?> providers = map(manifest.get("providers"));
        Map<?, ?> modules = map(manifest.get("modules"));

        System.out.println("[FrozenLands RuntimeManifest] providers=" + providers.keySet());
        System.out.println("[FrozenLands RuntimeManifest] modules=" + modules.keySet());
        reportModule(modules, "engine.core", List.of("status", "manifest", "modules", "providers", "call.module", "event.publish", "script.list", "script.read", "script.run", "script.autorun", "console.execute", "console.help", "console.version", "console.commandsList", "console.complete"));
        reportModule(modules, "engine.save", List.of("snapshot", "save", "load", "list"));
        reportModule(modules, "engine.particles", List.of("status", "snow.enable", "snow.rate", "emit", "impact"));
        reportModule(modules, "engine.terrain", List.of("status", "chunks", "heightAt", "spawnLocation"));
        reportModule(modules, "engine.sky", List.of("status", "command.execute", "atmosphere.setGradient", "weather.set", "weather.list", "clock.setTime", "environment.snapshot"));
        reportModule(modules, "engine.shaders", List.of("status", "setEnabled", "shadowSettings", "shadowSettings.set"));
        reportModule(modules, "engine.sound", List.of("load", "list.blocks", "list.events", "play"));
        reportModule(modules, "engine.material", List.of("load", "list", "get"));
        reportModule(modules, "engine.model", List.of("load", "list", "detach"));
        reportModule(modules, "engine.icoParser", List.of("status", "inspect", "best"));
    }

    private static void reportModule(Map<?, ?> modules, String id, List<String> requiredCommands) {
        Object moduleObject = modules.get(id);
        if (!(moduleObject instanceof Map<?, ?> module)) {
            System.out.println("[FrozenLands RuntimeManifest] MISSING module " + id);
            return;
        }

        Map<?, ?> commands = map(module.get("commands"));
        System.out.println("[FrozenLands RuntimeManifest] module " + id + " commands=" + commands.keySet());
        for (String command : requiredCommands) {
            if (!commands.containsKey(command)) {
                System.out.println("[FrozenLands RuntimeManifest] MISSING command " + id + "." + command);
            }
        }
    }

    private static Map<?, ?> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }
}
