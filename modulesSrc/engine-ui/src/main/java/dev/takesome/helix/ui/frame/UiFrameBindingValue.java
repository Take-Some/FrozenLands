package dev.takesome.helix.ui.frame;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
/** Resolved binding value attached to a UI IR node. */
public final class UiFrameBindingValue {
    private final String target;
    private final String type;
    private final Object value;

    public UiFrameBindingValue(String target, String type, Object value) {
        this.target = trimToEmpty(target);
        this.type = trimToEmpty(type);
        this.value = value;
    }

    public String target() {
        return target;
    }

    public String type() {
        return type;
    }

    public Object value() {
        return value;
    }
}
