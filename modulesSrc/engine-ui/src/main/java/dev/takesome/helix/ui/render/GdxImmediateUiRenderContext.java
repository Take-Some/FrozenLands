package dev.takesome.helix.ui.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.BufferUtils;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.render.GdxUiPainter;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.nio.IntBuffer;

/**
 * Immediate LibGDX implementation for retained UI nodes.
 *
 * <p>This intentionally prioritizes correctness for the first boot menu slice:
 * fill calls use ShapeRenderer, text calls use SpriteBatch/BitmapFont. Later this
 * can be replaced by a queued/batched UI renderer without changing NodeScene.</p>
 */
public final class GdxImmediateUiRenderContext implements UiRenderContext, Disposable {
    private final GdxUiPrimitiveDrawer primitives;
    private final GdxUiTextDrawer text;
    private final GdxUiElementDrawer elements;
    private final GdxUiDebugTracer debugTracer;
    private final boolean elementSupport;
    private Matrix4 projection;
    private boolean drawTracers;
    private final Deque<UiRect> clipStack = new ArrayDeque<>();
    private final Deque<Float> opacityStack = new ArrayDeque<>();
    private float opacity = 1f;
    private final IntBuffer viewportBuffer = BufferUtils.newIntBuffer(16);

    public GdxImmediateUiRenderContext(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        this(batch, shapes, font, font, null, Collections.emptyMap(), null, null);
    }

    public GdxImmediateUiRenderContext(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, BitmapFont buttonFont) {
        this(batch, shapes, font, buttonFont, null, Collections.emptyMap(), null, null);
    }

    public GdxImmediateUiRenderContext(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, BitmapFont buttonFont, BitmapFont iconFont) {
        this(batch, shapes, font, buttonFont, iconFont, Collections.emptyMap(), null, null);
    }

    public GdxImmediateUiRenderContext(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, BitmapFont buttonFont, Map<String, BitmapFont> iconFonts) {
        this(batch, shapes, font, buttonFont, null, iconFonts, null, null);
    }

    public GdxImmediateUiRenderContext(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, AssetProvider assets, MaterialProvider materials) {
        this(batch, shapes, font, font, null, Collections.emptyMap(), assets, materials);
    }

    public GdxImmediateUiRenderContext(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, BitmapFont buttonFont, AssetProvider assets, MaterialProvider materials) {
        this(batch, shapes, font, buttonFont, null, Collections.emptyMap(), assets, materials);
    }

    public GdxImmediateUiRenderContext(
            SpriteBatch batch,
            ShapeRenderer shapes,
            BitmapFont font,
            BitmapFont buttonFont,
            BitmapFont iconFont,
            AssetProvider assets,
            MaterialProvider materials
    ) {
        this(batch, shapes, font, buttonFont, iconFont, Collections.emptyMap(), assets, materials);
    }

    public GdxImmediateUiRenderContext(
            SpriteBatch batch,
            ShapeRenderer shapes,
            BitmapFont font,
            BitmapFont buttonFont,
            BitmapFont fallbackIconFont,
            Map<String, BitmapFont> iconFonts,
            AssetProvider assets,
            MaterialProvider materials
    ) {
        if (batch == null) throw new IllegalArgumentException("batch must not be null");
        if (shapes == null) throw new IllegalArgumentException("shapes must not be null");
        if (font == null) throw new IllegalArgumentException("font must not be null");

        GdxUiPainter painter = new GdxUiPainter();
        this.primitives = new GdxUiPrimitiveDrawer(batch, shapes, font, painter);
        this.text = new GdxUiTextDrawer(batch, shapes, font, buttonFont, fallbackIconFont, iconFonts, assets, painter);
        this.elements = new GdxUiElementDrawer(batch, shapes, font, painter, assets, materials);
        this.elementSupport = assets != null || materials != null;
        this.debugTracer = new GdxUiDebugTracer(shapes);
    }

    public void setProjection(Matrix4 projection) {
        this.projection = projection;
    }

    public void setDrawTracers(boolean drawTracers) {
        this.drawTracers = drawTracers;
    }

    @Override
    public void fill(UiRect rect, UiColor color) {
        primitives.fill(projection, rect, withOpacity(color));
    }

