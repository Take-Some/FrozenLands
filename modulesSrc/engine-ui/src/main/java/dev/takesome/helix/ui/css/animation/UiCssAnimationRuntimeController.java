package dev.takesome.helix.ui.css.animation;



import dev.takesome.helix.ui.css.UiCssKeyframesRule;

import dev.takesome.helix.ui.css.runtime.UiCssNodeStyleApplier;

import dev.takesome.helix.ui.node.Node;

import dev.takesome.helix.ui.node.NodeRuntimeBinding;



import java.util.List;

import java.util.Map;



public final class UiCssAnimationRuntimeController implements NodeRuntimeBinding {

    private final Map<String, String> baseStyle;

    private final Map<String, UiCssKeyframesRule> keyframes;

    private final UiCssAnimationResolver resolver = new UiCssAnimationResolver();

    private final UiCssAnimationTimeline timeline = new UiCssAnimationTimeline();

    private final UiCssNodeStyleApplier applier = new UiCssNodeStyleApplier();

    private long nowMs;



    public UiCssAnimationRuntimeController(Map<String, String> baseStyle, Map<String, UiCssKeyframesRule> keyframes) {

        this.baseStyle = baseStyle == null ? Map.of() : Map.copyOf(baseStyle);

        this.keyframes = keyframes == null ? Map.of() : Map.copyOf(keyframes);

    }



    public boolean active() {

        return !keyframes.isEmpty() && baseStyle.containsKey("animation-name");

    }



    @Override

    public void update(Node node, float dt) {

        if (node == null || !active()) return;

        if (Float.isFinite(dt) && dt > 0f) nowMs += Math.round(dt * 1000f);

        List<UiCssAnimationDescriptor> animations = resolver.resolveAll(baseStyle);

        Map<String, String> frame = timeline.frame(animations, keyframes, nowMs);

        if (!frame.isEmpty()) applier.apply(node, frame, baseStyle);

    }

}
