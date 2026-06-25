package dev.takesome.helix.ui.markup.provider;

import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.UiMarkupProvider;
import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.ui.node.ContainerNode;
import dev.takesome.helix.ui.node.Node;

/** Degraded provider used when the markup module is absent. */
public final class NoOpUiMarkupProvider implements UiMarkupProvider {
    @Override
    public UiMarkupDocument parse(String source) {
        throw new IllegalStateException("HELIX UI Markup provider is not installed");
    }

    @Override
    public Node compile(UiMarkupDocument document, float width, float height, EventBus events) {
        return new ContainerNode();
    }

    @Override
    public Node load(String path, float width, float height, EventBus events) {
        throw new IllegalStateException("HELIX UI Markup provider is not installed: " + path);
    }
}
