package org.takesome.frozenlands.engine.runtime;

import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;

import java.util.Map;

/**
 * Data-driven runtime options backed by engine.core runtime.json.
 *
 * <p>This class is intentionally tolerant: if the runtime config cannot be read
 * during very early startup, stable defaults are used and the engine still boots.</p>
 */
public final class EngineRuntimeOptions {
    private static final String CORE_MODULE_ID = "engine.core";
    private static final EngineRuntimeOptions DEFAULT = loadDefault();

    private final Map<String, Object> root;

    public EngineRuntimeOptions(Map<String, Object> root) {
        this.root = root == null ? Map.of() : root;
    }

    public static EngineRuntimeOptions defaultOptions() {
        return DEFAULT;
    }

    public int eventHistoryLimit(String bus) {
        Map<String, Object> options = busOptions(bus);
        int min = RuntimeMaps.integer(options, "minHistoryLimit", 64);
        int configured = RuntimeMaps.integer(options, "historyLimit", 512);
        return Math.max(min, configured);
    }

    public boolean strictModuleDependencies() {
        return RuntimeMaps.bool(RuntimeMaps.map(root, "modules"), "strictDependencies", false);
    }

    public int luaAbiVersion() {
        return RuntimeMaps.integer(luaAbiOptions(), "version", 1);
    }

    public String luaBridgeVersion() {
        return RuntimeMaps.string(luaAbiOptions(), "bridgeVersion", "1.1");
    }

    public int luaMaxDepth() {
        return Math.max(8, RuntimeMaps.integer(luaAbiOptions(), "maxDepth", 32));
    }

    public int luaMaxTableEntries() {
        return Math.max(64, RuntimeMaps.integer(luaAbiOptions(), "maxTableEntries", 4096));
    }

    public Map<String, Object> luaAbiOptions() {
        return RuntimeMaps.map(root, "luaAbi");
    }

    public Map<String, Object> scriptingDefaults() {
        return RuntimeMaps.map(RuntimeMaps.map(root, "scripting"), "defaults");
    }

    private Map<String, Object> busOptions(String bus) {
        return RuntimeMaps.map(RuntimeMaps.map(root, "events"), bus == null ? "" : bus);
    }

    private static EngineRuntimeOptions loadDefault() {
        try {
            return new EngineRuntimeOptions(new LuaRuntimeConfig().read(CORE_MODULE_ID));
        } catch (RuntimeException ignored) {
            return new EngineRuntimeOptions(Map.of());
        }
    }
}
