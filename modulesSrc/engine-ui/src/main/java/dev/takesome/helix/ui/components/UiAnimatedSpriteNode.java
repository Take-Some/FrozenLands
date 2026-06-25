package dev.takesome.helix.ui.components;

import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.components.sprite.UiSpriteAnimationPlayback;
import dev.takesome.helix.ui.components.sprite.UiSpriteAnimationSpec;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Retained UI node for animated sprite-sheet backgrounds/elements. */
public final class UiAnimatedSpriteNode extends UiComponent {
    private final UiSpriteAnimationPlayback playback;
    private UiElementSkin cachedSkin;
    private int cachedFrame = Integer.MIN_VALUE;

    public UiAnimatedSpriteNode(String source, int columns, int rows, int frameCount, float fps) {
        this(UiSpriteAnimationSpec.of(source, columns, rows, frameCount, fps));
    }

    public UiAnimatedSpriteNode(UiSpriteAnimationSpec spec) {
        this.playback = new UiSpriteAnimationPlayback(spec);
    }

    public static UiAnimatedSpriteNode looping(String source, int columns, int rows, int frameCount, float fps) {
        return new UiAnimatedSpriteNode(UiSpriteAnimationSpec.builder(source)
                .grid(columns, rows)
                .frameCount(frameCount)
                .fps(fps)
                .build());
    }

    public UiSpriteAnimationSpec spec() {
        return playback.spec();
    }

    public UiSpriteAnimationPlayback playback() {
        return playback;
    }

    public int currentFrame() {
        return playback.currentFrame();
    }

    public boolean playing() {
        return playback.playing();
    }

    public UiAnimatedSpriteNode play() {
        playback.play();
        markDirty();
        return this;
    }

    public UiAnimatedSpriteNode pause() {
        playback.pause();
        markDirty();
        return this;
    }

    public UiAnimatedSpriteNode stop() {
        playback.stop();
        invalidateSkin();
        return this;
    }

    public UiAnimatedSpriteNode resetAnimation() {
        playback.reset();
        invalidateSkin();
        return this;
    }

    public UiAnimatedSpriteNode seekSeconds(float seconds) {
        playback.seekSeconds(seconds);
        invalidateSkin();
        return this;
    }

    public UiAnimatedSpriteNode seekFrame(int frame) {
        playback.seekFrame(frame);
        invalidateSkin();
        return this;
    }

    @Override
    protected void onUpdate(float dt) {
        if (playback.update(dt)) invalidateSkin();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        if (ctx == null || spec().source().isBlank()) return;
        ctx.drawElement(skinForCurrentFrame(), absoluteBounds());
    }

    @Override
    protected String debugLabel() {
        return "UiAnimatedSpriteNode(" + spec().source() + ", frame=" + currentFrame() + ')';
    }

    private UiElementSkin skinForCurrentFrame() {
        int frame = playback.currentFrame();
        if (cachedSkin == null || cachedFrame != frame) {
            cachedFrame = frame;
            cachedSkin = spec().skin(frame);
        }
        return cachedSkin;
    }

    private void invalidateSkin() {
        cachedFrame = Integer.MIN_VALUE;
        cachedSkin = null;
        markDirty();
    }
}
