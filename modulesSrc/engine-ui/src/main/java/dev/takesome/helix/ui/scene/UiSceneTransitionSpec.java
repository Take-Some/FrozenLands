package dev.takesome.helix.ui.scene;


import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import java.util.Locale;

public final class UiSceneTransitionSpec {
    private static final UiSceneTransitionSpec NONE = new UiSceneTransitionSpec(Mode.NONE, 0f, Easing.LINEAR);
    private final Mode mode;
    private final float durationSeconds;
    private final Easing easing;

    private UiSceneTransitionSpec(Mode mode, float durationSeconds, Easing easing) {
        this.mode = mode == null ? Mode.NONE : mode;
        this.durationSeconds = Float.isFinite(durationSeconds) ? Math.max(0f, durationSeconds) : 0f;
        this.easing = easing == null ? Easing.LINEAR : easing;
    }

    public static UiSceneTransitionSpec none() { return NONE; }
    public static UiSceneTransitionSpec crossFade(float seconds) { return of(Mode.CROSS_FADE, seconds, Easing.EASE_OUT); }
    public static UiSceneTransitionSpec of(Mode mode, float seconds, Easing easing) {
        if (mode == null || mode == Mode.NONE || !Float.isFinite(seconds) || seconds <= 0f) return NONE;
        return new UiSceneTransitionSpec(mode, seconds, easing);
    }

    public Mode mode() { return mode; }
    public float durationSeconds() { return durationSeconds; }
    public Easing easing() { return easing; }
    public boolean enabled() { return mode != Mode.NONE && durationSeconds > 0f; }

    public float outgoingOpacity(float elapsedSeconds) {
        return enabled() && mode == Mode.CROSS_FADE ? 1f - progress(elapsedSeconds) : 0f;
    }

    public float incomingOpacity(float elapsedSeconds) {
        return enabled() && mode == Mode.CROSS_FADE ? progress(elapsedSeconds) : 1f;
    }

    private float progress(float elapsedSeconds) {
        return easing.apply(clamp01(elapsedSeconds / durationSeconds));
    }

    public static Mode parseMode(String raw) {
        String value = normalize(raw);
        if (value.isBlank() || value.equals("none") || value.equals("off") || value.equals("false")) return Mode.NONE;
        if (value.contains("cross") || value.contains("fade")) return Mode.CROSS_FADE;
        return Mode.NONE;
    }

    public static Easing parseEasing(String raw) {
        String value = normalize(raw);
        if (value.equals("ease")) return Easing.EASE;
        if (value.equals("ease-in") || value.equals("in")) return Easing.EASE_IN;
        if (value.equals("ease-out") || value.equals("out")) return Easing.EASE_OUT;
        if (value.equals("ease-in-out") || value.equals("in-out")) return Easing.EASE_IN_OUT;
        return Easing.LINEAR;
    }

    private static String normalize(String raw) {
        return lowerTrimToEmpty(raw, Locale.ROOT).replace('_', '-');
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }

    public enum Mode { NONE, CROSS_FADE }

    public enum Easing {
        LINEAR { @Override float apply(float t) { return t; } },
        EASE { @Override float apply(float t) { return t * t * (3f - 2f * t); } },
        EASE_IN { @Override float apply(float t) { return t * t * t; } },
        EASE_OUT { @Override float apply(float t) { float inv = 1f - t; return 1f - inv * inv * inv; } },
        EASE_IN_OUT { @Override float apply(float t) { return t < 0.5f ? 4f * t * t * t : 1f - (float)Math.pow(-2f * t + 2f, 3f) * 0.5f; } };
        abstract float apply(float t);
    }
}
