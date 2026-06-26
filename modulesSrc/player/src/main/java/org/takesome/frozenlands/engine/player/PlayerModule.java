package org.takesome.frozenlands.engine.player;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerModule implements EngineModule {
    public static final String ID = "engine.player";

    private final EngineContext context;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    public PlayerModule(EngineContext context) {
        this.context = context;
        registerCommands();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Player runtime control";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private void registerCommands() {
        commands.put("status", ModuleCommand.of("status", "Return player spawn and control status", args -> status()));
        commands.put("position", ModuleCommand.of("position", "Return player position", args -> position()));
        commands.put("warp", ModuleCommand.of("warp", "Warp player to coordinates", this::warp));
    }

    private Map<String, Object> status() {
        Map<String, Object> result = result("spawned", player() != null);
        if (player() != null) {
            result.put("hasCharacterControl", characterControl() != null);
            result.putAll(position());
        }
        return result;
    }

    private Map<String, Object> position() {
        Player player = player();
        if (player == null) {
            return result("spawned", false);
        }
        Vector3f position = player.getPlayerPosition();
        Map<String, Object> result = result("spawned", true);
        result.put("x", position.x);
        result.put("y", position.y);
        result.put("z", position.z);
        return result;
    }

    private Map<String, Object> warp(Map<String, Object> args) {
        BetterCharacterControl control = characterControl();
        if (control == null) {
            return result("warped", false);
        }
        Vector3f target = new Vector3f(floatArg(args, "x", 0f), floatArg(args, "y", 0f), floatArg(args, "z", 0f));
        control.warp(target);
        context.getModuleRegistry().publishEvent("player.warped", Map.of("x", target.x, "y", target.y, "z", target.z));
        Map<String, Object> result = result("warped", true);
        result.put("x", target.x);
        result.put("y", target.y);
        result.put("z", target.z);
        return result;
    }

    private BetterCharacterControl characterControl() {
        Player player = player();
        return player == null ? null : player.getPlayerOptions().getCharacterControl();
    }

    private Player player() {
        return context.findService(Player.class).orElse(null);
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
