package dev.takesome.helix.ui.uiComponents.slider;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.function.Consumer;

/** Retained-mode numeric slider used by engine settings and editor tooling. */
public final class UiSliderNode extends UiComponent implements dev.takesome.helix.ui.css.runtime.UiCssStateProvider {
    private String nodeId = "";
    private String name = "";
    private final SliderModel model;
    private final SliderInputController input = new SliderInputController();
    private final SliderRenderer renderer = new SliderRenderer();
    private Consumer<UiSliderValueChangedEvent> onChanged;
    private Consumer<UiSliderInteractionEvent> onInteraction;
    private UiComponentEventSink eventSink = UiComponentEventSink.NONE;

    public UiSliderNode(double min, double max, double step, double value) {
        this.model = new SliderModel(min, max, step, value);
    }

    public String nodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = trimToEmpty(nodeId); }
    public String name() { return name; }
    public void setName(String name) { this.name = trimToEmpty(name); }
    public double min() { return model.min(); }
    public double max() { return model.max(); }
    public double step() { return model.step(); }
    public double value() { return model.value(); }

    public UiSliderDragState dragState() { return input.dragState(isEnabled()); }
    public void setOnChanged(Consumer<UiSliderValueChangedEvent> onChanged) { this.onChanged = onChanged; }
    public void setOnInteraction(Consumer<UiSliderInteractionEvent> onInteraction) { this.onInteraction = onInteraction; }
    public void setEventSink(UiComponentEventSink eventSink) { this.eventSink = eventSink == null ? UiComponentEventSink.NONE : eventSink; }

    public void setRange(double min, double max, double step) {
        double previous = model.value();
        model.setRange(min, max, step);
        if (Math.abs(previous - model.value()) >= SliderModel.EPSILON) markDirty();
    }

    public void setValue(double value) { setValue(value, false, false); }

    public void setValue(double nextValue, boolean notify, boolean committed) {
        double previous = model.value();
        if (!model.setValue(nextValue)) return;
        markDirty();
        if (notify) {
            UiSliderValueChangedEvent event = new UiSliderValueChangedEvent(nodeId, previous, model.value(), committed);
            emitComponentEvent("changed", previous, model.value(), committed);
            if (onChanged != null) onChanged.accept(event);
            emitInteraction("changed", previous, model.value(), committed);
            emitInteraction(committed ? "commit" : "drag", previous, model.value(), committed);
        }
    }

    @Override
    public boolean cssState(String name) {
        if ("hover".equals(name)) return input.hovered();
        if ("active".equals(name)) return input.dragging();
        if ("disabled".equals(name)) return !isEnabled();
        if ("enabled".equals(name)) return isEnabled();
        return false;
    }

    @Override
    protected void onComponentPropertyChanged(String name, Object value) {
        if ("value".equals(name)) {
            setValue(doubleValue(value, model.value()));
            return;
        }
        if ("min".equals(name) || "max".equals(name) || "step".equals(name)) {
            setRange(
                    "min".equals(name) ? doubleValue(value, model.min()) : model.min(),
                    "max".equals(name) ? doubleValue(value, model.max()) : model.max(),
                    "step".equals(name) ? doubleValue(value, model.step()) : model.step()
            );
            return;
        }
        if ("disabled".equals(name)) setEnabled(!booleanValue(value));
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        renderer.render(ctx, absoluteBounds(), model, dragState(), isEnabled());
    }

    @Override
    protected boolean onInput(UiInputEvent event) {
        return input.handle(event, this, model, isEnabled(), new SliderInputController.Callbacks() {
            @Override public void setFromPointer(float pointerX, boolean committed) { UiSliderNode.this.setFromPointer(pointerX, committed); }
            @Override public void emitInteraction(String interaction, double previousValue, double nextValue, boolean committed) {
                UiSliderNode.this.emitInteraction(interaction, previousValue, nextValue, committed);
            }
            @Override public void markDirty() { UiSliderNode.this.markDirty(); }
        });
    }

    private void setFromPointer(float pointerX, boolean committed) {
        setValue(model.valueFromPointer(pointerX, absoluteBounds()), true, committed);
    }

    private void emitInteraction(String interaction, double previousValue, double nextValue, boolean committed) {
        if (interaction == null || interaction.isBlank()) return;
        emitComponentEvent(interaction, previousValue, nextValue, committed);
        if (onInteraction != null) onInteraction.accept(new UiSliderInteractionEvent(nodeId, interaction, previousValue, nextValue, committed));
    }

    private void emitComponentEvent(String interaction, double previousValue, double nextValue, boolean committed) {
        eventSink.emit(UiComponentEvent.builder("slider", interaction)
                .nodeId(nodeId)
                .name(name)
                .value(Double.toString(nextValue))
                .previousValue(Double.toString(previousValue))
                .enabled(isEnabled())
                .committed(committed)
                .attribute("min", Double.toString(model.min()))
                .attribute("max", Double.toString(model.max()))
                .attribute("step", Double.toString(model.step()))
                .attribute("dragState", dragState().name()));
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s.trim()); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
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
