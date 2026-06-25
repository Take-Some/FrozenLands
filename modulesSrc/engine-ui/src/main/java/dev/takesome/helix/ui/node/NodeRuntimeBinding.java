package dev.takesome.helix.ui.node;

/** Per-frame runtime binding attached to a retained UI node. */
@FunctionalInterface
public interface NodeRuntimeBinding {
    void update(Node node, float dt);
}
