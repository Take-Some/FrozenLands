package dev.takesome.helix.ui.uiComponents.checkbox;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.toStringOrEmpty;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;
import dev.takesome.helix.ui.uiComponents.common.UiInteractionListener;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Retained-mode checkbox with optional external skin and primitive fallback. */
public class UiCheckboxNode extends UiComponent implements dev.takesome.helix.ui.css.runtime.UiCssStateProvider {
    private final CheckboxState state;
    private final CheckboxStyle style = new CheckboxStyle(this::markDirty);
    private final CheckboxInputController input;
    private final CheckboxRenderer renderer = new CheckboxRenderer();
    private UiComponentEventSink eventSink = UiComponentEventSink.NONE;

    private String nodeId = "";
    private String name = "";
    private String label;
    private BiConsumer<Boolean, Boolean> onChanged;

    public UiCheckboxNode(String label) { this(label, false); }

    public UiCheckboxNode(String label, boolean checked) {
        this.label = emptyIfNull(label);
        this.state = new CheckboxState(checked, this::markDirty);
        this.input = new CheckboxInputController(this, state);
    }

    public String nodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = trimToEmpty(nodeId); }
    public String name() { return name; }
    public void setName(String name) { this.name = trimToEmpty(name); }
    public boolean isChecked() { return state.checked(); }
    public boolean isHovered() { return state.hovered(); }
    public boolean isPressed() { return state.pressed(); }
    public void setInteractionListener(UiInteractionListener listener) { state.setInteractionListener(listener); }

    public void setEventSink(UiComponentEventSink eventSink) {
        this.eventSink = eventSink == null ? UiComponentEventSink.NONE : eventSink;
    }

    @Override
    public boolean cssState(String name) {
        if ("hover".equals(name)) return state.hovered();
        if ("active".equals(name)) return state.pressed();
        if ("checked".equals(name)) return state.checked();
        if ("disabled".equals(name)) return !isEnabled();
        if ("enabled".equals(name)) return isEnabled();
        return false;
    }

    public void setChecked(boolean checked) { setChecked(checked, false); }

    public void setChecked(boolean checked, boolean notify) {
        boolean previous = state.checked();
        if (!state.setChecked(checked)) return;
        markDirty();
        if (notify) {
            emitChanged(previous, state.checked());
            if (onChanged != null) onChanged.accept(previous, state.checked());
        }
    }

    public void toggle() { setChecked(!state.checked(), true); }
    public String label() { return label; }

    public void setLabel(String label) {
        String next = emptyIfNull(label);
        if (this.label.equals(next)) return;
        this.label = next;
        markDirty();
    }

    public void setOnChanged(Consumer<Boolean> onChanged) {
        this.onChanged = onChanged == null ? null : (previous, next) -> onChanged.accept(next);
    }

    public void setOnChanged(BiConsumer<Boolean, Boolean> onChanged) { this.onChanged = onChanged; }
    public void setBoxSize(float boxSize) { style.setBoxSize(boxSize); }
    public void setLabelGap(float labelGap) { style.setLabelGap(labelGap); }
    public void setFontId(String fontId) { style.setFontId(fontId); }

    public void setElements(UiElementSkin checkedElement, UiElementSkin uncheckedElement, UiElementSkin hoverElement, UiElementSkin disabledElement) {
        style.setElements(checkedElement, uncheckedElement, hoverElement, disabledElement);
    }

    public void setStyleColors(UiColor box, UiColor inner, UiColor check, UiColor text, UiColor border, float borderWidth) {
        style.setStyleColors(box, inner, check, text, border, borderWidth);
    }

    @Override
    protected void onComponentPropertyChanged(String name, Object value) {
        if ("checked".equals(name) || "value".equals(name)) {
            setChecked(booleanValue(value), false);
            return;
        }
        if ("text".equals(name) || "label".equals(name)) {
            setLabel(toStringOrEmpty(value));
            return;
        }
        if ("disabled".equals(name)) {
            setEnabled(!booleanValue(value));
            return;
        }
        if ("box-size".equals(name)) {
            setBoxSize(floatValue(value, style.boxSize));
            return;
        }
        if ("label-gap".equals(name)) setLabelGap(floatValue(value, style.labelGap));
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        renderer.render(ctx, absoluteBounds(), label, state, style, isEnabled());
    }

    @Override
    protected boolean onInput(UiInputEvent event) {
        return input.handle(event, this::toggle);
    }

    private void emitChanged(boolean previous, boolean next) {
        eventSink.emit(UiComponentEvent.builder("checkbox", "changed")
                .nodeId(nodeId)
                .name(name)
                .label(label)
                .value(Boolean.toString(next))
                .previousValue(Boolean.toString(previous))
                .enabled(isEnabled())
                .checked(next)
                .attribute("hovered", Boolean.toString(isHovered()))
                .attribute("pressed", Boolean.toString(isPressed())));
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        if (value instanceof String s) {
            String normalized = s.trim();
            if (normalized.isEmpty()) return true;
            if ("true".equalsIgnoreCase(normalized) || "checked".equalsIgnoreCase(normalized) || "1".equals(normalized)) return true;
            if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)) return false;
        }
        return false;
    }

    private static float floatValue(Object value, float fallback) {
        if (value instanceof Number n) return n.floatValue();
        if (value instanceof String s) {
            try { return Float.parseFloat(s.trim()); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }
}
