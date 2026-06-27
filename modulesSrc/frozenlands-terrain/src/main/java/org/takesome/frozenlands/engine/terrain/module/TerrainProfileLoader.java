package org.takesome.frozenlands.engine.terrain.module;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.lua.LuaScriptExecutor;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;
import org.takesome.frozenlands.engine.terrain.TerrainService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TerrainProfileLoader {
    private static final String RESOURCE = "scripts/lua/frozenlands/terrain/default.lua";

    private TerrainProfileLoader() {}

    static TerrainService.Profile load(EngineContext context) {
        TerrainService.Profile fallback = TerrainService.Profile.defaults();
        if (context == null) return fallback;
        try (InputStream input = TerrainProfileLoader.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) {
                context.getLogger().warn("Terrain Lua profile not found: {}", RESOURCE);
                return fallback;
            }
            String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> result = new LuaScriptExecutor(context).execute(RESOURCE, Path.of(RESOURCE), source, Map.of());
            Object returned = result.get("return");
            if (!(returned instanceof Map<?, ?> raw)) {
                context.getLogger().warn("Terrain Lua profile returned non-table payload: {}", result);
                return fallback;
            }
            Map<String, Object> profile = normalize(raw);
            return new TerrainService.Profile(
                    RuntimeMaps.string(profile, "id", fallback.id()),
                    RuntimeMaps.integer(profile, "abi", fallback.abi()),
                    RuntimeMaps.integer(profile, "version", fallback.version()),
                    RuntimeMaps.integer(profile, "chunk_size", fallback.chunkSize()),
                    RuntimeMaps.floating(profile, "cell_size", fallback.cellSize()),
                    RuntimeMaps.floating(profile, "visual_snow_scale", fallback.visualSnowScale()),
                    RuntimeMaps.floating(profile, "walkable_snow_scale", fallback.walkableSnowScale()),
                    RuntimeMaps.floating(profile, "max_snow_sink", fallback.maxSnowSink()),
                    mapList(profile.get("height"), "id"),
                    RuntimeMaps.map(profile.get("climate")),
                    mapList(profile.get("biomes"), "id"),
                    mapList(profile.get("features"), "id"),
                    RuntimeMaps.map(profile.get("flags"))
            );
        } catch (RuntimeException | java.io.IOException ex) {
            context.getLogger().warn("Terrain Lua profile load failed; using defaults", ex);
            return fallback;
        }
    }

    private static Map<String, Object> normalize(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static List<Map<String, Object>> mapList(Object value, String idKey) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                Map<String, Object> map = RuntimeMaps.map(item);
                if (!map.isEmpty()) result.add(map);
            }
            return result;
        }
        if (value instanceof Map<?, ?> rawMap) {
            List<Map<String, Object>> result = new ArrayList<>();
            rawMap.forEach((key, item) -> {
                Map<String, Object> map = RuntimeMaps.map(item);
                if (!map.isEmpty()) {
                    map = new LinkedHashMap<>(map);
                    map.putIfAbsent(idKey, String.valueOf(key));
                    result.add(map);
                }
            });
            return result;
        }
        return List.of();
    }
}
