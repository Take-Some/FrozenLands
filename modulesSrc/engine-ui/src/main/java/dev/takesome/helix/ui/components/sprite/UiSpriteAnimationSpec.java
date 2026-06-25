package dev.takesome.helix.ui.components.sprite;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.skin.UiElementSkin;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Immutable sprite-sheet animation descriptor for retained UI nodes. */
public final class UiSpriteAnimationSpec {
    public static final int DEFAULT_COLUMNS = 1;
    public static final int DEFAULT_ROWS = 1;
    public static final int DEFAULT_FRAME_COUNT = 1;
    public static final float DEFAULT_FPS = 10f;
    public static final float DEFAULT_MAX_DELTA_SECONDS = 0.25f;

    private static final Logger LOG = EngineLog.logger(UiSpriteAnimationSpec.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final String source;
    private final int columns;
    private final int rows;
    private final int frameCount;
    private final float fps;
    private final int startFrame;
    private final UiSpritePlaybackMode mode;
    private final boolean autoplay;
    private final float maxDeltaSeconds;

    private UiSpriteAnimationSpec(Builder builder) {
        this.source = cleanSource(builder.source);
        this.columns = positive(builder.columns, DEFAULT_COLUMNS, "columns", source);
        this.rows = positive(builder.rows, DEFAULT_ROWS, "rows", source);
        int maxFrames = Math.max(1, this.columns * this.rows);
        this.frameCount = clamp(positive(builder.frameCount, maxFrames, "frameCount", source), 1, maxFrames);
        this.fps = finitePositive(builder.fps, DEFAULT_FPS, "fps", source);
        this.startFrame = clamp(builder.startFrame, 0, this.frameCount - 1);
        this.mode = builder.mode == null ? UiSpritePlaybackMode.LOOP : builder.mode;
        this.autoplay = builder.autoplay;
        this.maxDeltaSeconds = finitePositive(builder.maxDeltaSeconds, DEFAULT_MAX_DELTA_SECONDS, "maxDeltaSeconds", source);
    }

    public static Builder builder(String source) {
        return new Builder(source);
    }

    public static UiSpriteAnimationSpec of(String source, int columns, int rows, int frameCount, float fps) {
        return builder(source).grid(columns, rows).frameCount(frameCount).fps(fps).build();
    }

    public String source() {
        return source;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public int frameCount() {
        return frameCount;
    }

    public int gridFrameCount() {
        return Math.max(1, columns * rows);
    }

    public float fps() {
        return fps;
    }

    public int startFrame() {
        return startFrame;
    }

    public UiSpritePlaybackMode mode() {
        return mode;
    }

    public boolean autoplay() {
        return autoplay;
    }

    public float maxDeltaSeconds() {
        return maxDeltaSeconds;
    }

    public boolean animated() {
        return frameCount > 1 && fps > 0f && !source.isBlank();
    }

    public String sourceWithLayout() {
        if (source.isBlank()) return "";
        String separator = source.contains("?") ? "&" : "?";
        return source + separator + "cols=" + columns + "&rows=" + rows;
    }

    public UiElementSkin skin(int frame) {
        return UiElementSkin.of("image", sourceWithLayout(), safeFrame(frame));
    }

    public int safeFrame(int frame) {
        return Math.floorMod(frame, frameCount);
    }

    private static String cleanSource(String source) {
        return trimToEmpty(source);
    }

    private static int positive(int value, int fallback, String field, String source) {
        if (value > 0) return value;
        warnOnce("positive:" + field + ':' + source,
                "UI sprite animation invalid {}={} source='{}'; fallback={}", field, value, source, fallback);
        return fallback;
    }

    private static float finitePositive(float value, float fallback, String field, String source) {
        if (Float.isFinite(value) && value > 0f) return value;
        warnOnce("finite-positive:" + field + ':' + source,
                "UI sprite animation invalid {}={} source='{}'; fallback={}", field, value, source, fallback);
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void warnOnce(String key, String message, Object... args) {
        if (WARNED.add(key)) LOG.warn(message, args);
    }

    public static final class Builder {
        private final String source;
        private int columns = DEFAULT_COLUMNS;
        private int rows = DEFAULT_ROWS;
        private int frameCount = DEFAULT_FRAME_COUNT;
        private float fps = DEFAULT_FPS;
        private int startFrame;
        private UiSpritePlaybackMode mode = UiSpritePlaybackMode.LOOP;
        private boolean autoplay = true;
        private float maxDeltaSeconds = DEFAULT_MAX_DELTA_SECONDS;

        private Builder(String source) {
            this.source = source;
        }

        public Builder grid(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
            if (frameCount == DEFAULT_FRAME_COUNT && columns > 0 && rows > 0) this.frameCount = columns * rows;
            return this;
        }

        public Builder frameCount(int frameCount) {
            this.frameCount = frameCount;
            return this;
        }

        public Builder fps(float fps) {
            this.fps = fps;
            return this;
        }

        public Builder startFrame(int startFrame) {
            this.startFrame = startFrame;
            return this;
        }

        public Builder mode(UiSpritePlaybackMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder mode(String mode) {
            this.mode = parseMode(mode, this.mode);
            return this;
        }

        public Builder loop(boolean loop) {
            this.mode = loop ? UiSpritePlaybackMode.LOOP : UiSpritePlaybackMode.ONCE;
            return this;
        }

        public Builder pingPong(boolean pingPong) {
            if (pingPong) this.mode = UiSpritePlaybackMode.PING_PONG;
            return this;
        }

        public Builder autoplay(boolean autoplay) {
            this.autoplay = autoplay;
            return this;
        }

        public Builder maxDeltaSeconds(float maxDeltaSeconds) {
            this.maxDeltaSeconds = maxDeltaSeconds;
            return this;
        }

        public UiSpriteAnimationSpec build() {
            return new UiSpriteAnimationSpec(this);
        }

        private static UiSpritePlaybackMode parseMode(String raw, UiSpritePlaybackMode fallback) {
            if (raw == null || raw.isBlank()) return fallback == null ? UiSpritePlaybackMode.LOOP : fallback;
            String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
            return switch (normalized) {
                case "once", "single", "one_shot", "oneshot" -> UiSpritePlaybackMode.ONCE;
                case "ping_pong", "pingpong", "alternate", "alternate_loop" -> UiSpritePlaybackMode.PING_PONG;
                case "loop", "repeat" -> UiSpritePlaybackMode.LOOP;
                default -> fallback == null ? UiSpritePlaybackMode.LOOP : fallback;
            };
        }
    }
}
