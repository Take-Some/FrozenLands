package dev.takesome.helix.ui.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import dev.takesome.helix.fonts.render.FontGlyphRenderer;
import dev.takesome.helix.fonts.render.FontRenderRequest;
import dev.takesome.helix.fonts.render.FontScalePolicy;
import dev.takesome.helix.fonts.render.FontTextAlign;
import dev.takesome.helix.fonts.gdx.render.GdxFontGlyphRenderer;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;

/**
 * LibGDX adapter for the helix UI painter contract.
 *
 * <p>Games keep their own HUD composition, but primitive drawing is centralized:
 * texture regions, filled rectangles and text all go through the same API.</p>
 */
public final class GdxUiPainter implements UiPainter<TextureRegion> {
    private final Color scratchColor = new Color();
    private final FontGlyphRenderer glyphs = new GdxFontGlyphRenderer();
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    public void bind(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
    }

    public void unbind() {
        this.batch = null;
        this.shapes = null;
        this.font = null;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float w, float h) {
        if (batch == null) throw new IllegalStateException("SpriteBatch is not bound");
        if (region == null || w <= 0f || h <= 0f) return;
        batch.draw(region, x, y, w, h);
    }

    @Override
    public void fill(float x, float y, float w, float h, UiColor color) {
        if (shapes == null) throw new IllegalStateException("ShapeRenderer is not bound");
        if (w <= 0f || h <= 0f) return;
        shapes.setColor(toGdx(color));
        shapes.rect(x, y, w, h);
    }

    @Override
    public void text(String text, float x, float y, float scale, UiColor color, TextAlign align) {
        if (font == null || batch == null) throw new IllegalStateException("BitmapFont/SpriteBatch is not bound");
        if (text == null || text.isEmpty()) return;
        glyphs.render(
                batch,
                null,
                FontRenderRequest.baseline(
                        font,
                        text,
                        x,
                        y,
                        scale,
                        toGdx(color),
                        toFontAlign(align),
                        false,
                        FontScalePolicy.QUARTER_STEP
                )
        );
    }

    private Color toGdx(UiColor color) {
        UiColor c = color == null ? UiColor.WHITE : color;
        return scratchColor.set(c.r, c.g, c.b, c.a);
    }

    private FontTextAlign toFontAlign(TextAlign align) {
        TextAlign resolved = align == null ? TextAlign.LEFT : align;
        return switch (resolved) {
            case CENTER -> FontTextAlign.CENTER;
            case RIGHT -> FontTextAlign.RIGHT;
            case LEFT -> FontTextAlign.LEFT;
        };
    }
}
