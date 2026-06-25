package dev.takesome.helix.ui.uiComponents.checkbox;

import dev.takesome.helix.ui.uiComponents.common.UiInteractionListener;
import dev.takesome.helix.ui.uiComponents.common.UiInteractiveState;

/** Checkbox value and retained interaction state. */
final class CheckboxState {
    private final UiInteractiveState interaction;
    private boolean checked;

    CheckboxState(boolean checked, Runnable dirty) {
        this.checked = checked;
        this.interaction = new UiInteractiveState(dirty);
    }

    UiInteractiveState interaction() { return interaction; }
    boolean hovered() { return interaction.hovered(); }
    boolean pressed() { return interaction.pressed(); }
    boolean checked() { return checked; }
    void setInteractionListener(UiInteractionListener listener) { interaction.setListener(listener); }

    boolean setChecked(boolean checked) {
        if (this.checked == checked) return false;
        this.checked = checked;
        return true;
    }

    boolean toggle() {
        checked = !checked;
        return checked;
    }
}
