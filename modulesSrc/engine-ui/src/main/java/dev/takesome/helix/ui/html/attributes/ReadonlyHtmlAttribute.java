package dev.takesome.helix.ui.html.attributes;

import dev.takesome.helix.ui.html.UiHtmlAttributeSpec;

public final class ReadonlyHtmlAttribute implements UiHtmlAttributeSpec {
    public static final String NAME = "readonly";

    public String name() { return NAME; }
}
