package dev.takesome.helix.ui.component.registered;

import dev.takesome.helix.ui.component.UiComponentTypeId;

/** Canonical ids for all built-in registered engine-ui components. */
public final class RegisteredUiComponentIds {
    public static final UiComponentTypeId BUTTON = UiComponentTypeId.of("button");
    public static final UiComponentTypeId LABEL = UiComponentTypeId.of("label");
    public static final UiComponentTypeId TEXT = UiComponentTypeId.of("text");
    public static final UiComponentTypeId IMAGE = UiComponentTypeId.of("image");
    public static final UiComponentTypeId PANEL = UiComponentTypeId.of("panel");
    public static final UiComponentTypeId CHECKBOX = UiComponentTypeId.of("checkbox");
    public static final UiComponentTypeId SLIDER = UiComponentTypeId.of("slider");
    public static final UiComponentTypeId COMBO_BOX = UiComponentTypeId.of("combo_box");
    public static final UiComponentTypeId INPUT = UiComponentTypeId.of("input");
    public static final UiComponentTypeId INPUT_CAPTURE = UiComponentTypeId.of("input_capture");

    private RegisteredUiComponentIds() {
    }
}
