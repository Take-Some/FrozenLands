package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.materials.model.Material;

import java.util.HashMap;
import java.util.Map;
import dev.takesome.helix.ui.definition.UiDocument;

public final class UiNineSliceResolver {
    private final AssetProvider assets;
    private final MaterialProvider materials;
    private final int alphaGridThreshold;
    private final int alphaSheetThreshold;
    private final Map<String, NineSlice<TextureRegion>> slices = new HashMap<>();
    private final Map<String, AlphaUiSheet> alphaSheets = new HashMap<>();

    UiNineSliceResolver(AssetProvider assets, MaterialProvider materials, int alphaGridThreshold, int alphaSheetThreshold) {
        this.assets = assets;
        this.materials = materials;
        this.alphaGridThreshold = alphaGridThreshold;
        this.alphaSheetThreshold = alphaSheetThreshold;
    }

    public NineSlice<TextureRegion> slice(UiDocument doc, String id) {
        NineSlice<TextureRegion> cached = slices.get(id);
        if (cached != null) return cached;

        UiNineSliceDefinition d = doc.nineSlices == null ? null : doc.nineSlices.get(id);
        if (d == null) return null;

        TextureRegion base = sourceRegion(d.material != null ? d.material : d.texture);
        if (base == null) return null;

        NineSlice<TextureRegion> resolved = alphaSource(d.source) ? AlphaGridNineSlice.from(base, alphaGridThreshold) : null;
        if (resolved == null) resolved = explicitSlice(base, d);
        slices.put(id, resolved);
        return resolved;
    }

    TextureRegion rawRegion(String id) {
        if (id == null || id.isBlank()) return null;
        if (materials != null) {
            Material material = materials.material(id);
            if (material != null && material.textureId != null && !material.textureId.isBlank()) {
                TextureRegion textureRegion = assets.region(material.textureId);
                if (textureRegion != null) return textureRegion;
            }
        }
        return assets.region(id);
    }

    TextureRegion region(String id, int frame) {
        if (id == null || id.isBlank()) return null;
        if (materials != null) {
            TextureRegion material = frame == 0 ? materials.materialRegion(id) : materials.materialSprite(id, frame);
            if (material != null) return material;
        }
        return assets.region(id);
    }

    AlphaUiSheet alphaSheet(String id, TextureRegion source) {
        if (id == null || id.isBlank() || source == null) return null;
        AlphaUiSheet cached = alphaSheets.get(id);
        if (cached != null) return cached;
        AlphaUiSheet parsed = AlphaUiSheet.parse(source, alphaSheetThreshold);
        if (parsed != null) alphaSheets.put(id, parsed);
        return parsed;
    }

    public void clear() {
        slices.clear();
        alphaSheets.clear();
    }

    private TextureRegion sourceRegion(String id) {
        if (id == null || id.isBlank()) return null;
        if (materials != null) {
            TextureRegion material = materials.materialRegion(id);
            if (material != null) return material;
        }
        return assets.region(id);
    }

    private NineSlice<TextureRegion> explicitSlice(TextureRegion base, UiNineSliceDefinition d) {
        return new NineSlice<>(
                sub(base, d.topLeft), sub(base, d.top), sub(base, d.topRight),
                sub(base, d.left), sub(base, d.center), sub(base, d.right),
                sub(base, d.bottomLeft), sub(base, d.bottom), sub(base, d.bottomRight)
        );
    }

    private boolean alphaSource(String source) {
        if (source == null || source.isBlank()) return false;
        String normalized = source.trim();
        return "alpha".equalsIgnoreCase(normalized)
                || "auto".equalsIgnoreCase(normalized)
                || "alphaGrid".equalsIgnoreCase(normalized)
                || "alpha-grid".equalsIgnoreCase(normalized);
    }

    private TextureRegion sub(TextureRegion base, UiRegionDefinition r) {
        return r == null ? base : r.slice(base);
    }
}
