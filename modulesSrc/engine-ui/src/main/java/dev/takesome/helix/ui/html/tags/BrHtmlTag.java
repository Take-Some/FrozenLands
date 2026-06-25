package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import java.util.Set;

public final class BrHtmlTag extends UiHtmlBaseTagSpec {
    public BrHtmlTag() {
        super("br", Set.of(), "text", Set.of("id", "class", "style", "title"));
    }
}
