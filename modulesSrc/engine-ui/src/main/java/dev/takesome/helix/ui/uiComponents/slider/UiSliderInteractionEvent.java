package dev.takesome.helix.ui.uiComponents.slider;

/** Interaction packet emitted by a range slider. */
public record UiSliderInteractionEvent(
        String nodeId,
        String interaction,
        double previousValue,
        double value,
        boolean committed
) {
}
