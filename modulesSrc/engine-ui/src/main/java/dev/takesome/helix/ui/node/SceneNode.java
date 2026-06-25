package dev.takesome.helix.ui.node;

/**
 * Standard root node for retained UI scenes.
 *
 * <p>Every engine UI scene should start from SceneNode. Generic Node remains the
 * primitive tree element, while SceneNode is the canonical root container with
 * scene-space dimensions and resize semantics.</p>
 */
public class SceneNode extends ContainerNode {
    private float sceneWidth;
    private float sceneHeight;

    public SceneNode(float width, float height) {
        resize(width, height);
    }

    public final void resize(float width, float height) {
        float nextWidth = Math.max(1f, width);
        float nextHeight = Math.max(1f, height);

        if (sceneWidth == nextWidth && sceneHeight == nextHeight) {
            return;
        }

        sceneWidth = nextWidth;
        sceneHeight = nextHeight;
        setBounds(0f, 0f, sceneWidth, sceneHeight);
        onSceneResized(sceneWidth, sceneHeight);
    }

    public final float sceneWidth() {
        return sceneWidth;
    }

    public final float sceneHeight() {
        return sceneHeight;
    }

    public final float centerX() {
        return sceneWidth * 0.5f;
    }

    public final float centerY() {
        return sceneHeight * 0.5f;
    }

    protected void onSceneResized(float width, float height) {
    }
}
