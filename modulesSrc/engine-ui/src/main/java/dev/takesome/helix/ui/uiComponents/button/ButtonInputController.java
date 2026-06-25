package dev.takesome.helix.ui.uiComponents.button;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.uiComponents.common.UiPointerBehavior;

/** Button activation routing separated from retained visual state. */
final class ButtonInputController {
    private final UiPointerBehavior pointer;
    private final Runnable onActivated;
    private Runnable onClick;

    ButtonInputController(Node owner, ButtonState state) {
        this(owner, state, null);
    }

    ButtonInputController(Node owner, ButtonState state, Runnable onActivated) {
        this.pointer = new UiPointerBehavior(owner, state.interaction());
        this.onActivated = onActivated;
    }

    void setOnClick(Runnable onClick) { this.onClick = onClick; }

    boolean handle(UiInputEvent event) {
        return pointer.handleActivation(event, this::click);
    }

    private void click() {
        if (onActivated != null) onActivated.run();
        if (onClick != null) onClick.run();
    }
}
