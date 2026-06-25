package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.math.MathUtils;

final class UiProgressState {
    private static final float RISE_SPEED = 10f;
    private static final float FALL_SPEED = 7f;
    private static final float GHOST_SPEED = 3.5f;
    private static final float PULSE_DURATION = 0.22f;

    float display;
    float ghost;
    float pulse;
    private float lastTime;

    UiProgressState(float value, float time) {
        display = value;
        ghost = value;
        lastTime = time;
    }

    UiProgressState update(float target, float now) {
        float dt = MathUtils.clamp(now - lastTime, 0f, 0.25f);
        lastTime = now;
        float oldDisplay = display;
        float speed = target >= display ? RISE_SPEED : FALL_SPEED;
        display = approach(display, target, speed * dt);
        if (target >= oldDisplay + 0.001f) pulse = PULSE_DURATION;
        pulse = Math.max(0f, pulse - dt);
        if (display < ghost) ghost = approach(ghost, display, GHOST_SPEED * dt);
        else ghost = display;
        return this;
    }

    private static float approach(float value, float target, float step) {
        if (step <= 0f) return value;
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }
}
