package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.node.Node;

public interface UiMarkupElementComposer {
    String id();

    Node compose(UiMarkupComposeContext context);
}
