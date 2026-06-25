package dev.takesome.helix.ui.skin;

import dev.takesome.helix.ui.skin.UiElementSkin;
import java.util.Map;

/** Converts already-computed style maps into paint skin descriptors. */
public interface UiSkinResolver {
    UiElementSkin resolve(Map<String, String> style, String defaultKind);
}
