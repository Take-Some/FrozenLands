package dev.takesome.helix.ui.markup.internal.parse.scanner;

/** Token classes produced by the HELIX HTML scanner. */
public enum UiHtmlTokenType {
    TEXT,
    START_TAG,
    END_TAG,
    RAW_TEXT,
    EOF,
    ERROR
}
