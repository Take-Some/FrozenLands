package dev.takesome.helix.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDefinitionLoader;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.render.GdxUiPainter;
import dev.takesome.helix.ui.legacy.render.NineSliceRenderer;
import dev.takesome.helix.ui.legacy.render.TextureRegionMetrics;
import dev.takesome.helix.ui.legacy.render.UiDocumentRenderer;

/**
 * Public library runtime for rendering engine-ui documents.
 *
 * <p>This class is the supported high-level runtime boundary for game,
 * editor and standalone consumers. It owns the default GDX painter,
 * nine-slice adapter and document renderer wiring, so callers do not need
 * to construct internal renderer passes manually.</p>
 */
public final class EngineUiRuntime implements Disposable {
    private final EngineUiConfig config;
    private final AssetProvider assets;
    private final MaterialProvider materials;
    private final GdxUiPainter painter;
    private final UiDocumentRenderer renderer;
    private UiDocument document;

    EngineUiRuntime(EngineUiConfig config) {
        if (config == null) throw new IllegalArgumentException("config must not be null");
        this.config = config;
        this.assets = config.assets();
        this.materials = config.materials();
        this.painter = config.painter();
        this.renderer = new UiDocumentRenderer(
                assets,
                materials,
                new NineSliceRenderer<TextureRegion>(painter, TextureRegionMetrics.INSTANCE),
                config.animationPipeline(),
                config.i18n()
        );
    }

    public EngineUiConfig config() {
        return config;
    }

    public UiDocument document() {
        return document;
    }

    public EngineUiRuntime document(UiDocument document) {
        this.document = document == null ? new UiDocument() : document;
        return this;
    }

    public UiDocument load(String internalPath) {
        return document(UiDefinitionLoader.load(internalPath)).document;
    }

    public UiDocument loadOrEmpty(String internalPath) {
        return document(UiDefinitionLoader.loadOrDefault(internalPath, new UiDocument())).document;
    }

    public UiDocument tryLoad(String internalPath) {
        if (internalPath == null || internalPath.isBlank()) return null;
        try {
            return UiDefinitionLoader.load(internalPath);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public UiDocument loadFirst(String... internalPaths) {
        if (internalPaths != null) {
            for (String path : internalPaths) {
                UiDocument candidate = tryLoad(path);
                if (candidate != null) return document(candidate).document;
            }
        }
        return document(new UiDocument()).document;
    }

    public UiDocument loadFirstWithPanels(String... internalPaths) {
        if (internalPaths != null) {
            for (String path : internalPaths) {
                UiDocument candidate = tryLoad(path);
                if (EngineUi.hasPanels(candidate)) return document(candidate).document;
            }
        }
        return document(new UiDocument()).document;
    }

    public void setI18n(EngineI18n i18n) {
        renderer.setI18n(i18n);
    }

    public void setDrawTracers(boolean drawTracers) {
        renderer.setDrawTracers(drawTracers);
    }

    public void resetEffects() {
        renderer.resetEffects();
    }

    public void render(UiBindingSource binding, SpriteBatch batch, float viewportW, float viewportH) {
        render(document, binding, batch, config.defaultFont(), viewportW, viewportH);
    }

    public void render(UiDocument document, UiBindingSource binding, SpriteBatch batch, float viewportW, float viewportH) {
        render(document, binding, batch, config.defaultFont(), viewportW, viewportH);
    }

    public void render(UiDocument document, UiBindingSource binding, SpriteBatch batch, BitmapFont font, float viewportW, float viewportH) {
        if (batch == null) return;
        BitmapFont resolvedFont = font == null ? config.defaultFont() : font;
        painter.bind(batch, null, resolvedFont);
        try {
            renderer.render(document, binding, batch, resolvedFont, viewportW, viewportH);
        } finally {
            painter.unbind();
        }
    }

    public void renderFrame(UiBindingSource binding, SpriteBatch batch, float viewportW, float viewportH) {
        renderFrame(document, binding, batch, config.defaultFont(), viewportW, viewportH);
    }

    public void renderFrame(UiDocument document, UiBindingSource binding, SpriteBatch batch, float viewportW, float viewportH) {
        renderFrame(document, binding, batch, config.defaultFont(), viewportW, viewportH);
    }

    public void renderFrame(UiDocument document, UiBindingSource binding, SpriteBatch batch, BitmapFont font, float viewportW, float viewportH) {
        if (batch == null) return;
        batch.enableBlending();
        boolean drawing = false;
        try {
            batch.begin();
            drawing = true;
            render(document, binding, batch, font, viewportW, viewportH);
        } finally {
            painter.unbind();
            if (drawing) batch.end();
        }
    }

    @Override
    public void dispose() {
        renderer.dispose();
        painter.unbind();
    }
}
