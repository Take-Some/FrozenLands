package dev.takesome.helix.ui.skin;

/** Three independent source pieces used for horizontally scalable UI chrome. */
public record UiSkinThreeSlice(
        UiSkinRect left,
        UiSkinRect middle,
        UiSkinRect right,
        UiSliceScaleMode mode
) {
    public UiSkinThreeSlice {
        left = left == null ? UiSkinRect.EMPTY : left;
        middle = middle == null ? UiSkinRect.EMPTY : middle;
        right = right == null ? UiSkinRect.EMPTY : right;
        mode = mode == null ? UiSliceScaleMode.STRETCH : mode;
    }

    public boolean valid() {
        return left.valid() && middle.valid() && right.valid();
    }

    public int nominalHeight() {
        return Math.max(left.h(), Math.max(middle.h(), right.h()));
    }
}
