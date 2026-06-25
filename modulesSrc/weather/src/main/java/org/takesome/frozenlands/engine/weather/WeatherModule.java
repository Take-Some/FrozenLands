package org.takesome.frozenlands.engine.weather;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WeatherModule implements EngineModule {
    public static final String ID = "engine.weather";

    private final Map<String, ModuleCommand> commands = new LinkedHashMap<>();
    private WeatherService service;
    private EngineContext context;

    public WeatherModule() {
        commands.put("status", ModuleCommand.of("status", "Return current weather runtime status.", this::status));
        commands.put("snow_start", ModuleCommand.of("snow_start", "Start sky snow precipitation.", this::snowStart));
        commands.put("snow_stop", ModuleCommand.of("snow_stop", "Stop sky snow precipitation.", this::snowStop));
        commands.put("snow_light", ModuleCommand.of("snow_light", "Switch to light sky snow.", args -> snowMode("light", true)));
        commands.put("snow_blizzard", ModuleCommand.of("snow_blizzard", "Switch to dense Frostpunk-style blizzard snow.", args -> snowMode("blizzard", true)));
        commands.put("wind", ModuleCommand.of("wind", "Set weather wind vector: {x,y,z}.", this::wind));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Weather runtime module: Frostpunk-style sky snow, wind and precipitation events.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("engine.world", "engine.player");
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public void onRegister(EngineContext context) {
        this.context = context;
        service = new WeatherService(context);
        context.appStateManager().attach(service);
    }

    @Override
    public void onUnregister(EngineContext context) {
        if (service != null) {
            context.appStateManager().detach(service);
        }
        service = null;
        this.context = null;
    }

    public WeatherService service() {
        return service;
    }

    private Map<String, Object> status(Map<String, Object> args) {
        if (service == null) {
            return Map.of("initialized", false, "module", ID);
        }
        return service.status();
    }

    private Map<String, Object> snowStart(Map<String, Object> args) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", "module-command");
        payload.put("mode", string(args, "mode", ""));
        context.getModuleRegistry().publishEvent(WeatherEvents.SNOW_STARTED, payload);
        return status(args);
    }

    private Map<String, Object> snowStop(Map<String, Object> args) {
        context.getModuleRegistry().publishEvent(WeatherEvents.SNOW_STOPPED, Map.of("reason", "module-command"));
        return status(args);
    }

    private Map<String, Object> snowMode(String mode, boolean running) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", "module-command");
        payload.put("mode", mode);
        payload.put("running", running);
        context.getModuleRegistry().publishEvent(WeatherEvents.SNOW_DENSITY_CHANGED, payload);
        return status(Map.of());
    }

    private Map<String, Object> wind(Map<String, Object> args) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", "module-command");
        payload.put("x", number(args, "x", 0f));
        payload.put("y", number(args, "y", 0f));
        payload.put("z", number(args, "z", 0f));
        context.getModuleRegistry().publishEvent(WeatherEvents.WIND_CHANGED, payload);
        return status(args);
    }

    private static String string(Map<String, Object> args, String key, String fallback) {
        Object value = args == null ? null : args.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static float number(Map<String, Object> args, String key, float fallback) {
        Object value = args == null ? null : args.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }
}
