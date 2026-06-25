package dev.takesome.helix.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.animation.UiAnimationPipeline;
import dev.takesome.helix.ui.render.GdxUiPainter;

/**
 * Immutable configuration for the public engine-ui library entry point.
 *
 * <p>The config intentionally accepts generic infrastructure contracts only:
 * assets, optional materials, optional i18n, an optional default font and the
 * low-level painter/animation overrides used by tests or standalone tools.</p>
 */
public final class EngineUiConfig {
    private final AssetProvider assets;
    private final MaterialProvider materials;
    private final BitmapFont defaultFont;
    private final EngineI18n i18n;
    private final GdxUiPainter painter;
    private final UiAnimationPipeline animationPipeline;

    private EngineUiConfig(Builder builder) {
        this.assets = requireAssets(builder.assets);
        this.materials = builder.materials;
        this.defaultFont = builder.defaultFont;
        this.i18n = builder.i18n;
        this.painter = builder.painter == null ? new GdxUiPainter() : builder.painter;
        this.animationPipeline = builder.animationPipeline == null ? new UiAnimationPipeline() : builder.animationPipeline;
    }

    public static Builder builder(AssetProvider assets) {
        return new Builder(assets);
    }

    public static EngineUiConfig of(AssetProvider assets) {
        return builder(assets).build();
    }

    public static EngineUiConfig of(AssetProvider assets, MaterialProvider materials) {
        return builder(assets).materials(materials).build();
    }

    public AssetProvider assets() {
        return assets;
    }

    public MaterialProvider materials() {
        return materials;
    }

    public BitmapFont defaultFont() {
        return defaultFont == null ? assets.font() : defaultFont;
    }

    public EngineI18n i18n() {
        return i18n;
    }

    public GdxUiPainter painter() {
        return painter;
    }

    public UiAnimationPipeline animationPipeline() {
        return animationPipeline;
    }

    private static AssetProvider requireAssets(AssetProvider assets) {
        if (assets == null) throw new IllegalArgumentException("assets must not be null");
        return assets;
    }

    public static final class Builder {
        private final AssetProvider assets;
        private MaterialProvider materials;
        private BitmapFont defaultFont;
        private EngineI18n i18n;
        private GdxUiPainter painter;
        private UiAnimationPipeline animationPipeline;

        private Builder(AssetProvider assets) {
            this.assets = requireAssets(assets);
        }

        public Builder materials(MaterialProvider materials) {
            this.materials = materials;
            return this;
        }

        public Builder defaultFont(BitmapFont defaultFont) {
            this.defaultFont = defaultFont;
            return this;
        }

        public Builder i18n(EngineI18n i18n) {
            this.i18n = i18n;
            return this;
        }

        public Builder painter(GdxUiPainter painter) {
            this.painter = painter;
            return this;
        }

        public Builder animationPipeline(UiAnimationPipeline animationPipeline) {
            this.animationPipeline = animationPipeline;
            return this;
        }

        public EngineUiConfig build() {
            return new EngineUiConfig(this);
        }
    }
}
