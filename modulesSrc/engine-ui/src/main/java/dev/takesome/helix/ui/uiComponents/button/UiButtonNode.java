package dev.takesome.helix.ui.uiComponents.button;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.UiComponent;
import dev.takesome.helix.ui.render.UiRenderContext;
import dev.takesome.helix.ui.uiComponents.common.UiInteractionListener;

/** Reusable retained-mode button node. */
public class UiButtonNode extends UiComponent implements dev.takesome.helix.ui.css.runtime.UiCssStateProvider {
    private final String label;
    private final ButtonState state = new ButtonState(this::markDirty);
    private final ButtonStyle style = new ButtonStyle(this::markDirty);
    private final UiButtonContentRenderer content = new UiButtonContentRenderer();
    private final ButtonRenderer renderer = new ButtonRenderer(content);
    private final ButtonInputController input = new ButtonInputController(this, state, this::emitClickEvent);
    private UiInteractionListener externalInteractionListener = UiInteractionListener.NONE;
    private UiComponentEventSink eventSink = UiComponentEventSink.NONE;
    private UiButtonType buttonType = UiButtonType.BUTTON;

    public UiButtonNode(String label) {
        this.label = emptyIfNull(label);
        state.setListener(new UiInteractionListener() {
            @Override public void onHoverChanged(boolean hovered) {
                emitBooleanEvent("hover", hovered);
                externalInteractionListener.onHoverChanged(hovered);
            }
            @Override public void onPressedChanged(boolean pressed) {
                emitBooleanEvent(pressed ? "press" : "release", pressed);
                externalInteractionListener.onPressedChanged(pressed);
            }
            @Override public void onActiveChanged(boolean active) {
                emitBooleanEvent(active ? "activate" : "deactivate", active);
                externalInteractionListener.onActiveChanged(active);
            }
        });
    }

    public String label() { return label; }
    public void setOnClick(Runnable onClick) { input.setOnClick(onClick); }
    public void setInteractionListener(UiInteractionListener listener) {
        this.externalInteractionListener = listener == null ? UiInteractionListener.NONE : listener;
    }

    public void setEventSink(UiComponentEventSink eventSink) {
        this.eventSink = eventSink == null ? UiComponentEventSink.NONE : eventSink;
    }

    public UiButtonType buttonType() {
        return buttonType;
    }

    public void setButtonType(UiButtonType buttonType) {
        this.buttonType = buttonType == null ? UiButtonType.BUTTON : buttonType;
    }

    public void setButtonType(String rawButtonType) {
        setButtonType(UiButtonType.fromMarkupType(rawButtonType));
    }
    public boolean isHovered() { return state.hovered(); }
    public boolean isPressed() { return state.pressed(); }
    public boolean isActive() { return state.active(); }
    public void setActive(boolean active) { state.setActive(active); }

    @Override
    public boolean cssState(String name) {
        if ("hover".equals(name)) return state.hovered();
        if ("active".equals(name)) return state.pressed() || state.active();
        if ("disabled".equals(name)) return !isEnabled();
        if ("enabled".equals(name)) return isEnabled();
        return false;
    }

    public void setColors(UiColor normalColor, UiColor hoveredColor, UiColor downColor, UiColor disabledColor, UiColor textColor) {
        style.setColors(normalColor, hoveredColor, downColor, disabledColor, textColor);
    }

    public void setActiveColor(UiColor activeColor) { style.setActiveColor(activeColor); }
    public void setBorder(UiColor borderColor, float borderWidth) { style.setBorder(borderColor, borderWidth); }

    public void setStateBorders(UiColor hoveredBorderColor, UiColor pressedBorderColor, UiColor activeBorderColor, UiColor disabledBorderColor) {
        style.setStateBorders(hoveredBorderColor, pressedBorderColor, activeBorderColor, disabledBorderColor);
    }

    public void setElements(UiElementSkin normalElement, UiElementSkin hoveredElement, UiElementSkin pressedElement, UiElementSkin disabledElement) {
        style.setElements(normalElement, hoveredElement, pressedElement, disabledElement);
    }

    public void setActiveElement(UiElementSkin activeElement) { style.setActiveElement(activeElement); }
    public void setFontScale(float fontScale) { style.setFontScale(fontScale); }
    public void setFontId(String fontId) { style.setFontId(fontId); }

    public void setIcon(UiIcon icon, float iconScale, float iconSize, float iconGap, float iconInset) {
        style.setIcon(icon, iconScale, iconSize, iconGap, iconInset);
    }

    @Override
    protected void onUpdate(float dt) {
        if (state.update(dt)) markDirty();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        renderer.render(ctx, absoluteBounds(), label, state, style, isEnabled());
    }

    @Override
    protected UiRect debugContentBounds(UiRenderContext ctx) {
        return renderer.contentBounds(ctx, absoluteBounds(), state, style, isEnabled());
    }

    @Override
    protected boolean onInput(UiInputEvent event) {
        return input.handle(event);
    }

    private void emitClickEvent() {
        emitBooleanEvent(buttonType.activationInteraction(), true);
    }

    private void emitBooleanEvent(String interaction, boolean value) {
        eventSink.emit(UiComponentEvent.builder("button", interaction)
                .nodeId(componentId().value())
                .name(label)
                .label(label)
                .value(Boolean.toString(value))
                .enabled(isEnabled())
                .checked(false)
                .attribute("type", buttonType.attributeValue())
                .attribute("buttonType", buttonType.attributeValue())
                .attribute("button-type", buttonType.attributeValue())
                .attribute("submit", Boolean.toString(buttonType == UiButtonType.SUBMIT))
                .attribute("reset", Boolean.toString(buttonType == UiButtonType.RESET))
                .attribute("hovered", Boolean.toString(isHovered()))
                .attribute("pressed", Boolean.toString(isPressed()))
                .attribute("active", Boolean.toString(isActive())));
    }
}
