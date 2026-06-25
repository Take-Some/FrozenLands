package org.takesome.frozenlands.engine.world.terrain;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TerrainModule implements EngineModule {
    public static final String ID = "engine.terrain";

    private final TerrainManager terrainManager;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    public TerrainModule(TerrainManager terrainManager) {
        this.terrainManager = terrainManager;
        registerCommands();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Terrain and chunk runtime control";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private void registerCommands() {
        commands.put("status", ModuleCommand.of("status", "Return terrain/chunk status", args -> terrainManager.status()));
        commands.put("chunks", ModuleCommand.of("chunks", "Return terrain chunk snapshots", args -> result("chunks", terrainManager.getChunkTracker().snapshotMaps())));
        commands.put("heightAt", ModuleCommand.of("heightAt", "Return terrain height at x/z", this::heightAt));
        commands.put("spawnLocation", ModuleCommand.of("spawnLocation", "Resolve safe terrain spawn location", this::spawnLocation));
    }

    private Map<String, Object> heightAt(Map<String, Object> args) {
        float x = floatArg(args, "x", 0f);
        float z = floatArg(args, "z", 0f);
        Optional<Float> height = terrainManager.getHeightAt(x, z);
        Map<String, Object> result = result("ready", height.isPresent());
        height.ifPresent(value -> result.put("height", value));
        return result;
    }

    private Map<String, Object> spawnLocation(Map<String, Object> args) {
        float x = floatArg(args, "x", 0f);
        float z = floatArg(args, "z", 0f);
        float clearance = floatArg(args, "clearance", 4f);
        Optional<Vector3f> location = terrainManager.findSafeSpawnLocation(x, z, clearance);
        Map<String, Object> result = result("ready", location.isPresent());
        location.ifPresent(vector -> {
            result.put("x", vector.x);
            result.put("y", vector.y);
            result.put("z", vector.z);
        });
        return result;
    }

    private float floatArg(Map<String, Object> args, String name, float fallback) {
        Object value = args == null ? null : args.get(name);
        return value instanceof Number ? ((Number) value).floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }
}
