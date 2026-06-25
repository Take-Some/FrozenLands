package dev.takesome.helix.ui.components;

import com.badlogic.gdx.Input;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.uiComponents.button.UiButtonType;
import dev.takesome.helix.ui.uiComponents.checkbox.UiCheckboxNode;
import dev.takesome.helix.ui.uiComponents.common.UiFocusBehavior;
import dev.takesome.helix.ui.uiComponents.common.UiInteractiveState;
import dev.takesome.helix.ui.uiComponents.input.UiInputNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiInteractionContractTest {
    @Test
    void buttonClickRequiresPressedInsideAndReleasedInside() {
        UiButtonNode button = new UiButtonNode("Launch");
        button.setBounds(10f, 20f, 120f, 40f);
        AtomicInteger clicks = new AtomicInteger();
        button.setOnClick(clicks::incrementAndGet);

        assertFalse(button.isHovered());
        assertFalse(button.isPressed());

        assertFalse(button.handleInput(UiInputEvent.mouseMove(30f, 30f)));
        assertTrue(button.isHovered());

        assertTrue(button.handleInput(UiInputEvent.mouseDown(30f, 30f)));
        assertTrue(button.isPressed());
        assertEquals(0, clicks.get());

        assertTrue(button.handleInput(UiInputEvent.mouseUp(30f, 30f)));
        assertFalse(button.isPressed());
        assertEquals(1, clicks.get());
    }

    @Test
    void buttonReleaseOutsideCancelsClickButConsumesPressedSequence() {
        UiButtonNode button = new UiButtonNode("Cancel-safe");
        button.setBounds(0f, 0f, 80f, 30f);
        AtomicInteger clicks = new AtomicInteger();
        button.setOnClick(clicks::incrementAndGet);

        assertTrue(button.handleInput(UiInputEvent.mouseDown(10f, 10f)));
        assertTrue(button.isPressed());

        assertTrue(button.handleInput(UiInputEvent.mouseUp(200f, 200f)));
        assertFalse(button.isPressed());
        assertFalse(button.isHovered());
        assertEquals(0, clicks.get());
    }


    @Test
    void buttonActivationEventDefaultsToClickForOrdinaryButtons() {
        UiButtonNode button = new UiButtonNode("Open");
        button.setBounds(0f, 0f, 100f, 32f);
        ArrayList<UiComponentEvent> events = new ArrayList<>();
        button.setEventSink(events::add);

        assertTrue(button.handleInput(UiInputEvent.mouseDown(10f, 10f)));
        assertTrue(button.handleInput(UiInputEvent.mouseUp(10f, 10f)));

        UiComponentEvent activation = events.get(events.size() - 1);
        assertEquals("click", activation.interaction());
        assertEquals("button", activation.arguments().get("type"));
        assertEquals("button", activation.arguments().get("buttonType"));
    }

    @Test
    void buttonActivationEventUsesSubmitForSubmitButtons() {
        UiButtonNode button = new UiButtonNode("Confirm");
        button.setButtonType(UiButtonType.SUBMIT);
        button.setBounds(0f, 0f, 100f, 32f);
        ArrayList<UiComponentEvent> events = new ArrayList<>();
        button.setEventSink(events::add);

        assertTrue(button.handleInput(UiInputEvent.mouseDown(10f, 10f)));
        assertTrue(button.handleInput(UiInputEvent.mouseUp(10f, 10f)));

        UiComponentEvent activation = events.get(events.size() - 1);
        assertEquals("submit", activation.interaction());
        assertEquals("button:submit", activation.componentType() + ":" + activation.interaction());
        assertEquals("submit", activation.arguments().get("type"));
        assertEquals("true", activation.arguments().get("submit"));
    }

    @Test
    void buttonActivationEventUsesResetForResetButtons() {
        UiButtonNode button = new UiButtonNode("Reset");
        button.setButtonType("reset");
        button.setBounds(0f, 0f, 100f, 32f);
        ArrayList<UiComponentEvent> events = new ArrayList<>();
        button.setEventSink(events::add);

        assertTrue(button.handleInput(UiInputEvent.mouseDown(10f, 10f)));
        assertTrue(button.handleInput(UiInputEvent.mouseUp(10f, 10f)));

        UiComponentEvent activation = events.get(events.size() - 1);
        assertEquals("reset", activation.interaction());
        assertEquals("reset", activation.arguments().get("type"));
        assertEquals("true", activation.arguments().get("reset"));
    }

    @Test
    void checkboxTogglesOnceOnPressReleaseActivation() {
        UiCheckboxNode checkbox = new UiCheckboxNode("Enabled", false);
        checkbox.setBounds(0f, 0f, 160f, 40f);
        AtomicReference<Boolean> changed = new AtomicReference<>();
        checkbox.setOnChanged(changed::set);

        assertFalse(checkbox.isChecked());
        assertTrue(checkbox.handleInput(UiInputEvent.mouseDown(12f, 12f)));
        assertFalse(checkbox.isChecked());

        assertTrue(checkbox.handleInput(UiInputEvent.mouseUp(12f, 12f)));
        assertTrue(checkbox.isChecked());
        assertEquals(Boolean.TRUE, changed.get());
    }

    @Test
    void inputFocusTextEditingAndBlurAreRoutedThroughSharedBehavior() {
        UiInputNode input = new UiInputNode();
        input.setBounds(0f, 0f, 200f, 36f);
        AtomicReference<String> changed = new AtomicReference<>();
        input.setOnChanged(changed::set);

        assertFalse(input.isFocused());
        assertTrue(input.handleInput(UiInputEvent.mouseDown(20f, 18f)));
        assertTrue(input.isFocused());

        assertTrue(input.handleInput(UiInputEvent.textInput("ab")));
        assertEquals("ab", input.value());
        assertEquals("ab", changed.get());

        assertTrue(input.handleInput(UiInputEvent.keyDown(Input.Keys.BACKSPACE)));
        assertEquals("a", input.value());
        assertEquals("a", changed.get());

        assertFalse(input.handleInput(UiInputEvent.mouseDown(500f, 500f)));
        assertFalse(input.isFocused());
        assertFalse(input.handleInput(UiInputEvent.textInput("z")));
        assertEquals("a", input.value());
    }

    @Test
    void focusBehaviorControlsCaretBlinkLifecycle() {
        AtomicInteger dirty = new AtomicInteger();
        UiInteractiveState state = new UiInteractiveState(dirty::incrementAndGet);
        UiFocusBehavior focus = new UiFocusBehavior(state, dirty::incrementAndGet);

        assertFalse(focus.focused());
        focus.setFocused(true);
        assertTrue(focus.focused());
        assertTrue(focus.caretVisible());

        focus.update(0.61f);
        assertFalse(focus.caretVisible());

        focus.update(0.61f);
        assertTrue(focus.caretVisible());
        assertTrue(dirty.get() > 0);
    }
}
