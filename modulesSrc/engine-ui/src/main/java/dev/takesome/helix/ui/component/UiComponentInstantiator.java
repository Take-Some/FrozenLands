package dev.takesome.helix.ui.component;

import dev.takesome.helix.ui.node.UiComponent;

@FunctionalInterface
public interface UiComponentInstantiator {
    UiComponent create(UiComponentDescriptor descriptor);
}
