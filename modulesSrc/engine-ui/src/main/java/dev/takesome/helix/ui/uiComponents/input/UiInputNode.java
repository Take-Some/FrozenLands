package dev.takesome.helix.ui.uiComponents.input;


import static dev.takesome.helix.validation.EngineValidator.toStringOrEmpty;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.function.Consumer;

/** Retained-mode single-line input field with optional external skin and primitive fallback. */
public class UiInputNode extends UiComponent implements dev.takesome.helix.ui.css.runtime.UiCssStateProvider {
    private final InputTextModel model;
    private final InputRenderer renderer = new InputRenderer(this::markDirty);
    private final InputTextController controller;
    private UiComponentEventSink eventSink = UiComponentEventSink.NONE;
    private Consumer<String> externalOnChanged;

    public UiInputNode() { this(""); }

    public UiInputNode(String value) {
        this.model = new InputTextModel(value);
        this.controller = new InputTextController(this, this::markDirty);
        installChangedAdapter();
    }

    public String value() { return model.value(); }
    public void setValue(String value) { setValue(value, false); }
    public void setValue(String value, boolean notify) { controller.setValue(model, value, notify); }
    public String placeholder() { return model.placeholder(); }

    public void setPlaceholder(String placeholder) {
        model.setPlaceholder(placeholder);
        markDirty();
    }

    public boolean isFocused() { return controller.focused(); }
    public void setFocused(boolean focused) { controller.setFocused(focused); }

    @Override
    public boolean cssState(String name) {
        if ("focus".equals(name)) return controller.focused();
        if ("disabled".equals(name)) return !isEnabled();
        if ("enabled".equals(name)) return isEnabled();
        return false;
    }

    public void setMaxLength(int maxLength) {
        model.setMaxLength(maxLength);
        setValue(model.value(), false);
    }

    public void setFontScale(float fontScale) { renderer.setFontScale(fontScale); }
    public void setPadding(float paddingX, float paddingY) { renderer.setPadding(paddingX, paddingY); }
    public void setFontId(String fontId) { renderer.setFontId(fontId); }
    public void setOnChanged(Consumer<String> onChanged) {
        this.externalOnChanged = onChanged;
        installChangedAdapter();
    }

    public void setEventSink(UiComponentEventSink eventSink) {
        this.eventSink = eventSink == null ? UiComponentEventSink.NONE : eventSink;
    }
    public void setOnSubmit(Runnable onSubmit) { controller.setOnSubmit(onSubmit); }

    public void setElements(UiElementSkin normalElement, UiElementSkin hoverElement, UiElementSkin focusedElement, UiElementSkin disabledElement) {
        renderer.setElements(normalElement, hoverElement, focusedElement, disabledElement);
    }

    @Override
    protected void onUpdate(float dt) { controller.update(dt); }

    @Override
    protected void onRender(UiRenderContext ctx) {
        renderer.render(ctx, absoluteBounds(), model, controller.state(), controller.focused(), isEnabled(), controller.caretVisible());
    }

    @Override
    protected boolean onInput(UiInputEvent event) { return controller.handle(event, model, isEnabled()); }

    @Override
    protected void onComponentPropertyChanged(String name, Object value) {
        if ("value".equals(name)) {
            setValue(toStringOrEmpty(value));
            return;
        }
        if ("placeholder".equals(name)) {
            setPlaceholder(toStringOrEmpty(value));
            return;
        }
        if ("disabled".equals(name)) {
            setEnabled(!booleanValue(value));
        }
    }

    private void installChangedAdapter() {
        controller.setOnChanged(value -> {
            emitChanged(value);
            if (externalOnChanged != null) externalOnChanged.accept(value);
        });
    }

    private void emitChanged(String value) {
        eventSink.emit(UiComponentEvent.builder("input", "changed")
                .nodeId(componentId().value())
                .value(value)
                .enabled(isEnabled())
                .attribute("focused", Boolean.toString(isFocused()))
                .attribute("placeholder", placeholder()));
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
