package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.materials.model.Material;
import dev.takesome.helix.ui.skin.UiElementSkin;

final class UiElementAssetResolver implements Disposable {
    private final AssetProvider assets;
    private final MaterialProvider materials;
    private final UiDirectTextureCache directTextures = new UiDirectTextureCache();

    UiElementAssetResolver(AssetProvider assets, MaterialProvider materials) {
        this.assets = assets;
        this.materials = materials;
    }

    TextureRegion region(UiElementSkin element) {
        if (element == null || !element.hasSource()) return null;
        String source = element.source();

        if (directTextures.looksLikePath(source)) {
            TextureRegion direct = directTextures.region(source, element.frame());
            if (direct != null) return direct;
        }

        if (materials != null) {
            if (element.frame() > 0) {
                TextureRegion sprite = materials.materialSprite(source, element.frame());
                if (sprite != null) return sprite;
            }

            TextureRegion material = materials.materialRegion(source);
            if (material != null) return material;

            Material descriptor = materials.material(source);
            if (descriptor != null && descriptor.textureId != null && !descriptor.textureId.isBlank() && assets != null) {
                TextureRegion texture = assets.region(descriptor.textureId);
                if (texture != null) return texture;
            }
        }

        if (assets != null) {
            TextureRegion texture = assets.region(source);
            if (texture != null) return texture;
        }

        return directTextures.looksLikePath(source) ? null : directTextures.region(source);
    }

    @Override
    public void dispose() {
        directTextures.dispose();
    }
}
