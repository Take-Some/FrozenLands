package dev.takesome.helix.ui.uiComponents.combo;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.toStringOrEmpty;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.List;
import java.util.function.Consumer;

/** Retained-mode select/option combo box for settings and editor UI. */
public final class UiComboBoxNode extends UiComponent implements dev.takesome.helix.ui.css.runtime.UiCssStateProvider {
    private final ComboBoxModel model;
    private final ComboBoxInputController input = new ComboBoxInputController();
    private final ComboBoxRenderer renderer = new ComboBoxRenderer();
    private final ComboBoxInputController.Callbacks callbacks = new ComboCallbacks();

    private String nodeId = "";
    private String name = "";
    private String closedIconId = "chevron-down";
    private String openIconId = "chevron-up";
    private Consumer<UiComboBoxValueChangedEvent> onChanged;
    private Consumer<UiComboBoxInteractionEvent> onInteraction;
    private UiComponentEventSink eventSink = UiComponentEventSink.NONE;

    private UiColor styledBackgroundColor;
    private UiColor styledTextColor;
    private UiColor styledIconColor;
    private UiColor styledBorderColor;
    private float styledBorderWidth = -1f;

    public UiComboBoxNode(List<UiComboBoxOption> options, String value) {
        this.model = new ComboBoxModel(options, value);
    }

    public String nodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = trimToEmpty(nodeId); }
    public String name() { return name; }
    public void setName(String name) { this.name = trimToEmpty(name); }
    public String value() { return model.value(); }
    public List<UiComboBoxOption> options() { return model.options(); }
    public UiComboBoxState state() { return input.state(isEnabled()); }
    public boolean open() { return input.open(); }

    public void setOnChanged(Consumer<UiComboBoxValueChangedEvent> onChanged) { this.onChanged = onChanged; }
    public void setOnInteraction(Consumer<UiComboBoxInteractionEvent> onInteraction) { this.onInteraction = onInteraction; }
    public void setEventSink(UiComponentEventSink eventSink) { this.eventSink = eventSink == null ? UiComponentEventSink.NONE : eventSink; }

    public void setIconIds(String closedIconId, String openIconId) {
        if (closedIconId != null && !closedIconId.isBlank()) this.closedIconId = closedIconId.trim();
        if (openIconId != null && !openIconId.isBlank()) this.openIconId = openIconId.trim();
        markDirty();
    }

    public void setStyleColors(UiColor background, UiColor text, UiColor icon, UiColor border, float borderWidth) {
        this.styledBackgroundColor = background;
        this.styledTextColor = text;
        this.styledIconColor = icon;
        this.styledBorderColor = border;
        this.styledBorderWidth = Float.isFinite(borderWidth) ? borderWidth : -1f;
        markDirty();
    }

    public void setValue(String value) { setValue(value, false); }

    public void setValue(String nextValue, boolean notify) {
        String previous = model.value();
        String normalized = model.normalizedValue(nextValue);
        if (!model.setValue(normalized)) return;
        markDirty();
        if (notify) {
            emitComponentEvent("changed", previous, model.value(), model.labelFor(model.value()));
            if (onChanged != null) onChanged.accept(new UiComboBoxValueChangedEvent(nodeId, previous, model.value(), model.labelFor(model.value())));
        }
    }

    @Override
    public boolean cssState(String name) {
        if ("hover".equals(name)) return input.hovered();
        if ("active".equals(name) || "open".equals(name)) return input.open();
        if ("disabled".equals(name)) return !isEnabled();
        if ("enabled".equals(name)) return isEnabled();
        return false;
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        renderer.render(ctx, absoluteBounds(), model, input, isEnabled(), closedIconId, openIconId,
                styledBackgroundColor, styledTextColor, styledIconColor, styledBorderColor, styledBorderWidth);
    }

    @Override
    protected void onComponentPropertyChanged(String name, Object value) {
        if ("value".equals(name)) {
            setValue(toStringOrEmpty(value), false);
            return;
        }
        if ("disabled".equals(name)) {
            setEnabled(!booleanValue(value));
            return;
        }
        if ("icon".equals(name) || "closed-icon".equals(name)) {
            setIconIds(toStringOrEmpty(value), "");
            return;
        }
        if ("open-icon".equals(name) || "icon-open".equals(name)) setIconIds("", toStringOrEmpty(value));
    }

    @Override
    protected boolean onInput(UiInputEvent event) {
        if (!isEnabled()) return false;
        return input.handleMain(event, this, absoluteBounds(), model, callbacks);
    }

    public void renderDropdownOverlay(UiRenderContext ctx) {
        if (ctx == null || !input.open()) return;
        renderer.renderOptions(ctx, absoluteBounds(), model, input.hoveredOption());
    }

    public boolean handleDropdownOverlayInput(UiInputEvent event) {
        if (!isEnabled()) return false;
        return input.handleOverlay(event, absoluteBounds(), model, callbacks);
    }

    private void selectOption(int index) {
        UiComboBoxOption option = model.option(index);
        if (option == null || option.disabled()) return;
        String previous = model.value();
        input.forceClosed(callbacks);
        setValue(option.value(), true);
        emitInteraction("select", previous, model.value(), option.label());
        markDirty();
    }

    private void setOpen(boolean open) {
        if (input.open() == open) return;
        String previous = model.value();
        input.setOpen(open, callbacks);
        emitInteraction(open ? "open" : "close", previous, model.value(), model.selectedLabel());
    }

    private void emitInteraction(String interaction, String previousValue, String nextValue, String label) {
        if (interaction == null || interaction.isBlank()) return;
        emitComponentEvent(interaction, previousValue, nextValue, label);
        if (onInteraction != null) onInteraction.accept(new UiComboBoxInteractionEvent(nodeId, interaction, previousValue, nextValue, label, input.open()));
    }

    private void emitComponentEvent(String interaction, String previousValue, String nextValue, String label) {
        eventSink.emit(UiComponentEvent.builder("combo", interaction)
                .nodeId(nodeId)
                .name(name)
                .value(nextValue)
                .previousValue(previousValue)
                .label(label)
                .enabled(isEnabled())
                .open(input.open())
                .attribute("state", state().name()));
    }

    private final class ComboCallbacks implements ComboBoxInputController.Callbacks {
        @Override public void selectOption(int index) { UiComboBoxNode.this.selectOption(index); }
        @Override public void setOpen(boolean open) { UiComboBoxNode.this.setOpen(open); }
        @Override public void markDirty() { UiComboBoxNode.this.markDirty(); }
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        if (value instanceof String s) {
            String normalized = s.trim();
            if (normalized.isEmpty()) return true;
            if ("true".equalsIgnoreCase(normalized) || "1".equals(normalized)) return true;
            if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)) return false;
        }
        return false;
    }
}
