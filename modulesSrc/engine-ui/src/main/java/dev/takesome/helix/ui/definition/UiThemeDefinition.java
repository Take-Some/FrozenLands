package dev.takesome.helix.ui.definition;

import dev.takesome.helix.ui.animation.UiAnimationManifest;
import dev.takesome.helix.ui.binding.UiBindingManifest;

import java.util.ArrayList;
import java.util.List;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveManifest;

/** Theme values for generic UI documents. */
public final class UiThemeDefinition {
    public float padding = 10f;
    public float barHeight = 16f;
    public float iconSize = 18f;
    public float fontScaleTitle = 1.0f;
    public float fontScaleSmall = 0.8f;
    public float[] textPrimary = new float[] {1f, 0.94f, 0.78f, 1f};
    public float[] textSecondary = new float[] {0.76f, 0.88f, 1f, 0.94f};

    /** Manifest references declared by the theme. Prefix with resource: for classpath resources. */
    public List<String> animationManifests = new ArrayList<>();

    /** Inline animation packs declared by the theme. */
    public List<UiAnimationManifest> animationPacks = new ArrayList<>();

    /** Binding manifest references declared by the theme. Prefix with resource: for classpath resources. */
    public List<String> bindingManifests = new ArrayList<>();

    /** Inline binding packs declared by the theme. */
    public List<UiBindingManifest> bindingPacks = new ArrayList<>();

    /** Widget primitive manifest references declared by the theme. Prefix with resource: for classpath resources. */
    public List<String> primitiveManifests = new ArrayList<>();

    /** Inline widget primitive packs declared by the theme. */
    public List<UiWidgetPrimitiveManifest> primitivePacks = new ArrayList<>();
}
