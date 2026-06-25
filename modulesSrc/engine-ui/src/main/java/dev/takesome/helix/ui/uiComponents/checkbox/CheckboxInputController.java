package dev.takesome.helix.ui.uiComponents.checkbox;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.uiComponents.common.UiPointerBehavior;

/** Pointer activation controller for checkbox toggling. */
final class CheckboxInputController {
    private final UiPointerBehavior pointer;

    CheckboxInputController(Node owner, CheckboxState state) {
        this.pointer = new UiPointerBehavior(owner, state.interaction());
    }

    boolean handle(UiInputEvent event, Runnable toggle) {
        return pointer.handleActivation(event, toggle);
    }
}
