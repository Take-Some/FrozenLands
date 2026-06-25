package dev.takesome.helix.ui.components.sprite;

/** Stateful playback controller for a retained UI sprite animation. */
public final class UiSpriteAnimationPlayback {
    private final UiSpriteAnimationSpec spec;
    private float time;
    private boolean playing;
    private int currentFrame;

    public UiSpriteAnimationPlayback(UiSpriteAnimationSpec spec) {
        this.spec = spec == null ? UiSpriteAnimationSpec.of("", 1, 1, 1, 10f) : spec;
        this.currentFrame = this.spec.startFrame();
        this.playing = this.spec.autoplay();
    }

    public UiSpriteAnimationSpec spec() {
        return spec;
    }

    public boolean update(float dt) {
        if (!playing || !spec.animated()) return false;
        if (!Float.isFinite(dt) || dt <= 0f) return false;
        int before = currentFrame;
        time += Math.min(dt, spec.maxDeltaSeconds());
        currentFrame = resolveFrame(time);
        if (spec.mode() == UiSpritePlaybackMode.ONCE && currentFrame == spec.frameCount() - 1) playing = false;
        return before != currentFrame;
    }

    public int currentFrame() {
        return currentFrame;
    }

    public float time() {
        return time;
    }

    public boolean playing() {
        return playing;
    }

    public UiSpriteAnimationPlayback play() {
        playing = true;
        return this;
    }

    public UiSpriteAnimationPlayback pause() {
        playing = false;
        return this;
    }

    public UiSpriteAnimationPlayback stop() {
        playing = false;
        reset();
        return this;
    }

    public UiSpriteAnimationPlayback reset() {
        time = 0f;
        currentFrame = spec.startFrame();
        return this;
    }

    public UiSpriteAnimationPlayback seekSeconds(float seconds) {
        time = Math.max(0f, Float.isFinite(seconds) ? seconds : 0f);
        currentFrame = resolveFrame(time);
        return this;
    }

    public UiSpriteAnimationPlayback seekFrame(int frame) {
        currentFrame = spec.safeFrame(frame);
        time = currentFrame / Math.max(0.01f, spec.fps());
        return this;
    }

    private int resolveFrame(float seconds) {
        int count = spec.frameCount();
        if (count <= 1) return 0;
        int step = Math.max(0, (int)Math.floor(seconds * spec.fps())) + spec.startFrame();
        return switch (spec.mode()) {
            case LOOP -> Math.floorMod(step, count);
            case ONCE -> Math.min(count - 1, step);
            case PING_PONG -> pingPongFrame(step, count);
        };
    }

    private int pingPongFrame(int step, int count) {
        if (count <= 1) return 0;
        int cycle = count * 2 - 2;
        int index = Math.floorMod(step, cycle);
        return index < count ? index : cycle - index;
    }
}
