package dev.takesome.helix.ui.definition;

import dev.takesome.helix.ui.animation.UiAnimationManifest;
import dev.takesome.helix.ui.binding.UiBindingManifest;
import dev.takesome.helix.variables.model.EngineVariableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveManifest;
import dev.takesome.helix.ui.legacy.render.UiNineSliceDefinition;

/** Data-driven UI document loaded from JSON. */
public final class UiDocument {
    public UiThemeDefinition theme = new UiThemeDefinition();
    public Map<String, EngineVariableDefinition> variables = new LinkedHashMap<>();
    public Map<String, UiFontDefinition> fonts = new LinkedHashMap<>();
    public Map<String, UiNineSliceDefinition> nineSlices = new LinkedHashMap<>();

    /** Manifest references declared by the document. Prefix with resource: for classpath resources. */
    public List<String> animationManifests = new ArrayList<>();

    /** Inline animation packs declared by the document. */
    public List<UiAnimationManifest> animationPacks = new ArrayList<>();

    /** Binding manifest references declared by the document. Prefix with resource: for classpath resources. */
    public List<String> bindingManifests = new ArrayList<>();

    /** Inline binding packs declared by the document. */
    public List<UiBindingManifest> bindingPacks = new ArrayList<>();

    /** Widget primitive manifest references declared by the document. Prefix with resource: for classpath resources. */
    public List<String> primitiveManifests = new ArrayList<>();

    /** Inline widget primitive packs declared by the document. */
    public List<UiWidgetPrimitiveManifest> primitivePacks = new ArrayList<>();

    public List<UiPanelDefinition> panels = new ArrayList<>();
}
