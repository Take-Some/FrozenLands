package org.takesome.frozenlands.engine.weather;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.weather.snow.SnowPrecipitationSystem;

import java.util.LinkedHashMap;
import java.util.Map;

public record WeatherEnvironmentState(
        boolean initialized,
        boolean autoStartSnow,
        boolean snowRunning,
        int flakes,
        Vector3f wind,
        Vector3f anchor,
        SnowPrecipitationSystem.Config snowConfig
) {
    public WeatherEnvironmentState {
        wind = wind == null ? new Vector3f() : wind.clone();
        anchor = anchor == null ? new Vector3f() : anchor.clone();
    }

    @Override
    public Vector3f wind() {
        return wind.clone();
    }

    @Override
    public Vector3f anchor() {
        return anchor.clone();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("initialized", initialized);
        status.put("autoStartSnow", autoStartSnow);
        status.put("snowRunning", snowRunning);
        status.put("flakes", flakes);
        status.put("wind", WeatherPayloads.vectorMap(wind));
        status.put("anchor", WeatherPayloads.vectorMap(anchor));
        if (snowConfig != null) {
            status.put("snowConfig", configMap(snowConfig));
        }
        return status;
    }

    public static WeatherEnvironmentState from(
            boolean initialized,
            boolean autoStartSnow,
            SnowPrecipitationSystem snow,
            Vector3f initialWind,
            Vector3f anchor
    ) {
        return new WeatherEnvironmentState(
                initialized,
                autoStartSnow,
                snow != null && snow.running(),
                snow == null ? 0 : snow.flakeCount(),
                snow == null ? initialWind : snow.wind(),
                anchor,
                snow == null ? null : snow.config()
        );
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
        result.put("texture", config.texture());
        return result;
    }
}
