package dev.takesome.helix.ui.legacy.render;

import java.util.Objects;

/**
 * A panel is not a picture. It is a nine-slice object:
 *
 * [ TL ][  TOP   ][ TR ]
 * [ L  ][ CENTER ][ R  ]
 * [ BL ][ BOTTOM ][ BR ]
 */
public final class NineSlice<TTextureRegion> {
    public final TTextureRegion topLeft;
    public final TTextureRegion top;
    public final TTextureRegion topRight;

    public final TTextureRegion left;
    public final TTextureRegion center;
    public final TTextureRegion right;

    public final TTextureRegion bottomLeft;
    public final TTextureRegion bottom;
    public final TTextureRegion bottomRight;

    public NineSlice(
            TTextureRegion topLeft,
            TTextureRegion top,
            TTextureRegion topRight,
            TTextureRegion left,
            TTextureRegion center,
            TTextureRegion right,
            TTextureRegion bottomLeft,
            TTextureRegion bottom,
            TTextureRegion bottomRight
    ) {
        this.topLeft = required(topLeft, "topLeft");
        this.top = required(top, "top");
        this.topRight = required(topRight, "topRight");
        this.left = required(left, "left");
        this.center = required(center, "center");
        this.right = required(right, "right");
        this.bottomLeft = required(bottomLeft, "bottomLeft");
        this.bottom = required(bottom, "bottom");
        this.bottomRight = required(bottomRight, "bottomRight");
    }

    private static <T> T required(T value, String name) {
        return Objects.requireNonNull(value, "NineSlice region is null: " + name);
    }
}
