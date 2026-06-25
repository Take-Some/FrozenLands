package dev.takesome.helix.ui.css.runtime;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.css.transition.UiCssTransitionDescriptor;
import dev.takesome.helix.ui.css.transition.UiCssTransitionResolver;
import dev.takesome.helix.ui.css.transition.UiCssTransitionTimeline;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.NodeRuntimeBinding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiCssStyleRuntimeController implements NodeRuntimeBinding {
    private static final String[] STATE_ORDER = {"disabled", "checked", "focus", "hover", "active", "open"};

    private final String key;
    private final UiCssTransitionTimeline timeline;
    private final UiCssTransitionResolver resolver;
    private final UiCssNodeStyleApplier applier;
    private Map<String, String> targetStyle = Map.of();
    private Map<String, Map<String, String>> stateStyles = Map.of();
    private List<UiCssTransitionDescriptor> transitions = List.of();
    private long nowMs;

    public UiCssStyleRuntimeController(String key, Map<String, String> targetStyle) {
        this(key, targetStyle, Map.of(), new UiCssTransitionTimeline(), new UiCssTransitionResolver(), new UiCssNodeStyleApplier());
    }

    public UiCssStyleRuntimeController(String key, Map<String, String> targetStyle, Map<String, Map<String, String>> stateStyles) {
        this(key, targetStyle, stateStyles, new UiCssTransitionTimeline(), new UiCssTransitionResolver(), new UiCssNodeStyleApplier());
    }

    public UiCssStyleRuntimeController(String key, Map<String, String> targetStyle, Map<String, Map<String, String>> stateStyles, UiCssTransitionTimeline timeline, UiCssTransitionResolver resolver, UiCssNodeStyleApplier applier) {
        this.key = key == null || key.isBlank() ? "node" : key;
        this.timeline = timeline == null ? new UiCssTransitionTimeline() : timeline;
        this.resolver = resolver == null ? new UiCssTransitionResolver() : resolver;
        this.applier = applier == null ? new UiCssNodeStyleApplier() : applier;
        setTargetStyle(targetStyle);
        setStateStyles(stateStyles);
    }

    public void setTargetStyle(Map<String, String> style) {
        this.targetStyle = copy(style);
        this.transitions = resolver.resolveAll(this.targetStyle);
    }

    public void setStateStyles(Map<String, Map<String, String>> states) {
        LinkedHashMap<String, Map<String, String>> out = new LinkedHashMap<>();
        if (states != null) states.forEach((name, style) -> {
            if (name != null && !name.isBlank()) out.put(name.trim().toLowerCase(java.util.Locale.ROOT), copy(style));
        });
        this.stateStyles = Map.copyOf(out);
    }

    public Map<String, String> targetStyle() { return targetStyle; }

    public Map<String, Map<String, String>> stateStyles() { return stateStyles; }

    public List<UiCssTransitionDescriptor> transitions() { return transitions; }

    public void applyInitial(Node node) {
        Map<String, String> target = targetFor(node);
        Map<String, String> presented = timeline.frame(key, target, resolver.resolveAll(target), nowMs);
        applier.apply(node, presented);
    }

    @Override
    public void update(Node node, float dt) {
        if (node == null) return;
        if (Float.isFinite(dt) && dt > 0f) nowMs += Math.round(dt * 1000f);
        Map<String, String> target = targetFor(node);
        Map<String, String> presented = timeline.frame(key, target, resolver.resolveAll(target), nowMs);
        applier.apply(node, presented);
    }

    private Map<String, String> targetFor(Node node) {
        if (!(node instanceof UiCssStateProvider provider)) return targetStyle;
        LinkedHashMap<String, String> out = new LinkedHashMap<>(targetStyle);
        for (String state : STATE_ORDER) {
            if (!provider.cssState(state)) continue;
            Map<String, String> stateStyle = stateStyles.get(state);
            if (stateStyle != null) out.putAll(stateStyle);
        }
        return out;
    }

    private Map<String, String> copy(Map<String, String> source) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (source == null) return out;
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank()) out.put(key.trim().toLowerCase(java.util.Locale.ROOT), trimToEmpty(value));
        });
        return Map.copyOf(out);
    }
}
