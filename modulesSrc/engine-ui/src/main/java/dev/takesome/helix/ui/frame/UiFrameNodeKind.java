package dev.takesome.helix.ui.frame;

/** Canonical runtime node categories. Not HTML tags and not renderer backend classes. */
public enum UiFrameNodeKind {
    ROOT,
    CONTAINER,
    PANEL,
    TEXT,
    IMAGE,
    BUTTON,
    INPUT,
    CHECKBOX,
    ELEMENT,
    CUSTOM
}
