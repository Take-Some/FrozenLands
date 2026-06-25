package dev.takesome.helix.ui.css.properties.animation;

import dev.takesome.helix.ui.css.UiCssStringPropertySpec;
import java.util.Set;

public final class AnimationNameCssProperty extends UiCssStringPropertySpec {
    public AnimationNameCssProperty() {
        super("animation-name", Set.of(), true);
    }
}
