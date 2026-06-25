package dev.takesome.helix.ui.css.transition;

import dev.takesome.helix.ui.animation.UiAnimationPipeline;
import dev.takesome.helix.ui.css.UiCssCascade;
import dev.takesome.helix.ui.css.UiCssParser;
import dev.takesome.helix.ui.css.UiStylesheet;
import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssTransitionTimelineTest {
    @Test
    void interpolatesNumericValuesOverCssTimeline() {
        UiCssTransitionTimeline timeline = new UiCssTransitionTimeline();
        UiCssTransitionDescriptor transition = new UiCssTransitionDescriptor("opacity", 100L, 0L, "linear");

        assertEquals("0", timeline.frame("button", Map.of("opacity", "0"), transition, 0L).get("opacity"));
        assertEquals("0", timeline.frame("button", Map.of("opacity", "1"), transition, 0L).get("opacity"));
        assertEquals("0.5", timeline.frame("button", Map.of("opacity", "1"), transition, 50L).get("opacity"));
        assertEquals("1", timeline.frame("button", Map.of("opacity", "1"), transition, 100L).get("opacity"));
    }

    @Test
    void respectsDelayAndLengthUnits() {
        UiCssTransitionTimeline timeline = new UiCssTransitionTimeline();
        UiCssTransitionDescriptor transition = new UiCssTransitionDescriptor("left", 100L, 100L, "linear");

        assertEquals("0px", timeline.frame("panel", Map.of("left", "0px"), transition, 0L).get("left"));
        assertEquals("0px", timeline.frame("panel", Map.of("left", "100px"), transition, 0L).get("left"));
        assertEquals("0px", timeline.frame("panel", Map.of("left", "100px"), transition, 50L).get("left"));
        assertEquals("50px", timeline.frame("panel", Map.of("left", "100px"), transition, 150L).get("left"));
    }

    @Test
    void interpolatesColors() {
        UiCssTransitionTimeline timeline = new UiCssTransitionTimeline();
        UiCssTransitionDescriptor transition = new UiCssTransitionDescriptor("background-color", 100L, 0L, "linear");

        timeline.frame("box", Map.of("background-color", "#000000"), transition, 0L);
        timeline.frame("box", Map.of("background-color", "#ffffff"), transition, 0L);

        assertEquals("#808080", timeline.frame("box", Map.of("background-color", "#ffffff"), transition, 50L).get("background-color"));
    }

    @Test
    void supportsAllAndMultipleDescriptors() {
        UiCssTransitionTimeline timeline = new UiCssTransitionTimeline();
        List<UiCssTransitionDescriptor> transitions = List.of(
                new UiCssTransitionDescriptor("opacity", 100L, 0L, "linear"),
                new UiCssTransitionDescriptor("all", 200L, 0L, "linear")
        );

        timeline.frame("node", Map.of("opacity", "0", "left", "0px"), transitions, 0L);
        timeline.frame("node", Map.of("opacity", "1", "left", "100px"), transitions, 0L);
        Map<String, String> mid = timeline.frame("node", Map.of("opacity", "1", "left", "100px"), transitions, 50L);

        assertEquals("0.5", mid.get("opacity"));
        assertEquals("25px", mid.get("left"));
    }


    @Test
    void resolvesCommaSeparatedCssTransitionLists() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement button = document.createElement("button");
        root.appendChild(button);
        document.setRoot(root);
        UiStylesheet stylesheet = new UiCssParser().parse("button { transition: opacity 100ms linear, left 200ms ease 50ms; }");
        new UiCssCascade().apply(document, stylesheet);

        List<UiCssTransitionDescriptor> transitions = new UiCssTransitionResolver().resolveAll(button);

        assertEquals(2, transitions.size());
        assertEquals("opacity", transitions.get(0).property());
        assertEquals(100L, transitions.get(0).durationMs());
        assertEquals("left", transitions.get(1).property());
        assertEquals(200L, transitions.get(1).durationMs());
        assertEquals(50L, transitions.get(1).delayMs());
    }

    @Test
    void isExposedThroughAnimationPipelineFrameClock() {
        UiAnimationPipeline pipeline = new UiAnimationPipeline();
        UiCssTransitionDescriptor transition = new UiCssTransitionDescriptor("opacity", 100L, 0L, "linear");

        pipeline.beginFrame(0f);
        pipeline.transitionStyle("button", Map.of("opacity", "0"), transition);
        pipeline.transitionStyle("button", Map.of("opacity", "1"), transition);
        pipeline.beginFrame(0.05f);

        assertEquals("0.5", pipeline.transitionStyle("button", Map.of("opacity", "1"), transition).get("opacity"));
    }

    @Test
    void interpolatesEightDigitHexAlphaColors() {
        assertEquals("rgba(128,128,128,0.5)", UiCssTransitionInterpolator.interpolate("#00000000", "#ffffffff", 0.5));
    }

}
