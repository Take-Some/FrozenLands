package org.takesome.frozenlands.engine.weather;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.takesome.frozenlands.engine.weather.snow.SnowPrecipitationSystem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WeatherService extends BaseAppState {
    private final EngineContext context;
    private final Node weatherRoot = new Node("weather.root");
    private final Vector3f anchor = new Vector3f();
    private final Vector3f initialWind = new Vector3f();

    private SnowPrecipitationSystem snow;
    private SnowPrecipitationSystem.Config initialSnowConfig = SnowPrecipitationSystem.Config.defaultConfig();
    private AutoCloseable snowStartedSubscription;
    private AutoCloseable snowStoppedSubscription;
    private AutoCloseable snowDensitySubscription;
    private AutoCloseable windChangedSubscription;
    private boolean autoStartSnow = true;
    private boolean initialized;

    public WeatherService(EngineContext context) {
        this.context = context;
        loadOptions();
    }

    @Override
    protected void initialize(Application app) {
        if (weatherRoot.getParent() == null) {
            context.getRootNode().attachChild(weatherRoot);
        }
        snow = new SnowPrecipitationSystem(context.getAssetManager());
        snow.setConfig(initialSnowConfig);
        snow.setWind(initialWind);
        weatherRoot.attachChild(snow.root());
        subscribeEvents();
        initialized = true;
        if (autoStartSnow) {
            snow.start();
        }
        publishWeatherState(autoStartSnow ? "initialized-auto-snow" : "initialized");
    }

    @Override
    public void update(float tpf) {
        if (snow == null) {
            return;
        }
        resolveAnchor();
        snow.update(tpf, anchor, context.getCamera());
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("initialized", initialized);
        status.put("autoStartSnow", autoStartSnow);
        status.put("snowRunning", snow != null && snow.running());
        status.put("flakes", snow == null ? 0 : snow.flakeCount());
        status.put("wind", vectorMap(snow == null ? initialWind : snow.wind()));
        status.put("anchor", vectorMap(anchor));
        if (snow != null) {
            status.put("snowConfig", configMap(snow.config()));
        }
        return status;
    }

    private void resolveAnchor() {
        Player player = context.getPlayer();
        if (player != null) {
            anchor.set(player.getWorldTranslation());
            return;
        }
        if (context.getCamera() != null) {
            anchor.set(context.getCamera().getLocation());
        }
    }

    private void subscribeEvents() {
        snowStartedSubscription = context.getModuleRegistry().getEventBus().subscribe(WeatherEvents.SNOW_STARTED, this::onSnowStarted, true);
        snowStoppedSubscription = context.getModuleRegistry().getEventBus().subscribe(WeatherEvents.SNOW_STOPPED, this::onSnowStopped, true);
        snowDensitySubscription = context.getModuleRegistry().getEventBus().subscribe(WeatherEvents.SNOW_DENSITY_CHANGED, this::onSnowDensityChanged, true);
        windChangedSubscription = context.getModuleRegistry().getEventBus().subscribe(WeatherEvents.WIND_CHANGED, this::onWindChanged, true);
    }

    private void onSnowStarted(Map<String, Object> event) {
        if (snow == null) {
            return;
        }
        Map<String, Object> payload = payload(event);
        applySnowMode(string(payload, "mode", ""));
        snow.start();
        publishWeatherState("snow-started");
    }

    private void onSnowStopped(Map<String, Object> event) {
        if (snow == null) {
            return;
        }
        snow.stop();
        publishWeatherState("snow-stopped");
    }

    private void onSnowDensityChanged(Map<String, Object> event) {
        if (snow == null) {
            return;
        }
        Map<String, Object> payload = payload(event);
        String mode = string(payload, "mode", "");
        if (!mode.isBlank()) {
            applySnowMode(mode);
        } else {
            snow.setConfig(SnowPrecipitationSystem.Config.from(payload, snow.config()));
        }
        if (bool(payload, "running", snow.running())) {
            snow.start();
        }
        publishWeatherState("snow-density-changed");
    }

    private void onWindChanged(Map<String, Object> event) {
        Map<String, Object> payload = payload(event);
        Vector3f nextWind = new Vector3f(
                number(payload, "x", 0f),
                number(payload, "y", 0f),
                number(payload, "z", 0f)
        );
        initialWind.set(nextWind);
        if (snow != null) {
            snow.setWind(nextWind);
        }
        publishWeatherState("wind-changed");
    }

    private void applySnowMode(String mode) {
        if (snow == null || mode == null || mode.isBlank()) {
            return;
        }
        switch (mode.trim().toLowerCase()) {
            case "light" -> snow.setConfig(SnowPrecipitationSystem.Config.light());
            case "blizzard", "storm" -> snow.setConfig(SnowPrecipitationSystem.Config.blizzard());
            case "default", "normal", "snow" -> snow.setConfig(SnowPrecipitationSystem.Config.defaultConfig());
            default -> context.getLogger().warn("Unknown weather snow mode: {}", mode);
        }
    }

    private void publishWeatherState(String reason) {
        Map<String, Object> payload = status();
        payload.put("reason", reason);
        context.getModuleRegistry().publishEvent(WeatherEvents.WEATHER_STATE_CHANGED, payload);
    }

    private void loadOptions() {
        try {
            Map<String, Object> root = ModuleIndexCatalog.defaultCatalog().readConfigMap(WeatherModule.ID, "weather");
            Map<String, Object> weather = map(root.get("weather"));
            autoStartSnow = bool(weather, "autoStartSnow", true);
            initialWind.set(vector(map(weather.get("initialWind")), new Vector3f(0.75f, 0f, -0.25f)));
            initialSnowConfig = SnowPrecipitationSystem.Config.from(map(weather.get("snow")), SnowPrecipitationSystem.Config.defaultConfig());
        } catch (RuntimeException e) {
            autoStartSnow = true;
            initialWind.set(0.75f, 0f, -0.25f);
            initialSnowConfig = SnowPrecipitationSystem.Config.defaultConfig();
            context.getLogger().warn("Weather config load failed; using defaults", e);
        }
    }

    @Override
    protected void cleanup(Application app) {
        close(snowStartedSubscription);
        close(snowStoppedSubscription);
        close(snowDensitySubscription);
        close(windChangedSubscription);
        weatherRoot.removeFromParent();
        initialized = false;
    }

    @Override
    protected void onEnable() {
        weatherRoot.setCullHint(Spatial.CullHint.Inherit);
    }

    @Override
    protected void onDisable() {
        weatherRoot.setCullHint(Spatial.CullHint.Always);
    }

    private void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, val) -> result.put(String.valueOf(key), val));
        return result;
    }

    private static Vector3f vector(Map<String, Object> payload, Vector3f fallback) {
        return new Vector3f(
                number(payload, "x", fallback.x),
                number(payload, "y", fallback.y),
                number(payload, "z", fallback.z)
        );
    }

    private static Map<String, Object> vectorMap(Vector3f vector) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("x", vector.x);
        result.put("y", vector.y);
        result.put("z", vector.z);
        return result;
    }

    private static Map<String, Object> configMap(SnowPrecipitationSystem.Config config) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxFlakes", config.maxFlakes());
        result.put("spawnWidth", config.spawnWidth());
        result.put("spawnDepth", config.spawnDepth());
        result.put("spawnMinHeight", config.spawnMinHeight());
        result.put("spawnMaxHeight", config.spawnMaxHeight());
        result.put("killBelowHeight", config.killBelowHeight());
        result.put("minFallSpeed", config.minFallSpeed());
        result.put("maxFallSpeed", config.maxFallSpeed());
        result.put("drift", config.drift());
        result.put("minSize", config.minSize());
        result.put("maxSize", config.maxSize());
        result.put("minAlpha", config.minAlpha());
        result.put("maxAlpha", config.maxAlpha());
        result.put("minLife", config.minLife());
        result.put("maxLife", config.maxLife());
        result.put("maxAngularVelocity", config.maxAngularVelocity());
        return result;
    }

    private static String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private static float number(Map<String, Object> payload, String key, float fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }
}
