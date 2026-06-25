package dev.takesome.helix.ui.uiComponents.text;

import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;

/** Explicit text component alias backed by the label/text renderer. */
public final class UiTextNode extends UiLabelNode {
    public UiTextNode(String text) {
        super(text);
    }

    public UiTextNode(String text, float scale, UiColor color, TextAlign align) {
        super(text, scale, color, align);
    }

    public UiTextNode(String text, float scale, UiColor color, TextAlign align, String fontId) {
        super(text, scale, color, align, fontId);
    }
}
