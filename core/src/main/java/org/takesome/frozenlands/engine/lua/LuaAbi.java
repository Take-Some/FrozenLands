package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.runtime.EngineRuntimeOptions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable Lua ABI descriptor shared by manifest output and the Java bridge. */
public final class LuaAbi {
    public static final String ENGINE_NAME = "luaj-jse";
    public static final String ENGINE_VERSION = "3.0.1";

    private static final List<String> DISABLED_GLOBALS = List.of("io", "os", "debug", "luajava", "dofile", "loadfile");
    private static final List<String> BRIDGE_FUNCTIONS = List.of(
            "callModule",
            "callProvider",
            "publishModuleEvent",
            "publishProviderEvent",
            "manifest",
            "console",
            "consoleHelp",
            "consoleVersion",
            "commandsList",
            "consoleComplete",
            "eventsSnapshot",
            "eventsDrain",
            "emit",
            "publishEvent"
    );

    private LuaAbi() {
    }

    public static List<String> disabledGlobals() {
        return DISABLED_GLOBALS;
    }

    public static List<String> bridgeFunctions() {
        return BRIDGE_FUNCTIONS;
    }

    public static String bridgeId() {
        return "engine.core/java." + String.join("/java.", BRIDGE_FUNCTIONS);
    }

    public static Map<String, Object> descriptor(Collection<String> moduleIds) {
        EngineRuntimeOptions options = EngineRuntimeOptions.defaultOptions();
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("abi", "frozenlands.lua.abi.v" + options.luaAbiVersion());
        descriptor.put("abiVersion", options.luaAbiVersion());
        descriptor.put("bridgeVersion", options.luaBridgeVersion());
        descriptor.put("engine", ENGINE_NAME);
        descriptor.put("engineVersion", ENGINE_VERSION);
        descriptor.put("bridge", bridgeId());
        descriptor.put("disabledGlobals", DISABLED_GLOBALS);
        descriptor.put("bridgeFunctions", BRIDGE_FUNCTIONS);
        descriptor.put("preloadedModules", moduleIds == null ? List.of() : List.copyOf(moduleIds));
        descriptor.put("maxDepth", options.luaMaxDepth());
        descriptor.put("maxTableEntries", options.luaMaxTableEntries());
        return descriptor;
    }
}
