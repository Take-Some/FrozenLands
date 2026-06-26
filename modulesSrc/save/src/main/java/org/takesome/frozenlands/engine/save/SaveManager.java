package org.takesome.frozenlands.engine.save;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.takesome.frozenlands.engine.player.Player;

public final class SaveManager {
    private static final String SAVE_VERSION = "frozenlands.save.v1";

    private final EngineContext context;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path saveDir;

    public SaveManager(EngineContext context) {
        this(context, Path.of("saves"));
    }

    public SaveManager(EngineContext context, Path saveDir) {
        this.context = context;
        this.saveDir = saveDir;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("version", SAVE_VERSION);
        snapshot.put("savedAt", Instant.now().toString());
        snapshot.put("player", playerSnapshot());
        snapshot.put("providers", context.getProviderRegistry().luaManifest());
        snapshot.put("modules", context.getModuleRegistry().luaManifest());
        snapshot.put("providerEvents", context.getProviderRegistry().getEventBus().snapshot());
        snapshot.put("moduleEvents", context.getModuleRegistry().getEventBus().snapshot());
        return snapshot;
    }

    public Map<String, Object> save(String slot) {
        try {
            Files.createDirectories(saveDir);
            Path path = pathFor(slot);
            Map<String, Object> snapshot = snapshot();
            mapper.writeValue(path.toFile(), snapshot);
            context.getModuleRegistry().publishEvent("save.written", Map.of("slot", slot, "path", path.toString()));
            return result("path", path.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write save slot: " + slot, e);
        }
    }

    public Map<String, Object> load(String slot) {
        Path path = pathFor(slot);
        try {
            Map<String, Object> save = mapper.readValue(path.toFile(), new TypeReference<>() { });
            restorePlayer(save);
            context.getModuleRegistry().publishEvent("save.loaded", Map.of("slot", slot, "path", path.toString()));
            Map<String, Object> result = result("slot", slot);
            result.put("path", path.toString());
            result.put("save", save);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load save slot: " + slot, e);
        }
    }

    public List<Map<String, Object>> list() {
        if (!Files.isDirectory(saveDir)) {
            return List.of();
        }
        try {
            return Files.list(saveDir)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::saveInfo)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list saves", e);
        }
    }

    private Map<String, Object> playerSnapshot() {
        Map<String, Object> player = new LinkedHashMap<>();
        Player playerRuntime = context.findService(Player.class).orElse(null);
        player.put("spawned", playerRuntime != null);
        if (playerRuntime != null) {
            Vector3f position = playerRuntime.getPlayerPosition();
            player.put("x", position.x);
            player.put("y", position.y);
            player.put("z", position.z);
        }
        return player;
    }

    private void restorePlayer(Map<String, Object> save) {
        Object playerObject = save.get("player");
        if (!(playerObject instanceof Map<?, ?> playerMap)) {
            return;
        }
        boolean spawned = Boolean.parseBoolean(String.valueOf(playerMap.get("spawned")));
        if (!spawned) {
            return;
        }
        context.getModuleRegistry().call("engine.world", "spawnPlayer", Map.of());
        context.getModuleRegistry().call("engine.player", "warp", Map.of(
                "x", number(playerMap.get("x")),
                "y", number(playerMap.get("y")),
                "z", number(playerMap.get("z"))
        ));
    }

    private Map<String, Object> saveInfo(Path path) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("slot", path.getFileName().toString().replaceFirst("\\.json$", ""));
        info.put("path", path.toString());
        try {
            info.put("modifiedAt", Files.getLastModifiedTime(path).toInstant().toString());
            info.put("sizeBytes", Files.size(path));
        } catch (IOException e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    private Path pathFor(String slot) {
        return saveDir.resolve(sanitizeSlot(slot) + ".json");
    }

    private String sanitizeSlot(String slot) {
        String value = slot == null || slot.isBlank() ? "quick" : slot;
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }
}
