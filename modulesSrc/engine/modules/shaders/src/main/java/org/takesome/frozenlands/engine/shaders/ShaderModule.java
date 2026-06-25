package org.takesome.frozenlands.engine.shaders;

import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShaderModule implements EngineModule {
    public static final String ID = "engine.shaders";

    private final Shaders shaders;
    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();

    public ShaderModule(Shaders shaders) {
        this.shaders = shaders;
        registerCommands();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Post-processing and shader runtime control";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    private void registerCommands() {
        commands.put("status", ModuleCommand.of("status", "Return shader app-state status", args -> status()));
        commands.put("setEnabled", ModuleCommand.of("setEnabled", "Enable or disable shader app-state", args -> {
            boolean enabled = Boolean.parseBoolean(String.valueOf(args.getOrDefault("enabled", true)));
            shaders.setEnabled(enabled);
            return status();
        }));
        commands.put("shadowSettings", ModuleCommand.of("shadowSettings", "Return current shadow settings", args -> shadowSettings()));
        commands.put("shadowSettings.set", ModuleCommand.of("shadowSettings.set", "Update shadow settings", args -> {
            shaders.applyShadowSettings(args);
            return shadowSettings();
        }));
    }

    private Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("initialized", shaders.isInitialized());
        result.put("enabled", shaders.isEnabled());
        result.put("bloom", shaders.getBloom() != null);
        result.put("dof", shaders.getDof() != null);
        result.put("lightScattering", shaders.getLsf() != null);
        result.put("shadowFilter", shaders.getShadowFilter() != null);
        result.put("shadowSettings", shaders.getShadowSettings().toMap());
        return result;
    }

    private Map<String, Object> shadowSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("settings", shaders.getShadowSettings().toMap());
        return result;
    }
}
