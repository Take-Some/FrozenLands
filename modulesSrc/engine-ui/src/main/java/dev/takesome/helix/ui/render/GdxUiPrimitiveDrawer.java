package dev.takesome.helix.ui.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import dev.takesome.helix.ui.render.GdxUiPainter;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;

final class GdxUiPrimitiveDrawer {
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final GdxUiPainter painter;

    GdxUiPrimitiveDrawer(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, GdxUiPainter painter) {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.painter = painter;
    }

    void fill(Matrix4 projection, UiRect rect, UiColor color) {
        if (rect == null || rect.w <= 0f || rect.h <= 0f) return;
        if (projection != null) shapes.setProjectionMatrix(projection);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        painter.bind(batch, shapes, font);
        try {
            painter.fill(rect.x, rect.y, rect.w, rect.h, color);
        } finally {
            painter.unbind();
            shapes.end();
        }
    }
}
