package dev.takesome.helix.ui.components;

import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;

/**
 * Small runtime animation primitive for retained UI overlays.
 *
 * <p>Used by markup modals for controlled dim fade and optional panel motion
 * without replacing the underlying scene.</p>
 */
public final class UiAnimatedPanelNode extends UiPanelNode {
    private final UiColor targetColor;
    private final float fadeDuration;
    private float fadeElapsed;

    private boolean slideEnabled;
    private float slideDuration;
    private float slideElapsed;
    private float slideStartX;
    private float slideStartY;
    private float slideTargetX;
    private float slideTargetY;

    private boolean exitSlideEnabled;
    private float exitSlideDuration;
    private float exitSlideElapsed;
    private float exitSlideStartX;
    private float exitSlideStartY;
    private float exitSlideTargetX;
    private float exitSlideTargetY;
    private Runnable exitSlideFinished;

    private boolean alphaEnabled;
    private float alphaDuration;
    private float alphaElapsed;
    private float alphaStart;
    private float alphaTarget;
    private Runnable alphaFinished;

    public UiAnimatedPanelNode(UiColor targetColor, float fadeDuration) {
        super(safe(targetColor));
        this.targetColor = safe(targetColor);
        this.fadeDuration = Math.max(0f, fadeDuration);
        if (this.fadeDuration > 0f) setOpacity(0f);
    }

    public void slideFrom(float offsetY, float duration) {
        if (duration <= 0f || offsetY == 0f) return;

        UiRect bounds = bounds();
        slideTargetX = bounds.x;
        slideTargetY = bounds.y;
        slideStartX = bounds.x;
        slideStartY = bounds.y + offsetY;
        slideDuration = Math.max(0.001f, duration);
        slideElapsed = 0f;
        slideEnabled = true;
        exitSlideEnabled = false;
        setPosition(slideStartX, slideStartY);
    }

    public void slideOut(float offsetY, float duration) {
        slideOut(offsetY, duration, null);
    }

    public void slideOut(float offsetY, float duration, Runnable onFinished) {
        exitSlideFinished = onFinished;
        if (duration <= 0f || offsetY == 0f) {
            exitSlideEnabled = false;
            completeExitSlide();
            return;
        }

        UiRect bounds = bounds();
        exitSlideStartX = bounds.x;
        exitSlideStartY = bounds.y;
        exitSlideTargetX = bounds.x;
        exitSlideTargetY = bounds.y + offsetY;
        exitSlideDuration = Math.max(0.001f, duration);
        exitSlideElapsed = 0f;
        exitSlideEnabled = true;
        slideEnabled = false;
    }

    public boolean exitSlideActive() {
        return exitSlideEnabled;
    }

    public void fadeAlpha(float from, float to, float duration) {
        fadeAlpha(from, to, duration, null);
    }

    public void fadeAlpha(float from, float to, float duration, Runnable onFinished) {
        float requestedStart = clamp01(from);
        float requestedTarget = clamp01(to);
        float requestedDuration = Math.max(0f, duration);

        if (alphaEnabled && sameAlphaTarget(requestedTarget)) {
            alphaFinished = chain(alphaFinished, onFinished);
            return;
        }

        alphaFinished = onFinished;
        alphaStart = resumableAlphaStart(requestedStart, requestedTarget);
        alphaTarget = requestedTarget;
        alphaElapsed = 0f;
        alphaDuration = requestedDuration;
        setOpacity(alphaStart);
        if (alphaDuration <= 0f || alphaStart == alphaTarget) {
            alphaEnabled = false;
            setOpacity(alphaTarget);
            completeAlpha();
            return;
        }
        alphaEnabled = true;
    }

    public boolean alphaAnimationActive() {
        return alphaEnabled;
    }

    @Override
    protected void onUpdate(float dt) {
        float safeDt = Math.max(0f, dt);
        updateFade(safeDt);
        updateSlide(safeDt);
        updateExitSlide(safeDt);
        updateAlpha(safeDt);
    }

    private void updateFade(float dt) {
        if (fadeDuration <= 0f || alphaEnabled) return;
        fadeElapsed = Math.min(fadeDuration, fadeElapsed + dt);
        float t = easeOutCubic(fadeElapsed / fadeDuration);
        setOpacity(t);
    }

    private void updateSlide(float dt) {
        if (!slideEnabled) return;
        slideElapsed = Math.min(slideDuration, slideElapsed + dt);
        float t = easeOutCubic(slideElapsed / slideDuration);
        float x = lerp(slideStartX, slideTargetX, t);
        float y = lerp(slideStartY, slideTargetY, t);
        setPosition(x, y);
        if (slideElapsed >= slideDuration) slideEnabled = false;
    }

    private void updateExitSlide(float dt) {
        if (!exitSlideEnabled) return;
        exitSlideElapsed = Math.min(exitSlideDuration, exitSlideElapsed + dt);
        float t = easeInCubic(exitSlideElapsed / exitSlideDuration);
        float x = lerp(exitSlideStartX, exitSlideTargetX, t);
        float y = lerp(exitSlideStartY, exitSlideTargetY, t);
        setPosition(x, y);
        if (exitSlideElapsed >= exitSlideDuration) {
            exitSlideEnabled = false;
            completeExitSlide();
        }
    }

    private void updateAlpha(float dt) {
        if (!alphaEnabled) return;
        alphaElapsed = Math.min(alphaDuration, alphaElapsed + dt);
        float t = easeOutCubic(alphaElapsed / alphaDuration);
        float value = lerp(alphaStart, alphaTarget, t);
        setOpacity(value);
        if (alphaElapsed >= alphaDuration) {
            alphaEnabled = false;
            setOpacity(alphaTarget);
            completeAlpha();
        }
    }

    private float resumableAlphaStart(float requestedStart, float requestedTarget) {
        float current = opacity();
        if (alphaEnabled) return current;
        if (requestedTarget > requestedStart && current > requestedStart && current < requestedTarget) return current;
        if (requestedTarget < requestedStart && current < requestedStart && current > requestedTarget) return current;
        return requestedStart;
    }

    private boolean sameAlphaTarget(float requestedTarget) {
        return Math.abs(alphaTarget - requestedTarget) <= 0.0001f;
    }

    private static Runnable chain(Runnable first, Runnable second) {
        if (first == null) return second;
        if (second == null) return first;
        return () -> {
            first.run();
            second.run();
        };
    }

    private void completeExitSlide() {
        Runnable finished = exitSlideFinished;
        exitSlideFinished = null;
        if (finished != null) finished.run();
    }

    private void completeAlpha() {
        Runnable finished = alphaFinished;
        alphaFinished = null;
        if (finished != null) finished.run();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    private static float easeOutCubic(float value) {
        float t = 1f - clamp01(value);
        return 1f - t * t * t;
    }

    private static float easeInCubic(float value) {
        float t = clamp01(value);
        return t * t * t;
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private static UiColor safe(UiColor color) {
        return color == null ? UiColor.TRANSPARENT : color;
    }

    private static UiColor alpha(UiColor color, float alpha) {
        UiColor safe = safe(color);
        return new UiColor(safe.r, safe.g, safe.b, Math.max(0f, Math.min(1f, alpha)));
    }
}
