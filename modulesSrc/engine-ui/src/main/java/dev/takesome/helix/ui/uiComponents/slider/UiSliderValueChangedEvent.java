package dev.takesome.helix.ui.uiComponents.slider;

/** Value-change packet emitted by a range slider. */
public record UiSliderValueChangedEvent(
        String nodeId,
        double previousValue,
        double value,
        boolean committed
) {
}
