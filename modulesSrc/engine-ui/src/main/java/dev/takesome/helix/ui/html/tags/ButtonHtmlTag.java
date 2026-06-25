package dev.takesome.helix.ui.html.tags;



import dev.takesome.helix.ui.html.UiHtmlBaseTagSpec;

import dev.takesome.helix.ui.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class ButtonHtmlTag extends UiHtmlBaseTagSpec {

    public ButtonHtmlTag() {

        super("button", Set.of(), "button", UiHtmlCommonAttributes.interactiveControl("disabled", "value", "type"));

    }

}
