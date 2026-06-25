package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.model.UiRect;

final class UiWidgetImageRenderer {
    private final UiNineSliceResolver resolver;
    private final UiGeneratedTextures generatedTextures;
    private final Color scratch = new Color();
    private final Color alphaColor = new Color();

    UiWidgetImageRenderer(UiNineSliceResolver resolver, UiGeneratedTextures generatedTextures) {
        this.resolver = resolver;
        this.generatedTextures = generatedTextures;
    }

    void fillWidget(SpriteBatch batch, UiRect panel, UiWidgetDefinition widget) {
        float[] rgba = widget.fillColor != null ? widget.fillColor : widget.color;
        fillRect(batch, panel.x + UiWidgetBounds.x(widget), panel.y + UiWidgetBounds.y(widget), UiWidgetBounds.w(widget), UiWidgetBounds.h(widget), rgba, 1f);
    }

    void fillRect(SpriteBatch batch, float x, float y, float w, float h, float[] rgba, float alpha) {
        if (rgba == null || w <= 0f || h <= 0f) return;
        Color c = color(rgba, null);
        if (c == null || c.a <= 0f) return;
        drawTinted(batch, generatedTextures.solidRegion(), x, y, w, h, colorWithAlpha(c, alpha));
    }

    void fillRoundedRect(SpriteBatch batch, float x, float y, float w, float h, float radius, float[] rgba, float alpha) {
        if (rgba == null || w <= 0f || h <= 0f) return;
        float r = Math.max(0f, Math.min(radius, Math.min(w, h) * 0.5f));
        if (r <= 0.5f) {
            fillRect(batch, x, y, w, h, rgba, alpha);
            return;
        }
        fillRect(batch, x + r, y, Math.max(0f, w - r * 2f), h, rgba, alpha);
        fillRect(batch, x, y + r, r, Math.max(0f, h - r * 2f), rgba, alpha);
        fillRect(batch, x + w - r, y + r, r, Math.max(0f, h - r * 2f), rgba, alpha);
        float cap = Math.max(1f, r * 0.55f);
        fillRect(batch, x + cap, y + cap, Math.max(0f, r - cap), Math.max(0f, r - cap), rgba, alpha);
        fillRect(batch, x + w - r, y + cap, Math.max(0f, r - cap), Math.max(0f, r - cap), rgba, alpha);
        fillRect(batch, x + cap, y + h - r, Math.max(0f, r - cap), Math.max(0f, r - cap), rgba, alpha);
        fillRect(batch, x + w - r, y + h - r, Math.max(0f, r - cap), Math.max(0f, r - cap), rgba, alpha);
    }

    void image(SpriteBatch batch, UiRect panel, UiWidgetDefinition widget) {
        TextureRegion source = resolver.rawRegion(widget.material);
        AlphaUiSheet sheet = resolver.alphaSheet(widget.material, source);
        float x = panel.x + UiWidgetBounds.x(widget);
        float y = panel.y + UiWidgetBounds.y(widget);
        float w = UiWidgetBounds.w(widget);
        float h = UiWidgetBounds.h(widget);
        if (sheet != null && sheet.drawHorizontalStretch(batch, widget.frame, x, y, w, h)) return;
        TextureRegion image = sheet == null ? null : sheet.item(widget.frame);
        if (image == null) image = resolver.region(widget.material, widget.frame);
        if (image != null) batch.draw(image, x, y, w, h);
    }

    void stretchImage(SpriteBatch batch, UiRect panel, UiWidgetDefinition widget) {
        TextureRegion source = resolver.rawRegion(widget.material);
        AlphaUiSheet sheet = resolver.alphaSheet(widget.material, source);
        float x = panel.x + UiWidgetBounds.x(widget);
        float y = panel.y + UiWidgetBounds.y(widget);
        float w = UiWidgetBounds.w(widget);
        float h = UiWidgetBounds.h(widget);
        if (sheet != null && sheet.drawHorizontalStretch(batch, widget.frame, x, y, w, h)) return;
        if (source != null) batch.draw(source, x, y, w, h);
    }

    void drawStretchedAsset(SpriteBatch batch, String id, int frame, float x, float y, float w, float h, Color replacementColor) {
        if (id == null || id.isBlank() || w <= 0f || h <= 0f) return;
        String cacheKey = replacementColor == null ? id : generatedTextures.colorReplacementKey(id, replacementColor);
        TextureRegion source = replacementColor == null
                ? resolver.rawRegion(id)
                : generatedTextures.colorReplacedRawRegion(id, resolver.rawRegion(id), replacementColor);
        AlphaUiSheet sheet = resolver.alphaSheet(cacheKey, source);
        if (sheet != null && sheet.drawHorizontalStretch(batch, frame, x, y, w, h)) return;
        TextureRegion image = replacementColor == null ? resolver.region(id, frame) : source;
        drawTinted(batch, image, x, y, w, h, null);
    }

    TextureRegion region(String id, int frame) {
        return resolver.region(id, frame);
    }

    Color color(float[] rgba, Color fallback) {
        if (rgba == null || rgba.length < 3) return fallback;
        return scratch.set(rgba[0], rgba[1], rgba[2], rgba.length > 3 ? rgba[3] : 1f);
    }

    private Color colorWithAlpha(Color source, float alpha) {
        float a = MathUtils.clamp(alpha, 0f, 1f);
        return alphaColor.set(source.r, source.g, source.b, source.a * a);
    }

    private void drawTinted(SpriteBatch batch, TextureRegion image, float x, float y, float w, float h, Color tint) {
        if (batch == null || image == null || w <= 0f || h <= 0f) return;
        if (tint == null) {
            batch.draw(image, x, y, w, h);
            return;
        }
        Color oldColor = batch.getColor().cpy();
        try {
            batch.setColor(tint);
            batch.draw(image, x, y, w, h);
        } finally {
            batch.setColor(oldColor);
        }
    }
}
