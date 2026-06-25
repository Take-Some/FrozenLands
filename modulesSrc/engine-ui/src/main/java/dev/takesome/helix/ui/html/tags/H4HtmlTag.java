package dev.takesome.helix.ui.html.tags;

import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;
import java.util.Set;

public final class H4HtmlTag extends UiHtmlBaseTagSpec {
    public H4HtmlTag() {
        super("h4", Set.of(), "text", Set.of("id", "class", "style", "title", "action", "command", "data-*", "data-action", "data-target-id", "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style"));
    }
}
