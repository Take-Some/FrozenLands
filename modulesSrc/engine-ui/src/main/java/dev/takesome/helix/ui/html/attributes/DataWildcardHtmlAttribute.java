package dev.takesome.helix.ui.html.attributes;

import dev.takesome.helix.ui.html.UiHtmlAttributeSpec;

public final class DataWildcardHtmlAttribute implements UiHtmlAttributeSpec {
    public static final String NAME = "data-*";

    public String name() { return NAME; }
}
