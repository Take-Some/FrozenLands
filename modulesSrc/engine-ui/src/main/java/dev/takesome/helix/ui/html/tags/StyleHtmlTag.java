package dev.takesome.helix.ui.html.tags;



import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;

import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class StyleHtmlTag extends UiHtmlBaseTagSpec {

    public StyleHtmlTag() {

        super("style", Set.of(), "style", UiHtmlCommonAttributes.styleRaw("display"));

    }

}
