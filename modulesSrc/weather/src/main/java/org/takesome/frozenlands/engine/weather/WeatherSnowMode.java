package org.takesome.frozenlands.engine.weather;

import org.takesome.frozenlands.engine.weather.snow.SnowPrecipitationSystem;

import java.util.Locale;

public enum WeatherSnowMode {
    LIGHT,
    DEFAULT,
    BLIZZARD;

    public SnowPrecipitationSystem.Config config(SnowPrecipitationSystem.Config current) {
        SnowPrecipitationSystem.Config target = switch (this) {
            case LIGHT -> SnowPrecipitationSystem.Config.light();
            case DEFAULT -> SnowPrecipitationSystem.Config.defaultConfig();
            case BLIZZARD -> SnowPrecipitationSystem.Config.blizzard();
        };
        if (current != null && current.texture() != null && !current.texture().isBlank()) {
            return target.withTexture(current.texture());
        }
        return target;
    }

    public static WeatherSnowMode from(String mode) {
        if (mode == null || mode.isBlank()) {
            return null;
        }
        return switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "light" -> LIGHT;
            case "blizzard", "storm" -> BLIZZARD;
            case "default", "normal", "snow" -> DEFAULT;
            default -> null;
        };
    }
}
