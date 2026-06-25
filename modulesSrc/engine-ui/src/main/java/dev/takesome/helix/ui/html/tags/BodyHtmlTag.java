package dev.takesome.helix.ui.html.tags;



import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;

import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class BodyHtmlTag extends UiHtmlBaseTagSpec {

    public BodyHtmlTag() {

        super("body", Set.of(), "panel", UiHtmlCommonAttributes.root());

    }

}
