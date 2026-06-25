package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.animation.UiAnimationPipeline;
import dev.takesome.helix.ui.pipeline.UiBindingPreparePass;
import dev.takesome.helix.ui.pipeline.UiPanelLayoutPass;
import dev.takesome.helix.ui.pipeline.UiPanelRenderPass;
import dev.takesome.helix.ui.pipeline.UiWidgetRenderPass;
import dev.takesome.helix.ui.runtime.EngineUiRuntime;

import java.util.List;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;

/** Generic JSON-driven UI renderer facade. Rendering work is delegated to explicit pipeline passes. */
public final class UiDocumentRenderer implements Disposable {
    private static final int ALPHA_GRID_THRESHOLD = 8;
    private static final int ALPHA_SHEET_THRESHOLD = 8;

    private final UiAnimationPipeline animationPipeline;
    private final UiGeneratedTextures generatedTextures;
    private final UiNineSliceResolver resolver;
    private final UiWidgetRenderers widgets;
    private final UiBindingPreparePass bindingPreparePass;
    private final UiPanelLayoutPass panelLayoutPass;
    private final UiPanelRenderPass panelRenderPass;
    private float uiTime;
    private boolean drawTracers;

    public UiDocumentRenderer(AssetProvider assets, MaterialProvider materials, NineSliceRenderer<TextureRegion> nine) {
        this(assets, materials, nine, new UiAnimationPipeline(), null);
    }

    public UiDocumentRenderer(AssetProvider assets, MaterialProvider materials, NineSliceRenderer<TextureRegion> nine, EngineI18n i18n) {
        this(assets, materials, nine, new UiAnimationPipeline(), i18n);
    }

    public UiDocumentRenderer(AssetProvider assets, MaterialProvider materials, NineSliceRenderer<TextureRegion> nine, UiAnimationPipeline animationPipeline) {
        this(assets, materials, nine, animationPipeline, null);
    }

    public UiDocumentRenderer(AssetProvider assets, MaterialProvider materials, NineSliceRenderer<TextureRegion> nine, UiAnimationPipeline animationPipeline, EngineI18n i18n) {
        this.animationPipeline = animationPipeline == null ? new UiAnimationPipeline() : animationPipeline;
        this.generatedTextures = new UiGeneratedTextures();
        this.resolver = new UiNineSliceResolver(assets, materials, ALPHA_GRID_THRESHOLD, ALPHA_SHEET_THRESHOLD);

        UiTextRenderer text = new UiTextRenderer(assets, this.animationPipeline, i18n);
        UiWidgetImageRenderer images = new UiWidgetImageRenderer(resolver, generatedTextures);
        this.widgets = new UiWidgetRenderers(resolver, images, text, new UiProgressBarRenderer(images, text));
        UiWidgetRenderPass widgetRenderPass = new UiWidgetRenderPass(widgets);
        this.bindingPreparePass = new UiBindingPreparePass();
        this.panelLayoutPass = new UiPanelLayoutPass();
        this.panelRenderPass = new UiPanelRenderPass(nine, resolver, widgets, widgetRenderPass);
        setI18n(i18n);
    }

    public void render(UiDocument doc, UiBindingSource binding, SpriteBatch batch, BitmapFont font, float viewportW, float viewportH) {
        if (doc == null || doc.panels == null || batch == null || font == null) return;
        animationPipeline.configure(doc);
        UiBindingSource preparedBinding = bindingPreparePass.prepare(doc, binding);
        uiTime = nextUiTime();
        animationPipeline.beginFrame(uiTime);
        try {
            widgets.prepareFonts(doc);
            List<UiPanelRenderEntry> panels = panelLayoutPass.layout(doc, preparedBinding, font, viewportW, viewportH);
            panelRenderPass.render(doc, preparedBinding, batch, font, panels, viewportW, viewportH, uiTime, drawTracers);
        } finally {
            animationPipeline.endFrame();
        }
    }

    public void resetEffects() {
        animationPipeline.reset();
        widgets.resetEffects();
        uiTime = 0f;
    }

    public void setI18n(EngineI18n i18n) {
        widgets.setI18n(i18n);
        bindingPreparePass.setI18n(i18n);
    }

    public void setDrawTracers(boolean drawTracers) {
        this.drawTracers = drawTracers;
    }

    private float nextUiTime() {
        float dt = EngineUiRuntime.current().lastDeltaSeconds();
        if (!Float.isFinite(dt) || dt <= 0f) dt = 1f / 60f;
        return uiTime + MathUtils.clamp(dt, 0f, 0.25f);
    }

    @Override
    public void dispose() {
        animationPipeline.reset();
        resolver.clear();
        generatedTextures.dispose();
    }
}
