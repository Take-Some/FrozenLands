package org.takesome.frozenlands.engine.weather;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.weather.snow.SnowPrecipitationSystem;

import java.util.Map;

public final class WeatherService extends BaseAppState {
    private final EngineContext context;
    private final Node weatherRoot = new Node("weather.root");
    private final Vector3f anchor = new Vector3f();
    private final Vector3f initialWind = new Vector3f();
    private final WeatherRuntimeConfig runtimeConfig;

    private SnowPrecipitationSystem snow;
    private AutoCloseable snowStartedSubscription;
    private AutoCloseable snowStoppedSubscription;
    private AutoCloseable snowDensitySubscription;
    private AutoCloseable windChangedSubscription;
    private boolean initialized;

    public WeatherService(EngineContext context) {
        this.context = context;
        this.runtimeConfig = WeatherRuntimeConfig.load(context);
        this.initialWind.set(runtimeConfig.initialWind());
    }

    @Override
    protected void initialize(Application app) {
        if (weatherRoot.getParent() == null) {
            context.getRootNode().attachChild(weatherRoot);
        }
        snow = new SnowPrecipitationSystem(context.getAssetManager(), runtimeConfig.snowConfig());
        snow.setWind(initialWind);
        weatherRoot.attachChild(snow.root());
        subscribeEvents();
        initialized = true;
        if (runtimeConfig.autoStartSnow()) {
            snow.start();
        }
        publishWeatherState(runtimeConfig.autoStartSnow() ? "initialized-auto-snow" : "initialized");
    }

    @Override
    public void update(float tpf) {
        if (snow == null) {
            return;
        }
        resolveAnchor();
        snow.update(tpf, anchor, context.getCamera());
    }

    public WeatherEnvironmentState snapshot() {
        return WeatherEnvironmentState.from(
                initialized,
                runtimeConfig.autoStartSnow(),
                snow,
                initialWind,
                anchor
        );
    }

    public Map<String, Object> status() {
        return snapshot().toMap();
    }

    private void resolveAnchor() {
        Player player = context.findService(Player.class).orElse(null);
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
        Map<String, Object> payload = WeatherPayloads.payload(event);
        applySnowMode(WeatherPayloads.string(payload, "mode", ""));
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
        Map<String, Object> payload = WeatherPayloads.payload(event);
        String mode = WeatherPayloads.string(payload, "mode", "");
        if (!mode.isBlank()) {
            applySnowMode(mode);
        } else {
            snow.setConfig(SnowPrecipitationSystem.Config.from(payload, snow.config()));
        }
        if (WeatherPayloads.bool(payload, "running", snow.running())) {
            snow.start();
        }
        publishWeatherState("snow-density-changed");
    }

    private void onWindChanged(Map<String, Object> event) {
        Map<String, Object> payload = WeatherPayloads.payload(event);
        Vector3f nextWind = WeatherPayloads.vector(payload, Vector3f.ZERO);
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
        WeatherSnowMode snowMode = WeatherSnowMode.from(mode);
        if (snowMode == null) {
            context.getLogger().warn("Unknown weather snow mode: {}", mode);
            return;
        }
        snow.setConfig(snowMode.config(snow.config()));
    }

    private void publishWeatherState(String reason) {
        Map<String, Object> payload = snapshot().toMap();
        payload.put("reason", reason);
        context.getModuleRegistry().publishEvent(WeatherEvents.WEATHER_STATE_CHANGED, payload);
        context.getModuleRegistry().publishEvent(WeatherEvents.WEATHER_ENVIRONMENT_CHANGED, payload);
    }

    @Override
    protected void cleanup(Application app) {
        close(snowStartedSubscription);
        close(snowStoppedSubscription);
        close(snowDensitySubscription);
        close(windChangedSubscription);
        weatherRoot.removeFromParent();
        snow = null;
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

}
