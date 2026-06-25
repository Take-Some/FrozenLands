package org.takesome.frozenlands.engine.world.effect;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ParticleModule implements EngineModule {
    public static final String ID = "engine.particles";

    private final ParticleManager particleManager;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    public ParticleModule(ParticleManager particleManager) {
        this.particleManager = particleManager;
        registerCommands();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Runtime particle systems and weather effects";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private void registerCommands() {
        commands.put("status", ModuleCommand.of("status", "Return particle system status", args -> particleManager.status()));
        commands.put("snow.enable", ModuleCommand.of("snow.enable", "Enable or disable snow emitter", args -> {
            particleManager.setSnowEnabled(booleanArg(args, "enabled", true));
            return particleManager.status();
        }));
        commands.put("snow.rate", ModuleCommand.of("snow.rate", "Set snow particles per second", args -> {
            particleManager.setSnowRate(floatArg(args, "rate", 450f));
            return particleManager.status();
        }));
        commands.put("emit", ModuleCommand.of("emit", "Emit a transient particle effect", args ->
                particleManager.emit(effectArg(args), position(args))));
        commands.put("impact", ModuleCommand.of("impact", "Emit a transient impact particle effect", args ->
                particleManager.impact(effectArg(args), position(args))));
    }

    private String effectArg(Map<String, Object> args) {
        Object value = args == null ? null : args.get("effect");
        return value == null ? "smoke" : String.valueOf(value);
    }

    private Vector3f position(Map<String, Object> args) {
        return new Vector3f(floatArg(args, "x", 0f), floatArg(args, "y", 0f), floatArg(args, "z", 0f));
    }

    private boolean booleanArg(Map<String, Object> args, String name, boolean fallback) {
        Object value = args == null ? null : args.get(name);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private float floatArg(Map<String, Object> args, String name, float fallback) {
        Object value = args == null ? null : args.get(name);
        return value instanceof Number ? ((Number) value).floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }
}
