package dev.takesome.helix.ui.node;

import dev.takesome.helix.ui.input.UiInputEvent;

final class NodeInputRouter {
    boolean handle(Node owner, NodeChildren children, NodeTraversal traversal, UiInputEvent event) {
        if (event == null || event.isConsumed() || !owner.isEnabled() || !owner.isVisible()) return false;

        traversal.begin();
        try {
            if (markConsumed(owner.onInputCapture(event), event)) return true;

            boolean routeChildren = !owner.clipsChildren() || !event.isPointerEvent() || owner.containsAbsolute(event.mouseX(), event.mouseY());
            if (routeChildren) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    if (markConsumed(children.get(i).handleInput(event), event)) return true;
                }
            }

            return markConsumed(owner.onInput(event), event);
        } finally {
            traversal.end(children);
        }
    }

    private boolean markConsumed(boolean handled, UiInputEvent event) {
        if (handled) event.consume();
        return handled;
    }
}
