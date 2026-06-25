package dev.takesome.helix.ui.scene;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.render.UiRenderContext;

/**
 * Owns the active UI scene and provides one routing seam for update, render,
 * input and generic scene transitions.
 */
public final class SceneManager {
    private Scene activeScene;

    private Scene pendingScene;
    private UiSceneTransitionSpec pendingTransition = UiSceneTransitionSpec.none();
    private boolean hasPendingScene;
    private boolean dispatching;

    private UiSceneTransitionSpec defaultTransition = UiSceneTransitionSpec.none();
    private Scene outgoingScene;
    private UiSceneTransitionSpec activeTransition = UiSceneTransitionSpec.none();
    private float transitionElapsed;

    public void setDefaultTransition(UiSceneTransitionSpec transition) {
        defaultTransition = normalize(transition);
    }

    public UiSceneTransitionSpec defaultTransition() {
        return defaultTransition;
    }

    public void setScene(Scene nextScene) {
        setScene(nextScene, defaultTransition);
    }

    public void setScene(Scene nextScene, UiSceneTransitionSpec transition) {
        UiSceneTransitionSpec requested = normalize(transition);
        if (dispatching) {
            pendingScene = nextScene;
            pendingTransition = requested;
            hasPendingScene = true;
            return;
        }

        switchScene(nextScene, requested);
    }

    public Scene activeScene() {
        return activeScene;
    }

    public boolean transitioning() {
        return outgoingScene != null && activeTransition.enabled();
    }

    public void clear() {
        setScene(null, UiSceneTransitionSpec.none());
    }

    public void update(float dt) {
        drainPendingScene();

        if (activeScene == null && outgoingScene == null) return;

        dispatching = true;
        try {
            float step = sanitizeDelta(dt);
            if (transitioning() && outgoingScene != null) {
                outgoingScene.update(step);
            }
            if (activeScene != null) {
                activeScene.update(step);
            }
            advanceTransition(step);
        } finally {
            dispatching = false;
            drainPendingScene();
        }
    }

    public void render(UiRenderContext ctx) {
        drainPendingScene();

        if (activeScene == null) return;

        dispatching = true;
        try {
            if (transitioning() && outgoingScene != null) {
                renderScene(outgoingScene, ctx, activeTransition.outgoingOpacity(transitionElapsed));
                renderScene(activeScene, ctx, activeTransition.incomingOpacity(transitionElapsed));
            } else {
                activeScene.render(ctx);
            }
        } finally {
            dispatching = false;
            drainPendingScene();
        }
    }

    public boolean handleInput(UiInputEvent event) {
        drainPendingScene();

        if (activeScene == null || transitioning()) return false;

        dispatching = true;
        try {
            return activeScene.handleInput(event);
        } finally {
            dispatching = false;
            drainPendingScene();
        }
    }

    private void drainPendingScene() {
        if (!hasPendingScene || dispatching) return;

        Scene next = pendingScene;
        UiSceneTransitionSpec transition = pendingTransition;
        pendingScene = null;
        pendingTransition = UiSceneTransitionSpec.none();
        hasPendingScene = false;
        switchScene(next, transition);
    }

    private void switchScene(Scene nextScene, UiSceneTransitionSpec transition) {
        if (activeScene == nextScene && outgoingScene == null) return;

        if (nextScene == null) {
            activateImmediately(null);
            return;
        }

        UiSceneTransitionSpec requested = normalize(transition);
        if (activeScene == null || !requested.enabled()) {
            activateImmediately(nextScene);
            return;
        }

        if (activeScene == nextScene) return;
        beginTransition(nextScene, requested);
    }

    private void activateImmediately(Scene nextScene) {
        disposeOutgoingScene();

        if (activeScene == nextScene) return;

        if (activeScene != null) {
            activeScene.onExit();
        }

        activeScene = nextScene;

        if (activeScene != null) {
            activeScene.onEnter();
        }
    }

    private void beginTransition(Scene nextScene, UiSceneTransitionSpec transition) {
        disposeOutgoingScene();

        Scene previous = activeScene;
        nextScene.onEnter();

        outgoingScene = previous;
        activeScene = nextScene;
        activeTransition = normalize(transition);
        transitionElapsed = 0f;
    }

    private void advanceTransition(float dt) {
        if (!transitioning()) return;

        transitionElapsed += dt;
        if (transitionElapsed + 0.0001f >= activeTransition.durationSeconds()) {
            finishTransition();
        }
    }

    private void finishTransition() {
        Scene exiting = outgoingScene;
        outgoingScene = null;
        activeTransition = UiSceneTransitionSpec.none();
        transitionElapsed = 0f;

        if (exiting != null && exiting != activeScene) {
            exiting.onExit();
        }
    }

    private void disposeOutgoingScene() {
        Scene exiting = outgoingScene;
        outgoingScene = null;
        activeTransition = UiSceneTransitionSpec.none();
        transitionElapsed = 0f;

        if (exiting != null && exiting != activeScene) {
            exiting.onExit();
        }
    }

    private void renderScene(Scene scene, UiRenderContext ctx, float opacity) {
        if (scene == null || opacity <= 0.001f) return;

        if (opacity >= 0.999f) {
            scene.render(ctx);
            return;
        }

        boolean pushed = ctx.pushOpacity(opacity);
        try {
            scene.render(ctx);
        } finally {
            if (pushed) ctx.popOpacity();
        }
    }

    private static UiSceneTransitionSpec normalize(UiSceneTransitionSpec transition) {
        return transition == null ? UiSceneTransitionSpec.none() : transition;
    }

    private static float sanitizeDelta(float dt) {
        return Float.isFinite(dt) ? Math.max(0f, dt) : 0f;
    }
}
