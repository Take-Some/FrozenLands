package org.takesome.frozenlands.engine.weather;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.takesome.frozenlands.engine.weather.snow.SnowPrecipitationSystem;

import java.util.Map;

public record WeatherRuntimeConfig(
        boolean autoStartSnow,
        Vector3f initialWind,
        SnowPrecipitationSystem.Config snowConfig
) {
    public WeatherRuntimeConfig {
        initialWind = initialWind == null ? defaultWind() : initialWind.clone();
        snowConfig = snowConfig == null ? SnowPrecipitationSystem.Config.defaultConfig() : snowConfig;
    }

    @Override
    public Vector3f initialWind() {
        return initialWind.clone();
    }

    public static WeatherRuntimeConfig defaultConfig() {
        return new WeatherRuntimeConfig(
                true,
                defaultWind(),
                SnowPrecipitationSystem.Config.defaultConfig()
        );
    }

    public static WeatherRuntimeConfig load(EngineContext context) {
        try {
            Map<String, Object> root = ModuleIndexCatalog.defaultCatalog()
                    .readConfigMap(WeatherModule.ID, "weather");
            Map<String, Object> weather = WeatherPayloads.map(root.get("weather"));
            boolean autoStartSnow = WeatherPayloads.bool(
                    weather,
                    "autoStartSnow",
                    true
            );
            Vector3f initialWind = WeatherPayloads.vector(
                    WeatherPayloads.map(weather.get("initialWind")),
                    defaultWind()
            );
            SnowPrecipitationSystem.Config snowConfig
                    = SnowPrecipitationSystem.Config.from(
                            WeatherPayloads.map(weather.get("snow")),
                            SnowPrecipitationSystem.Config.defaultConfig()
                    );
            return new WeatherRuntimeConfig(autoStartSnow, initialWind, snowConfig);
        } catch (RuntimeException exception) {
            if (context != null) {
                context.getLogger().warn("Weather config load failed; using defaults", exception);
            }
            return defaultConfig();
        }
    }

    private static Vector3f defaultWind() {
        return new Vector3f(0.75f, 0f, -0.25f);
    }
}
