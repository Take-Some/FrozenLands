package dev.takesome.helix.ui.html.tags;



import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;

import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class InputHtmlTag extends UiHtmlBaseTagSpec {

    public InputHtmlTag() {

        super("input", Set.of(), "input", UiHtmlCommonAttributes.inputControl());

    }

}
