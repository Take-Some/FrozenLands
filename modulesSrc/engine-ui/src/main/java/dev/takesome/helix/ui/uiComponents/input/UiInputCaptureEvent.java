package dev.takesome.helix.ui.uiComponents.input;

/** Input capture result emitted after the component leaves listening mode. */
public record UiInputCaptureEvent(
        String nodeId,
        String inputActionId,
        String device,
        String chord,
        boolean cancelled
) {
}
