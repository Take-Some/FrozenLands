package dev.takesome.helix.ui.skin;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
/**
 * Engine-owned skin descriptor. Game/editor data owns concrete ids and source
 * coordinates; engine-ui owns how the descriptor is rendered.
 */
public record UiSkinDescriptor(
        String id,
        UiSkinType type,
        String source,
        int frame,
        UiSkinRect sourceRect,
        UiSkinThreeSlice threeSlice
) {
    public UiSkinDescriptor {
        id = clean(id);
        source = clean(source);
        if (id.isBlank()) throw new IllegalArgumentException("UI skin id must not be blank");
        if (source.isBlank()) throw new IllegalArgumentException("UI skin source must not be blank: " + id);
        type = type == null ? UiSkinType.IMAGE : type;
        frame = Math.max(0, frame);
        sourceRect = sourceRect == null ? UiSkinRect.EMPTY : sourceRect;
        threeSlice = threeSlice == null ? new UiSkinThreeSlice(UiSkinRect.EMPTY, UiSkinRect.EMPTY, UiSkinRect.EMPTY, UiSliceScaleMode.STRETCH) : threeSlice;
    }

    public boolean usesThreeSlice() {
        return type == UiSkinType.THREE_SLICE && threeSlice.valid();
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }
}
