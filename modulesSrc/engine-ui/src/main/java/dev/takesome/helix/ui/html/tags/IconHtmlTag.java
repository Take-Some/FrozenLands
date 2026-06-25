package dev.takesome.helix.ui.html.tags;



import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;

import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class IconHtmlTag extends UiHtmlBaseTagSpec {

    public IconHtmlTag() {

        super("i", Set.of("icon"), "icon", UiHtmlCommonAttributes.icon());

    }

}
