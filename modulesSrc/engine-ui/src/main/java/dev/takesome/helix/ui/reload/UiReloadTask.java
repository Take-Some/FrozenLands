package dev.takesome.helix.ui.reload;

import dev.takesome.helix.io.ResourcePath;

public interface UiReloadTask {
    String id();
    default int order() { return 0; }
    default boolean supports(ResourcePath path) { return true; }
    void reload(ResourcePath path);
}
