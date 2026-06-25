package dev.takesome.helix.ui.uiComponents.input;

import com.badlogic.gdx.Input;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.uiComponents.common.UiFocusBehavior;
import dev.takesome.helix.ui.uiComponents.common.UiInteractiveState;
import dev.takesome.helix.ui.uiComponents.common.UiPointerBehavior;

import java.util.function.Consumer;

/** Focus, caret and keyboard input controller for text inputs. */
final class InputTextController {
    private final UiInteractiveState state;
    private final UiPointerBehavior pointer;
    private final UiFocusBehavior focus;
    private final Runnable dirty;
    private Consumer<String> onChanged;
    private Runnable onSubmit;

    InputTextController(Node owner, Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;
        this.state = new UiInteractiveState(dirty);
        this.pointer = new UiPointerBehavior(owner, state);
        this.focus = new UiFocusBehavior(state, dirty);
    }

    UiInteractiveState state() { return state; }
    boolean focused() { return focus.focused(); }
    boolean caretVisible() { return focus.caretVisible(); }
    void setFocused(boolean focused) { focus.setFocused(focused); }
    void update(float dt) { focus.update(dt); }
    void setOnChanged(Consumer<String> onChanged) { this.onChanged = onChanged; }
    void setOnSubmit(Runnable onSubmit) { this.onSubmit = onSubmit; }

    boolean handle(UiInputEvent event, InputTextModel model, boolean enabled) {
        if (event.isPointerEvent()) return pointer.handleFocus(event, focus);
        if (!focus.focused() || !enabled) return false;

        if (event.type() == UiInputEvent.Type.TEXT_INPUT) {
            return append(model, event.text());
        }

        if (event.type() == UiInputEvent.Type.KEY_DOWN) {
            int key = event.keyCode();
            if (key == Input.Keys.BACKSPACE) return backspace(model);
            if (key == Input.Keys.DEL) return setValue(model, "", true);
            if (key == Input.Keys.ENTER || key == Input.Keys.NUMPAD_ENTER) {
                if (onSubmit != null) onSubmit.run();
                return true;
            }
            if (key == Input.Keys.ESCAPE) {
                setFocused(false);
                return true;
            }
        }
        return false;
    }

    boolean setValue(InputTextModel model, String value, boolean notify) {
        if (!model.setValue(value)) return false;
        focus.resetCaret();
        dirty.run();
        if (notify && onChanged != null) onChanged.accept(model.value());
        return true;
    }

    private boolean append(InputTextModel model, String text) {
        if (!model.append(text)) return false;
        focus.resetCaret();
        dirty.run();
        if (onChanged != null) onChanged.accept(model.value());
        return true;
    }

    private boolean backspace(InputTextModel model) {
        if (!model.backspace()) return false;
        focus.resetCaret();
        dirty.run();
        if (onChanged != null) onChanged.accept(model.value());
        return true;
    }
}