    @Override
    public void text(String value, UiRect rect, float scale, UiColor color, TextAlign align) {
        text.text(projection, value, rect, scale, withOpacity(color), align);
    }

    @Override
    public void text(String value, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        text.text(projection, value, rect, scale, withOpacity(color), align, fontId);
    }

    @Override
    public void buttonText(String value, UiRect rect, float scale, UiColor color, TextAlign align) {
        buttonText(value, rect, scale, color, align, "");
    }

    @Override
    public void buttonText(String value, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        text.buttonText(projection, value, rect, scale, withOpacity(color), align, fontId);
    }


    @Override
    public boolean pushOpacity(float opacity) {
        opacityStack.push(this.opacity);
        this.opacity = this.opacity * clamp01(opacity);
        return true;
    }

    @Override
    public void popOpacity() {
        if (opacityStack.isEmpty()) {
            opacity = 1f;
            return;
        }
        opacity = opacityStack.pop();
    }

    @Override
    public boolean pushClip(UiRect rect) {
        if (rect == null || rect.w <= 0f || rect.h <= 0f) return false;
        UiRect next = rect;
        if (!clipStack.isEmpty()) next = intersect(clipStack.peek(), rect);
        if (next.w <= 0f || next.h <= 0f) return false;
        UiScissorRectResolver.UiScissorRect scissor = screenScissor(next);
        if (scissor == null) return false;
        clipStack.push(next);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
        return true;
    }

    @Override
    public void popClip() {
        if (clipStack.isEmpty()) return;
        clipStack.pop();
        if (clipStack.isEmpty()) {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            return;
        }
        UiRect next = clipStack.peek();
        UiScissorRectResolver.UiScissorRect scissor = screenScissor(next);
        if (scissor == null) {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            clipStack.clear();
            return;
        }
        Gdx.gl.glScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
    }

    private UiScissorRectResolver.UiScissorRect screenScissor(UiRect rect) {
        viewportBuffer.clear();
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, viewportBuffer);
        return UiScissorRectResolver.resolve(
                rect,
                projection,
                viewportBuffer.get(0),
                viewportBuffer.get(1),
                viewportBuffer.get(2),
                viewportBuffer.get(3)
        );
    }

    private UiRect intersect(UiRect a, UiRect b) {
        float x0 = Math.max(a.x, b.x);
        float y0 = Math.max(a.y, b.y);
        float x1 = Math.min(a.x + a.w, b.x + b.w);
        float y1 = Math.min(a.y + a.h, b.y + b.h);
        return new UiRect(x0, y0, Math.max(0f, x1 - x0), Math.max(0f, y1 - y0));
    }

    @Override
    public boolean supportsIcons() {
        return text.supportsIcons();
    }

    @Override
    public boolean supportsElements() {
        return true;
    }

    @Override
    public boolean supportsImages() {
        return false;
    }

    @Override
    public boolean icon(UiIcon icon, UiRect rect, float scale, UiColor color, TextAlign align) {
        return text.icon(projection, icon, rect, scale, withOpacity(color), align);
    }

    @Override
    public boolean drawElement(UiElementSkin element, UiRect rect) {
        return drawElement(element, rect, null);
    }

    @Override
    public boolean drawElement(UiElementSkin element, UiRect rect, UiColor tint) {
        return elements.draw(projection, element, rect, elementTint(tint));
    }

    @Override
    public UiRect elementContentBounds(UiElementSkin element, UiRect rect) {
        return elements.contentBounds(element, rect);
    }

    @Override
    public boolean drawTracers() {
        return drawTracers;
    }

    @Override
    public void traceNode(UiRect bounds, UiRect contentBounds, int depth, String label) {
        if (!drawTracers) return;
        debugTracer.draw(projection, bounds, contentBounds, depth, label);
    }

    private UiColor withOpacity(UiColor color) {
        if (color == null || opacity >= 0.999f) return color;
        return new UiColor(color.r, color.g, color.b, color.a * opacity);
    }

    private UiColor elementTint(UiColor tint) {
        if (opacity >= 0.999f) return tint;
        if (tint == null) return new UiColor(1f, 1f, 1f, opacity);
        return new UiColor(tint.r, tint.g, tint.b, tint.a * opacity);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1f;
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    @Override
    public void dispose() {
        text.dispose();
        elements.dispose();
    }
}
