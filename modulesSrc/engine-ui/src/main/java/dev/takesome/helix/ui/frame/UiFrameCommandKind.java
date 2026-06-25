package dev.takesome.helix.ui.frame;

/** Typed UI draw operations consumed by render backends. */
public enum UiFrameCommandKind {
    FILL,
    STROKE,
    BOX_SHADOW,
    TEXT,
    BUTTON_TEXT,
    ICON,
    IMAGE,
    ELEMENT,
    TRACE_NODE
}
