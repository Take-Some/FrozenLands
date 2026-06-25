package dev.takesome.helix.ui.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.legacy.render.GdxUiElementRenderer;
import dev.takesome.helix.ui.render.GdxUiPainter;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;

final class GdxUiElementDrawer {
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final GdxUiPainter painter;
    private final GdxUiElementRenderer elementRenderer;

    GdxUiElementDrawer(
            SpriteBatch batch,
            ShapeRenderer shapes,
            BitmapFont font,
            GdxUiPainter painter,
            AssetProvider assets,
            MaterialProvider materials
    ) {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.painter = painter;
        this.elementRenderer = new GdxUiElementRenderer(painter, assets, materials);
    }

    boolean draw(Matrix4 projection, UiElementSkin element, UiRect rect, UiColor tint) {
        if (element == null || rect == null || rect.w <= 0f || rect.h <= 0f) return false;
        if (projection != null) batch.setProjectionMatrix(projection);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.begin();
        painter.bind(batch, shapes, font);
        try {
            return elementRenderer.draw(batch, element, rect, tint);
        } finally {
            painter.unbind();
            batch.end();
        }
    }

    UiRect contentBounds(UiElementSkin element, UiRect rect) {
        if (rect == null) return null;
        UiRect content = elementRenderer.contentBounds(element, rect);
        return content == null ? rect : content;
    }

    void dispose() {
        elementRenderer.dispose();
    }
}
