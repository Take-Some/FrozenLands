package dev.takesome.helix.ui.html.tags;



import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;

import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class ImageHtmlTag extends UiHtmlBaseTagSpec {

    public ImageHtmlTag() {

        super("img", Set.of(), "image", UiHtmlCommonAttributes.media());

    }

}
