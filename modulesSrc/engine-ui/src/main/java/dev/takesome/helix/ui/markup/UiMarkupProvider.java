package dev.takesome.helix.ui.markup;

import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.SceneNode;

/** Runtime entry point for HELIX UI Markup parsing and scene compilation. */
public interface UiMarkupProvider {
    UiMarkupDocument parse(String source);

    default UiMarkupDocument parse(String source, String sourcePath) {
        return parse(source);
    }

    Node compile(UiMarkupDocument document, float width, float height, EventBus events);

    Node load(String path, float width, float height, EventBus events);

    default Node loadInto(SceneNode root, String path, EventBus events) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        Node node = load(path, root.sceneWidth(), root.sceneHeight(), events);
        root.add(node);
        return node;
    }
}
