package dev.takesome.helix.ui.uiComponents.input;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import com.badlogic.gdx.Input;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.Locale;
import java.util.function.Consumer;

/** Click-to-listen input binding placeholder for key rebinding screens. */
public final class UiInputCaptureNode extends UiComponent implements dev.takesome.helix.ui.css.runtime.UiCssStateProvider {
    private static final UiColor NORMAL = new UiColor(0.08f, 0.12f, 0.14f, 0.96f);
    private static final UiColor HOVER = new UiColor(0.12f, 0.20f, 0.23f, 0.98f);
    private static final UiColor LISTENING = new UiColor(0.22f, 0.16f, 0.08f, 0.98f);
    private static final UiColor DISABLED = new UiColor(0.07f, 0.07f, 0.08f, 0.48f);
    private static final UiColor TEXT = new UiColor(1.00f, 0.93f, 0.74f, 1.00f);
    private static final float DEFAULT_TIMEOUT_SECONDS = 8.0f;

    private String nodeId = "";
    private String value = "";
    private String placeholder = "Assign";
    private String listeningText = "Press a key...";
    private String inputActionId = "";
    private String device = "keyboard";
    private boolean hovered;
    private boolean pressed;
    private boolean listening;
    private boolean captured;
    private float timeout = DEFAULT_TIMEOUT_SECONDS;
    private float elapsed;
    private Consumer<UiInputCaptureEvent> onCaptured;
    private UiComponentEventSink eventSink = UiComponentEventSink.NONE;

    public UiInputCaptureNode(String value) {
        this.value = trimToEmpty(value);
    }

    public String nodeId() { return nodeId; }
    public String value() { return value; }
    public String inputActionId() { return inputActionId; }
    public String device() { return device; }
    public boolean listening() { return listening; }

    public void setNodeId(String nodeId) { this.nodeId = clean(nodeId); }
    public void setPlaceholder(String placeholder) { this.placeholder = blankTo(placeholder, "Assign"); markDirty(); }
    public void setListeningText(String listeningText) { this.listeningText = blankTo(listeningText, "Press a key..."); markDirty(); }
    public void setInputActionId(String inputActionId) { this.inputActionId = clean(inputActionId); }
    public void setDevice(String device) { this.device = blankTo(device, "keyboard").toLowerCase(Locale.ROOT); }
    public void setOnCaptured(Consumer<UiInputCaptureEvent> onCaptured) { this.onCaptured = onCaptured; }
    public void setEventSink(UiComponentEventSink eventSink) { this.eventSink = eventSink == null ? UiComponentEventSink.NONE : eventSink; }

    public void setValue(String value) {
        String next = clean(value);
        if (this.value.equals(next)) return;
        this.value = next;
        markDirty();
    }

    public UiInputCaptureState captureState() {
        if (!isEnabled()) return UiInputCaptureState.DISABLED;
        if (listening) return UiInputCaptureState.LISTENING;
        if (captured) return UiInputCaptureState.CAPTURED;
        if (hovered) return UiInputCaptureState.HOVERED;
        return UiInputCaptureState.IDLE;
    }

    @Override
    public boolean cssState(String name) {
        if ("hover".equals(name)) return hovered;
        if ("active".equals(name) || "listening".equals(name)) return listening || pressed;
        if ("disabled".equals(name)) return !isEnabled();
        if ("enabled".equals(name)) return isEnabled();
        return false;
    }

    @Override
    protected void onUpdate(float dt) {
        if (!listening) return;
        elapsed += Math.max(0f, dt);
        if (elapsed >= timeout) cancelCapture(true);
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        UiRect b = absoluteBounds();
        ctx.fill(b, !isEnabled() ? DISABLED : listening ? LISTENING : hovered ? HOVER : NORMAL);
        ctx.stroke(b, new UiColor(0.78f, 0.58f, 0.28f, listening ? 0.95f : 0.55f), 1f);
        String label = listening ? listeningText : value.isBlank() ? placeholder : value;
        ctx.text(label, new UiRect(b.x + 12f, b.y + 4f, Math.max(1f, b.w - 24f), Math.max(1f, b.h - 8f)), 0.76f, TEXT, TextAlign.CENTER, "standard");
    }

    @Override
    protected boolean onInputCapture(UiInputEvent event) {
        if (!isEnabled() || event == null || !listening) return false;
        if (event.type() == UiInputEvent.Type.KEY_DOWN) {
            if (event.keyCode() == Input.Keys.ESCAPE) {
                cancelCapture(true);
                return true;
            }
            capture(chordForKey(event.keyCode()));
            return true;
        }
        if (event.type() == UiInputEvent.Type.MOUSE_DOWN && !containsAbsolute(event.mouseX(), event.mouseY())) {
            capture(chordForMouse(event.mouseButton()));
            return true;
        }
        return false;
    }

    @Override
    protected boolean onInput(UiInputEvent event) {
        if (!isEnabled() || event == null || !event.isPointerEvent()) return false;
        boolean inside = containsAbsolute(event.mouseX(), event.mouseY());
        if (event.isMouseMove()) {
            setHovered(inside);
            return false;
        }
        if (event.isMouseDown() && inside) {
            pressed = true;
            setHovered(true);
            markDirty();
            return true;
        }
        if (event.isMouseUp()) {
            boolean wasPressed = pressed;
            pressed = false;
            setHovered(inside);
            if (wasPressed && inside) beginCapture();
            markDirty();
            return wasPressed && inside;
        }
        if (event.isMouseClick() && inside) {
            beginCapture();
            return true;
        }
        return false;
    }

    private void beginCapture() {
        listening = true;
        captured = false;
        elapsed = 0f;
        markDirty();
    }

    private void capture(String chord) {
        String next = clean(chord);
        if (next.isBlank()) return;
        value = next;
        listening = false;
        captured = true;
        elapsed = 0f;
        markDirty();
        emitCaptureEvent(next, false);
        if (onCaptured != null) onCaptured.accept(new UiInputCaptureEvent(nodeId, inputActionId, device, next, false));
    }

    private void cancelCapture(boolean notify) {
        if (!listening) return;
        listening = false;
        elapsed = 0f;
        markDirty();
        if (notify) {
            emitCaptureEvent(value, true);
            if (onCaptured != null) onCaptured.accept(new UiInputCaptureEvent(nodeId, inputActionId, device, value, true));
        }
    }

    private void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        markDirty();
    }

    private void emitCaptureEvent(String chord, boolean cancelled) {
        eventSink.emit(UiComponentEvent.builder("inputCapture", cancelled ? "cancelled" : "captured")
                .nodeId(nodeId)
                .name(inputActionId)
                .value(chord)
                .enabled(isEnabled())
                .attribute("inputAction", inputActionId)
                .attribute("device", device)
                .attribute("cancelled", Boolean.toString(cancelled))
                .attribute("state", captureState().name()));
    }

    private String chordForKey(int keyCode) {
        String key = Input.Keys.toString(keyCode);
        if (key == null || key.isBlank()) key = Integer.toString(keyCode);
        return "Keyboard:" + key.replace(' ', '_');
    }

    private String chordForMouse(UiInputEvent.MouseButton button) {
        String name = button == null ? "NONE" : button.name();
        return "Mouse:" + name;
    }

    private static String clean(String value) { return trimToEmpty(value); }

    private static String blankTo(String value, String fallback) {
        String clean = clean(value);
        return clean.isBlank() ? fallback : clean;
    }
}
