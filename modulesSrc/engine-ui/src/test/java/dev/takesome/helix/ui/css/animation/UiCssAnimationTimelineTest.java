package dev.takesome.helix.ui.css.animation;

import dev.takesome.helix.ui.css.UiCssKeyframe;
import dev.takesome.helix.ui.css.UiCssKeyframesRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssAnimationTimelineTest {
    @Test
    void alternateDirectionUsesCycleIndexInsteadOfOnlyFractionalProgress() {
        UiCssAnimationDescriptor animation = new UiCssAnimationDescriptor(
                "pulse",
                1000L,
                0L,
                "linear",
                -1.0,
                "alternate",
                "both",
                "running"
        );
        UiCssKeyframesRule keyframes = new UiCssKeyframesRule("pulse", List.of(
                new UiCssKeyframe(0.0, Map.of("opacity", "0")),
                new UiCssKeyframe(1.0, Map.of("opacity", "1"))
        ));

        UiCssAnimationTimeline timeline = new UiCssAnimationTimeline();

        assertEquals("0.25", timeline.frame(List.of(animation), Map.of("pulse", keyframes), 250L).get("opacity"));
        assertEquals("0.75", timeline.frame(List.of(animation), Map.of("pulse", keyframes), 1250L).get("opacity"));
    }
}
