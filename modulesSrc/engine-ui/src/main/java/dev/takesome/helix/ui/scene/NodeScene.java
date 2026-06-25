package dev.takesome.helix.ui.scene;

import dev.takesome.helix.ui.backend.UiFrameRenderBackend;
import dev.takesome.helix.ui.backend.UiRenderBackend;
import dev.takesome.helix.ui.frame.UiNodeFrameCompiler;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.SceneNode;
import dev.takesome.helix.ui.render.UiRenderContext;

/**
 * Standard retained UI scene implementation backed by a SceneNode root.
 */
public abstract class NodeScene implements Scene {
    private final UiNodeFrameCompiler frameCompiler = new UiNodeFrameCompiler();
    private final UiRenderBackend renderBackend = new UiFrameRenderBackend();

    private SceneNode root;

    @Override
    public final void onEnter() {
        root = buildSceneRoot();

        if (root == null) {
            throw new IllegalStateException("buildSceneRoot() returned null");
        }

        root.attach();
        onSceneEnter(root);
    }

    @Override
    public final void onExit() {
        if (root == null) {
            return;
        }

        SceneNode exiting = root;
        onSceneExit(exiting);
        exiting.detach();
        root = null;
    }

    @Override
    public final void update(float dt) {
        if (root != null) root.update(dt);
    }

    @Override
    public final void render(UiRenderContext ctx) {
        if (root == null) return;
        if (Boolean.getBoolean("helix.ui.frameRender")) {
            renderBackend.render(frameCompiler.compile(root, ctx), ctx);
            return;
        }
        root.render(ctx);
    }

    @Override
    public final boolean handleInput(UiInputEvent event) {
        return root != null && root.handleInput(event);
    }

    protected final SceneNode root() {
        return root;
    }

    public final SceneNode sceneNode() {
        return root;
    }

    protected void onSceneEnter(SceneNode root) {
    }

    protected void onSceneExit(SceneNode root) {
    }

    protected abstract SceneNode buildSceneRoot();
}
