package dev.takesome.helix.ui.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import dev.takesome.helix.ui.model.UiRect;

final class GdxUiDebugTracer {
    private final ShapeRenderer shapes;

    GdxUiDebugTracer(ShapeRenderer shapes) {
        this.shapes = shapes;
    }

    void draw(Matrix4 projection, UiRect bounds, UiRect contentBounds, int depth, String label) {
        if (shapes == null || bounds == null || bounds.w <= 0f || bounds.h <= 0f) return;
        if (projection != null) shapes.setProjectionMatrix(projection);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Line);
        try {
            setDepthColor(depth, 0.88f);
            shapes.rect(bounds.x, bounds.y, bounds.w, bounds.h);
            shapes.line(bounds.x, bounds.y + bounds.h * 0.5f, bounds.right(), bounds.y + bounds.h * 0.5f);
            shapes.line(bounds.x + bounds.w * 0.5f, bounds.y, bounds.x + bounds.w * 0.5f, bounds.top());

            if (contentBounds != null && different(bounds, contentBounds) && contentBounds.w > 0f && contentBounds.h > 0f) {
                shapes.setColor(0.1f, 1f, 0.45f, 0.78f);
                shapes.rect(contentBounds.x, contentBounds.y, contentBounds.w, contentBounds.h);
            }

            float anchor = Math.max(3f, Math.min(8f, Math.min(bounds.w, bounds.h) * 0.15f));
            shapes.setColor(1f, 0.95f, 0.1f, 0.92f);
            shapes.line(bounds.x - anchor, bounds.y, bounds.x + anchor, bounds.y);
            shapes.line(bounds.x, bounds.y - anchor, bounds.x, bounds.y + anchor);
        } finally {
            shapes.end();
        }
    }

    private void setDepthColor(int depth, float alpha) {
        int d = Math.max(0, depth) % 6;
        switch (d) {
            case 0 -> shapes.setColor(0.20f, 0.85f, 1.00f, alpha);
            case 1 -> shapes.setColor(1.00f, 0.72f, 0.18f, alpha);
            case 2 -> shapes.setColor(0.62f, 1.00f, 0.30f, alpha);
            case 3 -> shapes.setColor(1.00f, 0.32f, 0.72f, alpha);
            case 4 -> shapes.setColor(0.70f, 0.48f, 1.00f, alpha);
            default -> shapes.setColor(1.00f, 1.00f, 1.00f, alpha);
        }
    }

    private boolean different(UiRect a, UiRect b) {
        return Math.abs(a.x - b.x) > 0.5f
                || Math.abs(a.y - b.y) > 0.5f
                || Math.abs(a.w - b.w) > 0.5f
                || Math.abs(a.h - b.h) > 0.5f;
    }
}
